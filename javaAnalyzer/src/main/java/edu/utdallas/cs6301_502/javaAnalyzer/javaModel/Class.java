package edu.utdallas.cs6301_502.javaAnalyzer.javaModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import edu.utdallas.cs6301_502.javaAnalyzer.AstVisitor;

import edu.utdallas.cs6301_502.javaAnalyzer.viewFilter.DiagramType;
import edu.utdallas.cs6301_502.javaAnalyzer.viewFilter.JavaFilter;
import net.networkdowntime.renderer.GraphvizRenderer;

public class Class extends DependentBase {

	String name;
	Package pkg;
	boolean isInterface = false;
	boolean isAbstract = false;
	boolean isAnnotation = false;
	boolean isEnum = false;

	List<Package> packageDependencies = new ArrayList<Package>();
	Map<String, Method> methods = new LinkedHashMap<String, Method>();
	List<String> imports = new ArrayList<String>();

	Class extnds = null; // this is deferred for now
	String extndsStr = null;
	List<Class> impls = new ArrayList<Class>();
	List<String> implsStrings = new ArrayList<String>();
	boolean fromFile = false;

	HashSet<Class> referencedByClass = new HashSet<Class>();

	public Class(Package pkg, String name, boolean isInterface,
			boolean isAbstract, boolean isEnum, boolean isAnnotation) {
		this.pkg = pkg;
		this.name = name;
		this.isInterface = isInterface;
		this.isAbstract = isAbstract;
		this.isEnum = isEnum;
		this.isAnnotation = isAnnotation;
		AstVisitor.log(2, "Creating Class: " + pkg.getName() + "." + name);
	}

	public void addImplsStr(String implsString) {
		this.implsStrings.add(implsString);
	}

	public void setExtendsStr(String extndsString) {
		this.extndsStr = extndsString;
	}

	public String getName() {
		return this.name;
	}

	public String getExtends() {
		return this.extndsStr;
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

	public Method getMethod(String name) {
		Method method = methods.get(name);
		return method;
	}

	public void addImport(String name) {
		if (name != null && name.length() > 0) {
			AstVisitor.log(4, "Adding import for: " + name);
			imports.add(name);
			String pkgOrClassName = name.substring(name.lastIndexOf(".") + 1);
			boolean isClass = Character.isUpperCase(pkgOrClassName.charAt(0));

			if (!isClass) {
				Package pkg1 = this.pkg.prj.getOrCreateAndGetPackage(name,
						false);
				this.packageDependencies.add(pkg1);
			} else {
				String pkgName = name.substring(0, name.lastIndexOf("."));
				String className = pkgOrClassName;
				Package pkg1 = this.pkg.prj.getOrCreateAndGetPackage(pkgName,
						false);
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
		AstVisitor.log(5, "Class.searchForUnresolvedClass(" + className + ")");

		Class matchedClass = super.searchForUnresolvedClass(className);

		if (matchedClass == null) {
			matchedClass = pkg.searchForUnresolvedClass(name, className);
		}
		return matchedClass;
	}

	public void validatePassOne() {
		AstVisitor.log(3, "Validating Pass One class: " + getCanonicalName());

		AstVisitor.log(4, "Checking for extends:");
		if (extndsStr != null) {
			Class clazz = pkg.searchForUnresolvedClass(name, extndsStr);
			if (clazz != null) {
				extnds = clazz;
				addResolvedClass(clazz);
				AstVisitor.log(5, "Resolved extends to class: " + clazz.getCanonicalName());
			}
		}

		AstVisitor.log(4, "Checking for implements:");
		if (implsStrings != null) {
			for (String implString : implsStrings) {
				Class clazz = pkg.searchForUnresolvedClass(name, implString);
				if (clazz != null) {
					impls.add(clazz);
					addResolvedClass(clazz);
					AstVisitor.log(5, "Resolved implements to class: " + clazz.getCanonicalName());
				}
			}
		}

		super.validatePassOne();

		for (Method method : methods.values()) {
			method.validatePassOne();
		}
	}

	public void validatePassTwo() {
		AstVisitor.log(1, "Validating Pass Two class: " + getCanonicalName());

		super.validatePassTwo();

		List<Method> tmpMethods = new ArrayList<Method>();
		tmpMethods.addAll(methods.values());
		for (Method method : tmpMethods) {
			method.validatePassTwo();
		}
	}

	public String createGraph(GraphvizRenderer renderer, JavaFilter filter) {
		AstVisitor.log(1, "Class: " + this.name);

		HashSet<String> refsToSkip = new HashSet<String>();

		StringBuffer sb = new StringBuffer();

		if ((filter.getDiagramType() == DiagramType.UNREFERENCED_CLASSES && this.referencedByClass
				.size() == 0)
				|| filter.getDiagramType() != DiagramType.UNREFERENCED_CLASSES) {
			sb.append(renderer.getBeginRecord(this.pkg.name + "." + this.name,
					this.name, ""));

			if (filter.isShowFields()) {
				for (String field : this.varNameClassMap.keySet()) {
					Class clazz = this.varNameClassMap.get(field);
					sb.append(renderer.addRecordField(field, field + ": "
							+ clazz.name));
				}
			}

			if (filter.isShowMethods()) {
				for (Method method : methods.values()) {
					sb.append(renderer.addRecordField(method.name, method.name));
				}
			}

			sb.append(renderer.getEndRecord());

			// Add edge for extending another class, if only 1 reference to that
			// class don't add a reference edge later
			if (extnds != null) {
				boolean exclude = filter.getPackagesToExclude().contains(
						extnds.pkg.name);
				exclude = exclude
						|| filter.getClassesToExclude().contains(
								extnds.pkg.name + "." + extnds.name);
				if (!exclude) {
					sb.append(renderer.addEdge(this.pkg.name + "." + this.name,
							extnds.pkg.name + "." + extnds.name, "", true));

					Integer count = this.unresolvedClassCount.get(extnds.name);
					if (count != null && count.intValue() == 1) {
						refsToSkip.add(extnds.name);
					}
				}
			}

			// Add edges for implementing interfaces, if only 1 reference to
			// that class don't add a reference edge later
			for (Class clazz : this.impls) {
				sb.append(renderer.addEdge(this.pkg.name + "." + this.name,
						clazz.pkg.name + "." + clazz.name, "", true));
				Integer count = this.unresolvedClassCount.get(clazz.name);
				if (count != null && count.intValue() == 1) {
					boolean exclude = filter.getPackagesToExclude().contains(
							clazz.pkg.name);
					exclude = exclude
							|| filter.getClassesToExclude().contains(
									clazz.pkg.name + "." + clazz.name);
					if (!exclude)
						refsToSkip.add(clazz.name);
				}
			}

			for (Class clazz : this.classDependencies.values()) {
				if (!refsToSkip.contains(clazz.name)) {
					boolean exclude = filter.getPackagesToExclude().contains(
							clazz.pkg.name);
					exclude = exclude
							|| filter.getClassesToExclude().contains(
									clazz.pkg.name + "." + clazz.name);
					if (!exclude) {
						if ((filter.isFromFile() && clazz.fromFile)
								|| !filter.isFromFile()) {
							if ((filter.getDiagramType() == DiagramType.UNREFERENCED_CLASSES && clazz.referencedByClass
									.size() == 0)
									|| filter.getDiagramType() != DiagramType.UNREFERENCED_CLASSES) {
								sb.append(renderer.addEdge(this.pkg.name + "."
										+ this.name, clazz.pkg.name + "."
										+ clazz.name, ""));
							}
						}
					}
				}
			}

		}
		return sb.toString();
	}

	public void addReferencedByClass(Class referencingClass) {
		referencedByClass.add(referencingClass);
	}

}