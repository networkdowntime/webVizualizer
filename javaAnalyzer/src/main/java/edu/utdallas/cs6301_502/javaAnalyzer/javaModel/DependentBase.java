package edu.utdallas.cs6301_502.javaAnalyzer.javaModel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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

	
	public void addPotentialClass(String className) {
		boolean found = false;

		DependentBase base = this;

		while (base != null) {
			if (base.varNameTypeMap.containsKey(className) || base.unresolvedAnnotations.contains(className)) {
				found = true;
			}
			base = base.parent;
		}

		if (!found) {
			addUnresolvedClass(className);
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

	public void addUnresolvedClass(String className) {
		if (className.contains("["))
			className = className.substring(0, className.indexOf("["));
		
		if (!isPrimative(className) && !this.unresolvedClasses.contains(className)) {
			Integer count = this.unresolvedClassCount.get(className);
			if (count == null)
				count = 0;
			this.unresolvedClasses.add(className);
			this.unresolvedClassCount.put(className, count.intValue() + 1);
			AstVisitor.log(3, "Adding unresolved class: " + className);
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

	private boolean isPrimative(String name) {
		boolean retval = false;

		if (name.equals("boolean"))
			retval = true;
		else if (name.equals("byte"))
			retval = true;
		else if (name.equals("short"))
			retval = true;
		else if (name.equals("int"))
			retval = true;
		else if (name.equals("long"))
			retval = true;
		else if (name.equals("float"))
			retval = true;
		else if (name.equals("double"))
			retval = true;
		else if (name.equals("char"))
			retval = true;

		return retval;
	}

	public void addVariable(String name, String type) {
		if (!varNameTypeMap.containsKey(name)) {
			AstVisitor.log(0, "Adding variable to (" + this.getClass().getName() + "," + this.getClass().hashCode() + "): " + type + " " + name);

			varNameTypeMap.put(name, type);
		}
		// ToDo - Add logic to investigate the type and determine if it belongs to classDependency or unresolvedClasses
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

	public Class searchForUnresolvedClass(String className) {
		AstVisitor.log(5, "DependentBase.searchForUnresolvedClass(" + className + ")");
		Class matchedClass = classDependencies.get(className);
		
		if (matchedClass == null) {
			if (parent != null)
				matchedClass =  parent.searchForUnresolvedClass(className);
		}
		return matchedClass;
	}

	public Class searchForVariableClass(String variableName) {
		AstVisitor.log(1, "DependentBase.searchForVariable(" + variableName + ")");
		Class matchedClass = varNameClassMap.get(variableName);
		
		if (matchedClass == null) {
			if (parent != null)
				matchedClass =  parent.searchForVariableClass(variableName);
		}
		return matchedClass;
	}

	public void validatePassOne() {
		Class c = findClass();

		if (this instanceof Method) {
			AstVisitor.log(4, ((Method) this).name);
		} else {
			AstVisitor.log(4, "Validating Block");
		}

		for (String s : this.unresolvedInterfaces) {
			AstVisitor.log(4, "Class " + c.getName() + ": Searching for unresolved interfaces: " + s);
			Class clazz = searchForUnresolvedClass(s);
			if (clazz != null) {
				AstVisitor.log(5, "Matched unresolved interface: " + s + " to " + clazz.getCanonicalName());
				addResolvedClass(clazz);
				this.annotationDependencies.add(clazz);
			}
		}

		for (String s : this.unresolvedAnnotations) {
			AstVisitor.log(4, "Class " + c.getName() + ": Searching for unresolved annotation: " + s);
			Class clazz = searchForUnresolvedClass(s);
			if (clazz != null) {
				AstVisitor.log(5, "Matched unresolved annotation: " + s + " to " + clazz.getCanonicalName());
				addResolvedClass(clazz);
				this.annotationDependencies.add(clazz);
			}
		}

		for (String s : this.unresolvedClasses) {
			AstVisitor.log(4, "Class " + c.getName() + ": Searching for unresolved class: " + s);
			Class clazz = searchForUnresolvedClass(s);
			if (clazz != null) {
				AstVisitor.log(5, "Matched unresolved class: " + s + " to " + clazz.getCanonicalName());
				addResolvedClass(clazz);
			}
		}

		for (String varName : varNameTypeMap.keySet()) {
			String type = varNameTypeMap.get(varName);

			if (type.contains("<")) { // Generics
				type = type.substring(0, type.indexOf("<"));
			}
			
			AstVisitor.log(0, "Class " + c.getName() + ": Searching for class for variable: " + type + " " + varName + " " + isPrimative(type) + " " + "this".equals(type));
			if (!(isPrimative(type) || "this".equals(type))) {
				Class clazz = searchForUnresolvedClass(type);

				if (clazz != null) {
					AstVisitor.log(0, "Matched unresolved class: " + type + " to " + clazz.getCanonicalName());
					addResolvedClass(clazz);
					varNameClassMap.put(varName, clazz);
				}
			}
		}

		if (this instanceof Block) {
			Block b = (Block) this;
			for (Block block : b.childBlocks) {
				block.validatePassOne();
			}
		}

	}

	public void validatePassTwo() {
		AstVisitor.log(2, "DependentBase.validatePassTwo()");
		
		for (String typeOrVarName : unresolvedMethods.keySet()) {
			Class clazz = null;
			
			if ("this".equals(typeOrVarName)) {
				clazz = findClass();
			} else if ("super".equals(typeOrVarName)) {
				clazz = findClass().extnds;
			} else if ("String".equals(typeOrVarName)) {
				searchForUnresolvedClass("String");
			} else if ("null".equals(typeOrVarName) || typeOrVarName == null) {
			} else {
				clazz = searchForVariableClass(typeOrVarName);
				
				if (clazz == null) {
					clazz = searchForUnresolvedClass(typeOrVarName);
				}
			}
			
			if (clazz != null) {
				AstVisitor.log(3, "DependentBase.validatePassTwo() for " + findClass().name + ": typeOrVarName " + typeOrVarName + " matched to " + clazz.name);

				for (String methodName : unresolvedMethods.get(typeOrVarName)) {
					HashSet<Method> methods = methodCallMap.get(clazz);
					if (methods == null) {
						methods = new HashSet<Method>();
						methodCallMap.put(clazz, methods);
					}
					
					Method method = clazz.getOrCreateAndGetMethod(methodName + "()");
					methods.add(method);

					AstVisitor.log(4, "Found Method Call Reference: " + clazz.getCanonicalName() + "." + method.name);

				}
			}
		}
		
		if (this instanceof Block) {
			Block b = (Block) this;
			for (Block block : b.childBlocks) {
				block.validatePassTwo();
			}
		}
	}

	public void addUnresolvedMethodCall(String typeOrVarName, String methodName) {
		HashSet<String> methods = unresolvedMethods.get(varNameTypeMap);
		if (methods == null) {
			methods = new HashSet<String>();
			unresolvedMethods.put(typeOrVarName, methods);
		}
		methods.add(methodName);
	}

}