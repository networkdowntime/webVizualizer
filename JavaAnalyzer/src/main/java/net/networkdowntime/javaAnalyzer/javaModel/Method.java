package net.networkdowntime.javaAnalyzer.javaModel;
import java.util.HashMap;
import java.util.LinkedHashMap;

import net.networkdowntime.javaAnalyzer.JavaAnalyzer;


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
			
			for (String s : varNameTypeMap.keySet()) {
				System.out.print(s + ", ");
			}
			if (varNameTypeMap.size() > 0)
				System.out.println();

		}
	}

	public void validate() {
		JavaAnalyzer.log(1, "Validating Method: " + getName());
		super.validate();
	}
}
