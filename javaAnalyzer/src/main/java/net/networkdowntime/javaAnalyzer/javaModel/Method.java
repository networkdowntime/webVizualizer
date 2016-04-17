package net.networkdowntime.javaAnalyzer.javaModel;

import net.networkdowntime.javaAnalyzer.logger.Logger;

public class Method extends Block {

	String returnType;

	public Method(int depth, Class clazz, String name) {
		super(depth, null);
		this.parent = clazz;
		this.name = name;
		Logger.log(depth, "Creating Method: " + clazz.pkg.getName() + "." + clazz.name + "." + name);
	}

	public void setReturnType(int depth, String type, boolean addUnresolveClass) {
		Logger.log(depth, "Setting Method Return Type: " + type);
		this.returnType = type;
		if (addUnresolveClass) {
			this.addUnresolvedClass(depth + 1, type);
		}
	}

	@Override
	public void validatePassOne(int depth) {
		Logger.log(depth, "Validating " + this.getClass().getName() + ": " + getName());

		for (String varName : paramMap.keySet()) {
			for (String type : splitType(paramMap.get(varName))) {
				Logger.log(depth + 1, this.getClass().getName() + " " + getName() + ": Searching for class for variable: " + type + " " + varName + "; isPrimative=" + isPrimative(type) + "; is \"this\"=" + "this".equals(type));

				if (!(isPrimative(type) || "this".equals(type))) {
					Class clazz = searchForUnresolvedClass(depth + 2, type);

					if (clazz != null) {
						Logger.log(depth + 2, "Matched unresolved class: " + type + " to " + clazz.getCanonicalName());
						if (!"this".equals(type))
							addResolvedClass(clazz);
						varNameClassMap.put(varName, clazz);
					}
				}
			}
		}
		super.validatePassOne(depth + 1);
	}

}