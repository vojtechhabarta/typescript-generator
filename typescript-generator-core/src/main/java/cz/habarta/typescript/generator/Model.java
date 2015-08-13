
package cz.habarta.typescript.generator;

import java.util.List;

public class Model {

	private final List<BaseModel> beans;

	public Model(List<BaseModel> beans) {
		this.beans = beans;
	}

	public List<BaseModel> getBeans() {
		return beans;
	}

}
