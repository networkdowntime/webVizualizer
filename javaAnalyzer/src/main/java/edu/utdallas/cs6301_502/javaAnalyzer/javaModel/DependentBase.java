package edu.utdallas.cs6301_502.javaAnalyzer.javaModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

import edu.utdallas.cs6301_502.javaAnalyzer.AstVisitor;

public abstract class DependentBase {

	DependentBase parent;

	HashSet<Class> annotationDependencies = new HashSet<Class>();
	HashMap<String, Class> classDependencies = new HashMap<String, Class>();
	// HashSet<Method> methodDependencies = new HashSet<Method>();

	HashSet<String> unresolvedAnnotations = new HashSet<String>();
	HashSet<String> unresolvedClasses = new HashSet<String>();
	HashSet<String> unresolvedInterfaces = new HashSet<String>();
	HashMap<String, Integer> unresolvedClassCount = new HashMap<String, Integer>();

	HashMap<String, HashSet<String>> unresolvedMethods = new HashMap<String, HashSet<String>>();

	HashMap<String, String> varNameTypeMap = new LinkedHashMap<String, String>(); // This is the unqualified Type
	HashMap<String, Class> varNameClassMap = new LinkedHashMap<String, Class>();
	HashMap<Class, HashSet<Method>> methodCallMap = new HashMap<Class, HashSet<Method>>();

	public void setClass(DependentBase clazz) {
		this.parent = clazz;
	}

	public DependentBase getParent() {
		return parent;
	}

	public String getCanonicalName() {
		if (this instanceof Class) {
			return ((Class) this).pkg.getName() + "." + ((Class) this).name;
		} else {
			if (parent != null) {
				return parent.getCanonicalName();
			} else {
				return "";
			}
		}
	}

	public void addPotentialClass(int depth, String className) {
		boolean found = false;

		DependentBase base = this;

		while (base != null) {
			if (base.varNameTypeMap.containsKey(className) || base.unresolvedAnnotations.contains(className)) {
				found = true;
			}
			base = base.parent;
		}

		if (!found) {
			addUnresolvedClass(depth, className);
		}
	}

	public void addUnresolvedAnnotations(String annotationName) {
		if (!this.unresolvedAnnotations.contains(annotationName)) {
			this.unresolvedAnnotations.add(annotationName);
			AstVisitor.log(3, "Adding unresolved annotation: " + annotationName);
		}
	}

	public void addUnresolvedInterface(String interfaceName) {
		if (!this.unresolvedInterfaces.contains(interfaceName)) {
			this.unresolvedInterfaces.add(interfaceName);
			AstVisitor.log(3, "Adding unresolved interface: " + interfaceName);
		}
	}

	public void addUnresolvedClass(int depth, String className) {
		if (className.contains("[")) // remove array notation if needed
			className = className.substring(0, className.indexOf("["));

		if (!isVoid(className) && !isPrimative(className) && !"this".equals(className) && !this.unresolvedClasses.contains(className)) {
			Integer count = this.unresolvedClassCount.get(className);
			if (count == null)
				count = 0;
			this.unresolvedClasses.add(className);
			this.unresolvedClassCount.put(className, count.intValue() + 1);
			AstVisitor.log(depth, "Adding unresolved class: " + className);
		}
	}

	public void addResolvedClass(Class clazz) {
		classDependencies.put(clazz.getName(), clazz);

		if (this instanceof Class) {
			clazz.addReferencedByClass((Class) this);
		}
		if (parent != null) {
			parent.addResolvedClass(clazz);
		}
	}

	public void addUnresolvedMethodCall(int depth, String typeOrVarName, String methodName) {
		AstVisitor.log(depth, "Adding unresolved method call: " + typeOrVarName + " -> " + methodName);

		HashSet<String> methods = unresolvedMethods.get(varNameTypeMap);
		if (methods == null) {
			methods = new HashSet<String>();
			unresolvedMethods.put(typeOrVarName, methods);
		}
		methods.add(methodName);
	}

	protected boolean isVoid(String name) {
		boolean retval = false;

		if ("void".equals(name))
			retval = true;

		return retval;
	}

	protected boolean isPrimative(String name) {
		boolean retval = false;

		if ("boolean".equals(name) || "boolean[]".equals(name))
			retval = true;
		else if ("byte".equals(name) || "byte[]".equals(name))
			retval = true;
		else if ("short".equals(name) || "short[]".equals(name))
			retval = true;
		else if ("int".equals(name) || "int[]".equals(name))
			retval = true;
		else if ("long".equals(name) || "long[]".equals(name))
			retval = true;
		else if ("float".equals(name) || "float[]".equals(name))
			retval = true;
		else if ("double".equals(name) || "double[]".equals(name))
			retval = true;
		else if ("char".equals(name) || "char[]".equals(name))
			retval = true;

		return retval;
	}

	public static List<String> splitType(String type) {
		List<String> genericsExpansion = new ArrayList<String>();
		type = type.replaceAll("[<|,>]", " ");
		for (String genericType : type.split("\\s+")) {
			genericsExpansion.add(genericType);
		}
		return genericsExpansion;
	}

	public void addVariable(int depth, String name, String type) {
		if (!varNameTypeMap.containsKey(name)) {
			AstVisitor.log(depth, "Adding variable to " + this.getCanonicalName() + ": " + type + " " + name);
			varNameTypeMap.put(name, type);
		}
	}

	public Class findClass() {

		DependentBase db = this;

		while (db.getParent() != null)
			db = db.getParent();

		if (db instanceof Class) {
			return (Class) db;
		} else {
			return null;
		}
	}

	public Class searchForUnresolvedClass(int depth, String className) {
		AstVisitor.log(depth, "DependentBase.searchForUnresolvedClass(" + className + ")");
		Class matchedClass = classDependencies.get(className);

		if (matchedClass == null) {
			if (parent != null)
				matchedClass = parent.searchForUnresolvedClass(depth + 1, className);
		}
		return matchedClass;
	}

	public Class searchForVariableClass(int depth, String variableName) {
		AstVisitor.log(depth, "DependentBase.searchForVariable(" + variableName + ") in " + getCanonicalName());
		
		for (String varName : varNameClassMap.keySet()) {
			AstVisitor.log(depth + 1, "Considering variable " + varName + " of type " + varNameClassMap.get(varName).getName() + "; matched=" + variableName.trim().equals(varName.trim()));
		}

		Class matchedClass = varNameClassMap.get(variableName);

		if (matchedClass == null) {
			if (parent != null)
				matchedClass = parent.searchForVariableClass(depth + 1, variableName);
		}
		return matchedClass;
	}

	public void validatePassOne(int depth) {
		Class c = findClass();

		if (this instanceof Method) {
			AstVisitor.log(depth, "Validating Method: " + ((Method) this).name + "; method's class: " + c.getCanonicalName());
		} else {
			AstVisitor.log(depth, "Validating Block");
		}

		for (String s : this.unresolvedInterfaces) {
			AstVisitor.log(depth + 1, "Class " + c.getName() + ": Searching for unresolved interfaces: " + s);
			Class clazz = searchForUnresolvedClass(depth + 2, s);
			if (clazz != null) {
				AstVisitor.log(depth + 2, "Matched unresolved interface: " + s + " to " + clazz.getCanonicalName());
				addResolvedClass(clazz);
				this.annotationDependencies.add(clazz);
			}
		}

		for (String s : this.unresolvedAnnotations) {
			AstVisitor.log(depth + 1, "Class " + c.getName() + ": Searching for unresolved annotation: " + s);
			Class clazz = searchForUnresolvedClass(depth + 2, s);
			if (clazz != null) {
				AstVisitor.log(depth + 2, "Matched unresolved annotation: " + s + " to " + clazz.getCanonicalName());
				addResolvedClass(clazz);
				this.annotationDependencies.add(clazz);
			}
		}

		for (String s : this.unresolvedClasses) {
			AstVisitor.log(depth + 1, "Class " + c.getName() + ": Searching for unresolved class: " + s);
			Class clazz = searchForUnresolvedClass(depth + 2, s);
			if (clazz != null) {
				AstVisitor.log(depth + 2, "Matched unresolved class: " + s + " to " + clazz.getCanonicalName());
				addResolvedClass(clazz);
			}
		}

		for (String varName : varNameTypeMap.keySet()) {
			for (String type : splitType(varNameTypeMap.get(varName))) {

				AstVisitor.log(depth + 1,
						"Class " + c.getName() + ": Searching for class for variable: " + type + " " + varName + "; isPrimative=" + isPrimative(type) + "; is \"this\"=" + "this".equals(type));
				if (!(isPrimative(type) || "this".equals(type))) {
					Class clazz = searchForUnresolvedClass(depth + 2, type);

					if (clazz != null) {
						AstVisitor.log(depth + 2, "Matched unresolved class: " + type + " to " + clazz.getCanonicalName());
						if (!"this".equals(type))
							addResolvedClass(clazz);
						varNameClassMap.put(varName, clazz);
					}
				}
			}
		}

		if (this instanceof Block) {
			Block b = (Block) this;
			for (Block block : b.childBlocks) {
				block.validatePassOne(depth + 1);
			}
		}

	}

	public int validatePassTwo(int depth) {
		if (this instanceof Block) {
			Block b = (Block) this;
			for (Block block : b.childBlocks) {
				depth = block.validatePassTwo(depth + 1);
			}
		}

		AstVisitor.log(depth, this.getClass().getName() + " validatePassTwo(): " + getCanonicalName());

		for (String typeOrVarName : unresolvedMethods.keySet()) {
			HashSet<String> methodSet = unresolvedMethods.get(typeOrVarName);

			AstVisitor.log(depth + 1, "Checking for unresolved method call: " + typeOrVarName + " in " + getCanonicalName());

			String tovn = typeOrVarName;
			Class clazz = null;

			if (tovn.contains(".")) {
				String[] split = tovn.split("\\.");
				tovn = split[0];
			}

			if ("this".equals(tovn)) {
				clazz = findClass();
			} else if ("super".equals(tovn)) {
				// TODO should search the entends heirarchy to see if there is a method match, if not assume immediate extends
				clazz = findClass().extnds;
			} else if ("String".equals(tovn)) {
				clazz = searchForUnresolvedClass(depth + 2, "String");
			} else if (tovn.startsWith("\"") && tovn.endsWith("\"")) {
				clazz = searchForUnresolvedClass(depth + 2, "String");
			} else if ("null".equals(tovn) || tovn == null) {
				// nothing to do?
			} else {
				//TODO figure out how to handle chains.  i.e. System.out.println()
				// should add a field to System of type unknown something that has a method called println();
				// this attempts, going up the heirarchy to resolve the typeOrVarName to a defined variable
				clazz = searchForVariableClass(depth + 2, tovn);

				if (clazz == null) {
					clazz = searchForUnresolvedClass(depth + 2, tovn);
				}

			}

			if (clazz != null) {
				if (!"this".equals(tovn)) {
					addResolvedClass(clazz);
				}
				
				AstVisitor.log(depth + 2, "DependentBase.validatePassTwo() for " + findClass().name + ": typeOrVarName " + typeOrVarName + " matched to " + clazz.name);

				for (String methodName : methodSet) {
					HashSet<Method> methods = methodCallMap.get(clazz);
					if (methods == null) {
						methods = new HashSet<Method>();
						methodCallMap.put(clazz, methods);
					}

					Method method = null;
					if (clazz.fromFile) {
						method = clazz.getMethod(methodName + "()");

					} else {
						method = clazz.getOrCreateAndGetMethod(depth + 3, methodName + "()");
					}

					if (method != null) {
						methods.add(method);
						AstVisitor.log(depth + 3, "Found Method Call Reference: " + clazz.getCanonicalName() + "." + method.name);
					}
				}
			}
		}
		return depth + 1;
	}

}