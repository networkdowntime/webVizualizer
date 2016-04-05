package edu.utdallas.cs6301_502.javaAnalyzer.javaModel;

import java.util.LinkedHashMap;
import edu.utdallas.cs6301_502.javaAnalyzer.AstVisitor;

public class Method extends Block {

	String name;
	String returnType;

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

	public void setReturnType(int depth, String type) {
		AstVisitor.log(depth, "Setting Method Return Type: " + type);
		this.returnType = type;
		this.addUnresolvedClass(depth + 1, type);
	}

}