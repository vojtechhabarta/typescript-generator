
package cz.habarta.typescript.generator;

import java.util.List;

public class BeanModel extends BaseModel {

	private final String parent;
	private final List<PropertyModel> properties;

	public BeanModel(String name, String parent, List<PropertyModel> properties) {
		super(name);
		this.parent = parent;
		this.properties = properties;
	}

	public String getParent() {
		return parent;
	}

	public List<PropertyModel> getProperties() {
		return properties;
	}

	@Override
	public String toString() {
		return "BeanModel{" + "name=" + name + ", properties=" + properties + '}';
	}

}
