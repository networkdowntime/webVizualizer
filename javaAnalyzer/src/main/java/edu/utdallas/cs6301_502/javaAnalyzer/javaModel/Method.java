package edu.utdallas.cs6301_502.javaAnalyzer.javaModel;
import java.util.LinkedHashMap;

import edu.utdallas.cs6301_502.javaAnalyzer.JavaAnalyzer;


public class Method extends Block {

	String name;
	LinkedHashMap<String, String> paramMap = new LinkedHashMap<String, String>();

	public Method(Class clazz, String name) {
		super(null);
		this.parent = clazz;
		this.name = name;
		JavaAnalyzer.log(3, "Creating Method: " + clazz.pkg.getName() + "." + clazz.name + "." + name);
	}

	public String getName() {
		return this.name;
	}

	public void setParamMap(LinkedHashMap<String, String> paramMap) {
		assert (paramMap != null);

		this.paramMap = paramMap;

		for (String name : paramMap.keySet()) {
			JavaAnalyzer.log(0, "Adding Method Parameter: " + name);
			this.addVariable(name, paramMap.get(name));
		}
	}

	public void validatePassOne() {
		JavaAnalyzer.log(1, "Validating Method: " + getName());
		super.validatePassOne();
	}
	
	
}
