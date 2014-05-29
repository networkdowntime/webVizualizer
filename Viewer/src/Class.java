import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Class extends DependentBase {

	String name;
	Package pkg;
	boolean isInterface = false;
	boolean isAbstract = false;
	boolean isAnnotation = false;
	boolean isEnum = false;

	List<Package> packageDependencies = new ArrayList<Package>();
	Map<String, Method> methods = new HashMap<String, Method>();
	List<String> imports = new ArrayList<String>();

	Class extnds = null; // this is deferred for now
	String extndsStr = null;
	List<Class> impls = new ArrayList<Class>();

	public Class(Package pkg, String name, boolean isInterface, boolean isAbstract) {
		this.pkg = pkg;
		this.name = name;
		this.isInterface = isInterface;
		this.isAbstract = isAbstract;
		System.out.println("\t\tCreating Class: " + pkg.getName() + "." + name);
	}

	public void setExtendsStr(String extndsString) {
		this.extndsStr = extndsString;
	}

	public String getName() {
		return this.name;
	}

	public String getCanonicalName() {
		return this.pkg.getName() + "." + this.name;
	}

	public Method getOrCreateAndGetMethod(String name) {
		Method method = methods.get(name);
		if (method == null) {
			method = new Method(this, name);
			methods.put(name, method);
		}
		return method;
	}

	public void addImport(String name) {
		if (name != null && name.length() > 0) {
			imports.add(name);
			String pkgOrClassName = name.substring(name.lastIndexOf(".") + 1);
			boolean isClass = Character.isUpperCase(pkgOrClassName.charAt(0));

			if (!isClass) {
				Package pkg1 = this.pkg.prj.getOrCreateAndGetPackage(name);
				this.packageDependencies.add(pkg1);
			} else {
				String pkgName = name.substring(0, name.lastIndexOf("."));
				String className = pkgOrClassName;
				Package pkg1 = this.pkg.prj.getOrCreateAndGetPackage(pkgName);
				this.packageDependencies.add(pkg1);
				pkg1.getOrCreateAndGetClass(className);
			}

		}
	}

	public void setIsInterface(boolean isInterface) {
		this.isInterface = isInterface;
	}

	public void setIsAnnotation(boolean isAnnotation) {
		this.isAnnotation = isAnnotation;
	}

	public void setIsEnum(boolean isEnum) {
		this.isEnum = isEnum;
	}

	public void setIsAbstract(boolean isAbstract) {
		this.isAbstract = isAbstract;
	}

	public Class searchForUnresolvedClass(String className) {
		System.out.println("\tClass.searchForUnresolvedClass(" + className + ")");

		Class matchedClass = super.searchForUnresolvedClass(className);

		if (matchedClass == null) {
			matchedClass = pkg.searchForUnresolvedClass(name, className);
		}
		return matchedClass;
	}

	public void validate() {
		System.out.println("\nValidating class: " + getCanonicalName());

		if (extndsStr != null) {
			Class clazz = pkg.searchForUnresolvedClass(name, extndsStr);
			if (clazz != null) {
				extnds = clazz;
				addResolvedClass(clazz);
			}
		}

		// ToDo: Need to properly handle implements

		super.validate();

		for (Method method : methods.values()) {
			method.validate();
		}
	}

}
