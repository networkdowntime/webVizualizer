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

public class Class extends DependentBase implements Comparable<Class> {

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

	public Class(int depth, Package pkg, String name, boolean isInterface,
			boolean isAbstract, boolean isEnum, boolean isAnnotation) {
		this.pkg = pkg;
		this.name = name;
		this.isInterface = isInterface;
		this.isAbstract = isAbstract;
		this.isEnum = isEnum;
		this.isAnnotation = isAnnotation;
		AstVisitor.log(depth, "Creating Class: " + pkg.getName() + "." + name);
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

	//	public String getCanonicalName() {
	//		return this.pkg.getName() + "." + this.name;
	//	}

	public Method getOrCreateAndGetMethod(int depth, String name) {
		Method method = methods.get(name);
		if (method == null) {
			method = new Method(depth, this, name);
			methods.put(name, method);
		}
		return method;
	}

	public Method getMethod(String name) {
		Method method = methods.get(name);
		return method;
	}

	public void addImport(int depth, String name) {
		if (name != null && name.length() > 0) {
			AstVisitor.log(depth, "Adding import for: " + name);
			imports.add(name);
			String pkgOrClassName = name.substring(name.lastIndexOf(".") + 1);
			boolean isClass = Character.isUpperCase(pkgOrClassName.charAt(0));

			if (!isClass) {
				Package pkg1 = this.pkg.prj.getOrCreateAndGetPackage(depth + 1, name,
						false);
				this.packageDependencies.add(pkg1);
			} else {
				String pkgName = name.substring(0, name.lastIndexOf("."));
				String className = pkgOrClassName;
				Package pkg1 = this.pkg.prj.getOrCreateAndGetPackage(depth, pkgName,
						false);
				this.packageDependencies.add(pkg1);
				pkg1.getOrCreateAndGetClass(depth, className);
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

	@Override
	public Class searchForUnresolvedClass(int depth, String className) {
		AstVisitor.log(depth, "Class.searchForUnresolvedClass(" + className + ")");

		Class matchedClass = null;
		if (getCanonicalName().equals(className)) {
			matchedClass = this;
		} else {
			matchedClass = super.searchForUnresolvedClass(depth + 1, className);
		}

		if (matchedClass == null) {
			matchedClass = pkg.searchForUnresolvedClass(depth + 1, name, className);
		}
		return matchedClass;
	}

	@Override
	public void validatePassOne(int depth) {
		AstVisitor.log(depth, "Validating Pass One class: " + getCanonicalName());

		AstVisitor.log(depth + 1, "Checking for extends:");
		if (extndsStr != null) {
			Class clazz = pkg.searchForUnresolvedClass(depth, name, extndsStr);
			if (clazz != null) {
				extnds = clazz;
				addResolvedClass(clazz);
				AstVisitor.log(depth + 2, "Resolved extends to class: " + clazz.getCanonicalName());
			}
		} else {
			AstVisitor.log(depth + 2, "No extends needed for this class");
		}

		AstVisitor.log(depth + 1, "Checking for implements:");
		if (implsStrings != null && !implsStrings.isEmpty()) {
			for (String implString : implsStrings) {
				Class clazz = pkg.searchForUnresolvedClass(depth, name, implString);
				if (clazz != null && !impls.contains(clazz)) {
					impls.add(clazz);
					addResolvedClass(clazz);
					AstVisitor.log(depth + 2, "Resolved implements to class: " + clazz.getCanonicalName());
				}
			}
		} else {
			AstVisitor.log(depth + 2, "No implements needed for this class");
		}

		super.validatePassOne(depth + 1);

		AstVisitor.log(depth + 1, "Checking for methods:");
		if (methods != null && !methods.isEmpty()) {
			for (Method method : methods.values()) {
				method.validatePassOne(depth + 2);
			}
		} else {
			AstVisitor.log(depth + 2, "No methods for this class");
		}
	}

	@Override
	public int validatePassTwo(int depth) {
		AstVisitor.log(depth, "Validating Pass Two class: " + getCanonicalName());

		List<Method> tmpMethods = new ArrayList<Method>();
		tmpMethods.addAll(methods.values());
		for (Method method : tmpMethods) {
			method.validatePassTwo(depth + 1);
		}

		super.validatePassTwo(depth + 1);

		return depth + 1;
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
				boolean exclude = filter.getPackagesToExclude().contains(extnds.pkg.name);
				exclude = exclude || filter.getClassesToExclude().contains(extnds.pkg.name + "." + extnds.name);
				if (filter.isFromFile())
					exclude = exclude || (filter.isFromFile() && !extnds.fromFile);

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
			for (Class intr : this.impls) {
				boolean exclude = filter.getPackagesToExclude().contains(intr.pkg.name);
				exclude = exclude || filter.getClassesToExclude().contains(intr.pkg.name + "." + intr.name);
				if (filter.isFromFile())
					exclude = exclude || (filter.isFromFile() && !intr.fromFile);

				if (!exclude) {
					sb.append(renderer.addEdge(this.pkg.name + "." + this.name,	intr.pkg.name + "." + intr.name, "", true));

					Integer count = this.unresolvedClassCount.get(intr.name);
					if (count != null && count.intValue() == 1) {
						refsToSkip.add(intr.name);
					}
				}
			}

			for (Class clazz : this.classDependencies.values()) {
				if (!refsToSkip.contains(clazz.name)) {
					boolean exclude = filter.getPackagesToExclude().contains(clazz.pkg.name);
					exclude = exclude || filter.getClassesToExclude().contains(clazz.pkg.name + "." + clazz.name);
					if (!exclude) {
						if ((filter.isFromFile() && clazz.fromFile)	|| !filter.isFromFile()) {
							if ((filter.getDiagramType() == DiagramType.UNREFERENCED_CLASSES && clazz.referencedByClass.size() == 0)
									|| filter.getDiagramType() != DiagramType.UNREFERENCED_CLASSES) {
								sb.append(renderer.addEdge(this.pkg.name + "." + this.name, clazz.pkg.name + "." + clazz.name, ""));
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

	@Override
	public int hashCode() {
		return getCanonicalName().hashCode();
	}

	@Override
	public int compareTo(Class clazz) {
		return this.getCanonicalName().compareTo(clazz.getCanonicalName());
	}

}