package cz.habarta.typescript.generator;

import java.util.List;

public class EnumModel extends BaseModel {

	private final List<String> values;

	public EnumModel(String name, List<String> values) {
		super(name);
		this.values = values;
	}

	public List<String> getValues() {
		return values;
	}

	@Override
	public String toString() {
		return "EnumModel{" + "name=" + name + ", values=" + values + '}';
	}

}
