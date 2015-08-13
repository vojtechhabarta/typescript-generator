
package cz.habarta.typescript.generator;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Logger;

public abstract class ModelParser {

	protected final Logger logger;
	protected final Settings settings;
	private final Queue<ClassWithUsage> classQueue = new LinkedList<>();

	public ModelParser(Logger logger, Settings settings) {
		this.logger = logger;
		this.settings = settings;
	}

	public Model parseModel(Class<?> cls) {
		return parseModel(Arrays.asList(cls));
	}

	public Model parseModel(List<? extends Class<?>> classes) {
		for (Class<?> cls : classes) {
			classQueue.add(new ClassWithUsage(cls, null, null));
		}
		return parseQueue();
	}

	private Model parseQueue() {
		final LinkedHashMap<Class<?>, BaseModel> parsedClasses = new LinkedHashMap<>();
		ClassWithUsage classWithUsage;
		while ((classWithUsage = classQueue.poll()) != null) {
			final Class<?> cls = classWithUsage.beanClass;
			if (!parsedClasses.containsKey(cls)) {
				logger.info("Parsing '" + cls.getName() + "'" + (classWithUsage.usedInClass != null
						? " used in '" + classWithUsage.usedInClass.getSimpleName() + "." + classWithUsage.usedInProperty + "'"
						: ""));

				if (settings.declareEnums && cls.isEnum()) {
					final EnumModel bean = parseEnum(classWithUsage);
					parsedClasses.put(cls, bean);
				} else {
					final BeanModel bean = parseBean(classWithUsage);
					parsedClasses.put(cls, bean);
				}
			}
		}
		return new Model(new ArrayList<>(parsedClasses.values()));
	}

	protected abstract BeanModel parseBean(ClassWithUsage classWithUsage);

	protected abstract EnumModel parseEnum(ClassWithUsage classWithUsage);

	protected void addBeanToQueue(ClassWithUsage classWithUsage) {
		classQueue.add(classWithUsage);
	}

	protected PropertyModel processTypeAndCreateProperty(String name, Type type, Class<?> usedInClass) {
		final TsType originalType = typeFromJava(type, name, usedInClass);
		final LinkedHashSet<TsType.EnumType> replacedEnums = new LinkedHashSet<>();

		TsType tsType = null;
		List<String> comments = null;

		if (settings.declareEnums) {
			tsType = originalType;
		} else {
			tsType = TsType.replaceEnumsWithStrings(originalType, replacedEnums);
			if (!replacedEnums.isEmpty()) {
				comments = new ArrayList<>();
				comments.add("Original type: " + originalType);
				for (TsType.EnumType replacedEnum : replacedEnums) {
					comments.add(replacedEnum.toString() + ": " + join(replacedEnum.getValues(), ", "));
				}
			}
		}
		return new PropertyModel(name, type, tsType, comments);
	}

	protected String getMappedName(Class<?> cls) {
		if (cls == null) {
			return null;
		}
		final String name = cls.getSimpleName();
		if (settings.removeTypeNameSuffix != null && name.endsWith(settings.removeTypeNameSuffix)) {
			return name.substring(0, name.length() - settings.removeTypeNameSuffix.length());
		} else {
			return name;
		}
	}

	private TsType typeFromJava(Type javaType, String usedInProperty, Class<?> usedInClass) {
		if (KnownTypes.containsKey(javaType))
			return KnownTypes.get(javaType);
		if (javaType instanceof Class) {
			final Class<?> javaClass = (Class<?>) javaType;
			if (javaClass.isArray()) {
				return new TsType.BasicArrayType(typeFromJava(javaClass.getComponentType(), usedInProperty, usedInClass));
			}
			if (javaClass.isEnum()) {
				@SuppressWarnings("unchecked")
				final Class<? extends Enum<?>> enumClass = (Class<? extends Enum<?>>) javaClass;
				final List<java.lang.String> values = new ArrayList<>();
				for (Enum<?> enumConstant : enumClass.getEnumConstants()) {
					values.add(enumConstant.name());
				}
				return new TsType.EnumType(getMappedName(javaClass), values);
			}
			if (List.class.isAssignableFrom(javaClass)) {
				return new TsType.BasicArrayType(TsType.Any);
			}
			if (Map.class.isAssignableFrom(javaClass)) {
				return new TsType.IndexedArrayType(TsType.String, TsType.Any);
			}
			// consider it structural
			classQueue.add(new ClassWithUsage(javaClass, usedInProperty, usedInClass));
			return new TsType.StructuralType(getMappedName(javaClass));
		}
		if (javaType instanceof ParameterizedType) {
			final ParameterizedType parameterizedType = (ParameterizedType) javaType;
			if (parameterizedType.getRawType() instanceof Class) {
				final Class<?> javaClass = (Class<?>) parameterizedType.getRawType();
				if (List.class.isAssignableFrom(javaClass)) {
					return new TsType.BasicArrayType(
							typeFromJava(parameterizedType.getActualTypeArguments()[0], usedInProperty, usedInClass));
				}
				if (Map.class.isAssignableFrom(javaClass)) {
					return new TsType.IndexedArrayType(TsType.String,
							typeFromJava(parameterizedType.getActualTypeArguments()[1], usedInProperty, usedInClass));
				}
			}
		}
		logger.warning(
				String.format("Unsupported type '%s' used in '%s.%s'", javaType, usedInClass.getSimpleName(), usedInProperty));
		return TsType.Any;
	}

	private static Map<Type, TsType> getKnownTypes() {
		final Map<Type, TsType> knownTypes = new LinkedHashMap<>();
		knownTypes.put(Object.class, TsType.Any);
		knownTypes.put(Byte.class, TsType.Number);
		knownTypes.put(Byte.TYPE, TsType.Number);
		knownTypes.put(Short.class, TsType.Number);
		knownTypes.put(Short.TYPE, TsType.Number);
		knownTypes.put(Integer.class, TsType.Number);
		knownTypes.put(Integer.TYPE, TsType.Number);
		knownTypes.put(Long.class, TsType.Number);
		knownTypes.put(Long.TYPE, TsType.Number);
		knownTypes.put(Float.class, TsType.Number);
		knownTypes.put(Float.TYPE, TsType.Number);
		knownTypes.put(Double.class, TsType.Number);
		knownTypes.put(Double.TYPE, TsType.Number);
		knownTypes.put(Boolean.class, TsType.Boolean);
		knownTypes.put(Boolean.TYPE, TsType.Boolean);
		knownTypes.put(Character.class, TsType.String);
		knownTypes.put(Character.TYPE, TsType.String);
		knownTypes.put(String.class, TsType.String);
		knownTypes.put(Date.class, TsType.Date);
		knownTypes.put(LocalDate.class, TsType.Date);
		knownTypes.put(LocalTime.class, TsType.Date);
		knownTypes.put(LocalDateTime.class, TsType.Date);
		knownTypes.put(ZonedDateTime.class, TsType.Date);
		knownTypes.put(OffsetTime.class, TsType.Date);
		knownTypes.put(OffsetDateTime.class, TsType.Date);
		return knownTypes;
	}

	private static final Map<Type, TsType> KnownTypes = getKnownTypes();

	private static String join(Iterable<? extends Object> values, String delimiter) {
		final StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (Object value : values) {
			if (first) {
				first = false;
			} else {
				sb.append(delimiter);
			}
			sb.append(value);
		}
		return sb.toString();
	}
}
