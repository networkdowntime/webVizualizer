package net.networkdowntime.javaAnalyzer.javaModel;
import java.util.HashMap;
import java.util.HashSet;

import net.networkdowntime.javaAnalyzer.JavaAnalyzer;


public abstract class DependentBase {

	DependentBase parent;

	HashSet<Class> annotationDependencies = new HashSet<Class>();
	HashMap<String, Class> classDependencies = new HashMap<String, Class>();
	// HashSet<Method> methodDependencies = new HashSet<Method>();

	HashSet<String> unresolvedAnnotations = new HashSet<String>();
	HashSet<String> unresolvedClasses = new HashSet<String>();
	HashMap<String, Integer> unresolvedClassCount = new HashMap<String, Integer>(); 

	// HashSet<String> unresolvedMethods = new HashSet<String>();

	HashMap<String, String> varNameTypeMap = new HashMap<String, String>(); // This is the unqualified Type
	HashMap<String, Class> varNameClassMap = new HashMap<String, Class>();

	public void setClass(DependentBase clazz) {
		this.parent = clazz;
	}

	public DependentBase getParent() {
		return parent;
	}

	public void addUnresolvedAnnotations(String annotationName) {
		if (!this.unresolvedAnnotations.contains(annotationName)) {
			this.unresolvedAnnotations.add(annotationName);
			JavaAnalyzer.log(0, "Adding unresolved annotation: " + annotationName);
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
			JavaAnalyzer.log(0, "Adding unresolved class: " + className);
		}
	}

	public void addResolvedClass(Class clazz) {
		classDependencies.put(clazz.getName(), clazz);
		if (parent != null) {
			parent.addResolvedClass(clazz);
		}
	}

	private boolean isPrimative(String name) {
		if (name.equals("boolean"))
			return true;
		if (name.equals("byte"))
			return true;
		if (name.equals("short"))
			return true;
		if (name.equals("int"))
			return true;
		if (name.equals("long"))
			return true;
		if (name.equals("float"))
			return true;
		if (name.equals("double"))
			return true;
		if (name.equals("char"))
			return true;
		return false;
	}

	public void addVariable(String name, String type) {
		if (!varNameTypeMap.containsKey(name)) {
			JavaAnalyzer.log(0, "Adding variable: " + type + " " + name);
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
		JavaAnalyzer.log(1, "DependentBase.searchForUnresolvedClass(" + className + ")");
		Class matchedClass = classDependencies.get(className);
		
		if (matchedClass == null) {
			if (parent != null)
				matchedClass =  parent.searchForUnresolvedClass(className);
		}
		return matchedClass;
	}

	public void validate() {
		Class c = findClass();

		if (this instanceof Method) {
			JavaAnalyzer.log(0, ((Method) this).name);
		} else {
			JavaAnalyzer.log(0, "Validating Block");
		}

		for (String s : this.unresolvedAnnotations) {
			JavaAnalyzer.log(0, "Class " + c.getName() + ": Searching for unresolved annotation: " + s);
			Class clazz = searchForUnresolvedClass(s);
			if (clazz != null) {
				JavaAnalyzer.log(0, "Matched unresolved class: " + s + " to " + clazz.getCanonicalName());
				addResolvedClass(clazz);
				this.annotationDependencies.add(clazz);
			}
		}

		for (String s : this.unresolvedClasses) {
			JavaAnalyzer.log(0, "Class " + c.getName() + ": Searching for unresolved class: " + s);
			Class clazz = searchForUnresolvedClass(s);
			if (clazz != null) {
				JavaAnalyzer.log(0, "Matched unresolved class: " + s + " to " + clazz.getCanonicalName());
				addResolvedClass(clazz);
			}
		}

		for (String varName : varNameTypeMap.keySet()) {
			String type = varNameTypeMap.get(varName);

			if (type.contains("<")) { // Generics
				type = type.substring(0, type.indexOf("<"));
			}
			
			JavaAnalyzer.log(0, "Class " + c.getName() + ": Searching for class for variable: " + type + " " + varName);
			if (!isPrimative(type) || !"this".equals(type)) {
				Class clazz = searchForUnresolvedClass(type);

				if (clazz != null) {
					JavaAnalyzer.log(0, "Matched unresolved class: " + type + " to " + clazz.getCanonicalName());
					addResolvedClass(clazz);
					varNameClassMap.put(varName, clazz);
				}
			}
		}

		if (this instanceof Block) {
			Block b = (Block) this;
			for (Block block : b.childBlocks) {
				block.validate();
			}
		}

	}

}
