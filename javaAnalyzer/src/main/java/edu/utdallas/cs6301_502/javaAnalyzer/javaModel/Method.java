package edu.utdallas.cs6301_502.javaAnalyzer.javaModel;

import java.util.LinkedHashMap;
import edu.utdallas.cs6301_502.javaAnalyzer.AstVisitor;

public class Method extends Block {

	String name;
	String returnType;
	LinkedHashMap<String, String> paramMap = new LinkedHashMap<String, String>();

	public Method(int depth, Class clazz, String name) {
		super(depth, null);
		this.parent = clazz;
		this.name = name;
		AstVisitor.log(depth, "Creating Method: " + clazz.pkg.getName() + "." + clazz.name + "." + name);
	}

	public String getName() {
		return this.name;
	}

	public String getCanonicalName() {
		return parent.getCanonicalName() + "." + this.name;
	}

	public void setParamMap(int depth, LinkedHashMap<String, String> paramMap) {
		assert (paramMap != null);

		this.paramMap = paramMap;

		for (String name : paramMap.keySet()) {
			AstVisitor.log(depth, "Adding Method Parameter: " + name);
			this.addVariable(depth, name, paramMap.get(name));
		}
	}

	public void setReturnType(int depth, String type) {
		this.returnType = type;
		this.addUnresolvedClass(depth, type);
	}

	@Override
	public void validatePassOne(int depth) {
		AstVisitor.log(depth, "Validating Method: " + getName());

		for (String varName : paramMap.keySet()) {
			for (String type : splitType(paramMap.get(varName))) {
				AstVisitor.log(depth + 1, "Method " + getName() + ": Searching for class for variable: " + type + " " + varName + "; isPrimative=" + isPrimative(type) + "; is \"this\"=" + "this".equals(type));

				if (!(isPrimative(type) || "this".equals(type))) {
					Class clazz = searchForUnresolvedClass(depth + 2, type);

					if (clazz != null) {
						AstVisitor.log(depth + 2, "Matched unresolved class: " + type + " to " + clazz.getCanonicalName());
						addResolvedClass(clazz);
						varNameClassMap.put(varName, clazz);
					}
				}
			}
		}
	}

}