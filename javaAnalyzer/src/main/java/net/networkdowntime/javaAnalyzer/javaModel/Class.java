package net.networkdowntime.javaAnalyzer.javaModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Class extends DependentBase implements Comparable<Class> {
	
	private Package pkg;
	boolean isInterface = false;
	boolean isAbstract = false;
	boolean isAnnotation = false;
	boolean isAnonymous = false;
	boolean isEnum = false;

	List<Package> packageDependencies = new ArrayList<Package>();
	private Map<String, Method> methods = new LinkedHashMap<String, Method>();
	List<String> imports = new ArrayList<String>();

	private Class extnds = null; // this is deferred for now
	String extndsStr = null;
	private List<Class> impls = new ArrayList<Class>();
	List<String> implsStrings = new ArrayList<String>();
	boolean fromFile = false;
	String fileName = null;
	DependentBase anonymousClassDefinedIn = null;

	HashSet<Class> referencedByClass = new HashSet<Class>();
	HashSet<Package> referencedByPackage = new HashSet<Package>();

	Integer searchRank = new Integer(0);

	public Class(int depth, Package pkg, String name, boolean isInterface,
			boolean isAbstract, boolean isEnum, boolean isAnnotation) {
		this.pkg = pkg;
		this.name = name;
		this.isInterface = isInterface;
		this.isAbstract = isAbstract;
		this.isEnum = isEnum;
		this.isAnnotation = isAnnotation;
		logIndented(depth, "Creating Class: " + pkg.getName() + "." + name);
	}

	public void accept(ModelVisitor visitor) {
		visitor.visit(this);
	}

	public void addImplsStr(String implsString) {
		this.implsStrings.add(implsString);
	}

	public void setExtendsStr(String extndsString) {
		this.extndsStr = extndsString;
	}

	public String getExtends() {
		return this.extndsStr;
	}

	public Method getOrCreateAndGetMethod(int depth, String name) {
		Method method = getMethods().get(name);
		if (method == null) {
			method = new Method(depth, this, name);
			getMethods().put(name, method);
		}
		return method;
	}

	public Method getMethod(String name) {
		Method method = getMethods().get(name);
		return method;
	}

	public void addImport(int depth, String name) {
		if (name != null && name.length() > 0) {
			logIndented(depth, "Adding import for: " + name);
			imports.add(name);
			String pkgOrClassName = name.substring(name.lastIndexOf(".") + 1);
			boolean isClass = Character.isUpperCase(pkgOrClassName.charAt(0));

			if (!isClass) {
				Package pkg1 = getPkg().getOrCreateAndGetPackage(depth + 1, name, false, false);
				this.packageDependencies.add(pkg1);
			} else {
				String pkgName = name.substring(0, name.lastIndexOf("."));
				String className = pkgOrClassName;
				Package pkg1 = getPkg().getOrCreateAndGetPackage(depth, pkgName, false, false);
				this.packageDependencies.add(pkg1);
				pkg1.getOrCreateAndGetClass(depth, className);
			}

		}
	}

	public void setIsInterface(boolean isInterface) {
		this.isInterface = isInterface;
	}

	public void setIsAbstract(boolean isAbstract) {
		this.isAbstract = isAbstract;
	}

	public void setIsAnnotation(boolean isAnnotation) {
		this.isAnnotation = isAnnotation;
	}

	public void setIsAnonymous(boolean isAnonymous, DependentBase anonymousClassDefinedIn) {
		this.isAnonymous = isAnonymous;
		this.anonymousClassDefinedIn = anonymousClassDefinedIn;
	}

	public void setIsEnum(boolean isEnum) {
		this.isEnum = isEnum;
	}

	@Override
	public Class searchForUnresolvedClass(int depth, String className) {
		logIndented(depth, "Class.searchForUnresolvedClass(" + className + ")");

		Class matchedClass = null;
		if (getCanonicalName().equals(className)) {
			matchedClass = this;
		} else {
			matchedClass = super.searchForUnresolvedClass(depth + 1, className);
		}

		if (matchedClass == null) {
			for (String importedClass : imports) {
				if (importedClass.endsWith("." + className)) {
					matchedClass = getPkg().searchForUnresolvedClass(depth + 1, null, importedClass, true);
					break;
				}
			}
		}

		if (matchedClass == null) {
			matchedClass = getPkg().searchForUnresolvedClass(depth + 1, name, className, false);
		}

		return matchedClass;
	}

	@Override
	public void validatePassOne(int depth) {
		logIndented(depth, "Validating Pass One class: " + getCanonicalName());

		logIndented(depth + 1, "Checking for extends:");
		if (extndsStr != null) {
			Class clazz = getPkg().searchForUnresolvedClass(depth, name, extndsStr, true);
			if (clazz != null) {
				this.extnds = clazz;
				addResolvedClass(clazz);
				logIndented(depth + 2, "Resolved extends to class: " + clazz.getCanonicalName());
			}
		} else {
			logIndented(depth + 2, "No extends needed for this class");
		}

		logIndented(depth + 1, "Checking for implements:");
		if (implsStrings != null && !implsStrings.isEmpty()) {
			for (String implString : implsStrings) {
				Class clazz = getPkg().searchForUnresolvedClass(depth, name, implString, true);
				if (clazz != null && !getImplements().contains(clazz)) {
					getImplements().add(clazz);
					addResolvedClass(clazz);
					logIndented(depth + 2, "Resolved implements to class: " + clazz.getCanonicalName());
				}
			}
		} else {
			logIndented(depth + 2, "No implements needed for this class");
		}

		super.validatePassOne(depth + 1);

		logIndented(depth + 1, "Checking for methods:");
		if (getMethods() != null && !getMethods().isEmpty()) {
			for (Method method : getMethods().values()) {
				method.validatePassOne(depth + 2);
			}
		} else {
			logIndented(depth + 2, "No methods for this class");
		}
	}

	@Override
	public int validatePassTwo(int depth) {
		logIndented(depth, "Validating Pass Two class: " + getCanonicalName());

		List<Method> tmpMethods = new ArrayList<Method>();
		tmpMethods.addAll(getMethods().values());
		for (Method method : tmpMethods) {
			method.validatePassTwo(depth + 1);
		}

		super.validatePassTwo(depth + 1);

		return depth + 1;
	}

	public void addReferencedByClass(Class referencingClass) {
		referencedByClass.add(referencingClass);
		referencedByPackage.add(referencingClass.getPkg());
	}

	@Override
	public int hashCode() {
		return getCanonicalName().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Class other = (Class) obj;
		if (!getCanonicalName().equals(other.getCanonicalName()))
			return false;
		return true;
	}

	@Override
	public int compareTo(Class clazz) {
		return this.getCanonicalName().compareTo(clazz.getCanonicalName());
	}

	public boolean isFromFile() {
		return fromFile;
	}

	public List<Package> getPackageDependencies() {
		return packageDependencies;
	}

	public Package getPackage() {
		return this.getPkg();
	}

	public Set<Class> getReferencedByClass() {
		return this.referencedByClass;
	}

	public Set<Package> getReferencedByPackage() {
		return this.referencedByPackage;
	}

	public boolean isAnonymous() {
		return this.isAnonymous;
	}

	public boolean isInterface() {
		return this.isInterface;
	}

	public Map<String, Method> getMethods() {
		return methods;
	}

	public Class getExtnds() {
		return extnds;
	}

	public Package getPkg() {
		return pkg;
	}

	public List<Class> getImplements() {
		return impls;
	}

}