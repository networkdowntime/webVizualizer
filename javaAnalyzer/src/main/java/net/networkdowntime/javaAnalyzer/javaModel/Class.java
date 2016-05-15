package net.networkdowntime.javaAnalyzer.javaModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.networkdowntime.javaAnalyzer.logger.Logger;
import net.networkdowntime.javaAnalyzer.viewFilter.DiagramType;
import net.networkdowntime.javaAnalyzer.viewFilter.JavaFilter;
import net.networkdowntime.renderer.GraphvizRenderer;

public class Class extends DependentBase implements Comparable<Class> {

	Package pkg;
	boolean isInterface = false;
	boolean isAbstract = false;
	boolean isAnnotation = false;
	boolean isAnonymous = false;
	boolean isEnum = false;

	
	List<Package> packageDependencies = new ArrayList<Package>();
	Map<String, Method> methods = new LinkedHashMap<String, Method>();
	List<String> imports = new ArrayList<String>();

	Class extnds = null; // this is deferred for now
	String extndsStr = null;
	List<Class> impls = new ArrayList<Class>();
	List<String> implsStrings = new ArrayList<String>();
	boolean fromFile = false;
	String fileName = null;
	DependentBase anonymousClassDefinedIn = null;

	HashSet<Class> referencedByClass = new HashSet<Class>();
	HashSet<Package> referencedByPackage = new HashSet<Package>();

	Integer searchRank = new Integer(0);
	Integer upstreamReferenceDepth = new Integer(0);
	Integer downstreamReferenceDepth = new Integer(0);
	
	public Class(int depth, Package pkg, String name, boolean isInterface,
			boolean isAbstract, boolean isEnum, boolean isAnnotation) {
		this.pkg = pkg;
		this.name = name;
		this.isInterface = isInterface;
		this.isAbstract = isAbstract;
		this.isEnum = isEnum;
		this.isAnnotation = isAnnotation;
		Logger.log(depth, "Creating Class: " + pkg.getName() + "." + name);
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
			Logger.log(depth, "Adding import for: " + name);
			imports.add(name);
			String pkgOrClassName = name.substring(name.lastIndexOf(".") + 1);
			boolean isClass = Character.isUpperCase(pkgOrClassName.charAt(0));

			if (!isClass) {
				Package pkg1 = pkg.getOrCreateAndGetPackage(depth + 1, name, false, false);
				this.packageDependencies.add(pkg1);
			} else {
				String pkgName = name.substring(0, name.lastIndexOf("."));
				String className = pkgOrClassName;
				Package pkg1 = pkg.getOrCreateAndGetPackage(depth, pkgName, false, false);
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

	public Integer getUpstreamReferenceDepth() {
		return upstreamReferenceDepth;
	}

	public void setUpstreamReferenceDepth(Integer upstreamReferenceDepth) {
		this.upstreamReferenceDepth = upstreamReferenceDepth;
	}

	public Integer getDownstreamReferenceDepth() {
		return downstreamReferenceDepth;
	}

	public void setDownstreamReferenceDepth(Integer downstreamReferenceDepth) {
		this.downstreamReferenceDepth = downstreamReferenceDepth;
	}

	
	@Override
	public Class searchForUnresolvedClass(int depth, String className) {
		Logger.log(depth, "Class.searchForUnresolvedClass(" + className + ")");

		Class matchedClass = null;
		if (getCanonicalName().equals(className)) {
			matchedClass = this;
		} else {
			matchedClass = super.searchForUnresolvedClass(depth + 1, className);
		}

		if (matchedClass == null) {
			for (String importedClass : imports) {
				if (importedClass.endsWith("." + className)) {
					matchedClass = pkg.searchForUnresolvedClass(depth + 1, null, importedClass, true);
					break;
				}
			}
		}
		
		if (matchedClass == null) {
			matchedClass = pkg.searchForUnresolvedClass(depth + 1, name, className, false);
		}
		
		return matchedClass;
	}

	@Override
	public void validatePassOne(int depth) {
		Logger.log(depth, "Validating Pass One class: " + getCanonicalName());

		Logger.log(depth + 1, "Checking for extends:");
		if (extndsStr != null) {
			Class clazz = pkg.searchForUnresolvedClass(depth, name, extndsStr, true);
			if (clazz != null) {
					extnds = clazz;
				addResolvedClass(clazz);
				Logger.log(depth + 2, "Resolved extends to class: " + clazz.getCanonicalName());
			}
		} else {
			Logger.log(depth + 2, "No extends needed for this class");
		}

		Logger.log(depth + 1, "Checking for implements:");
		if (implsStrings != null && !implsStrings.isEmpty()) {
			for (String implString : implsStrings) {
				Class clazz = pkg.searchForUnresolvedClass(depth, name, implString, true);
				if (clazz != null && !impls.contains(clazz)) {
					impls.add(clazz);
					addResolvedClass(clazz);
					Logger.log(depth + 2, "Resolved implements to class: " + clazz.getCanonicalName());
				}
			}
		} else {
			Logger.log(depth + 2, "No implements needed for this class");
		}

		super.validatePassOne(depth + 1);

		Logger.log(depth + 1, "Checking for methods:");
		if (methods != null && !methods.isEmpty()) {
			for (Method method : methods.values()) {
				method.validatePassOne(depth + 2);
			}
		} else {
			Logger.log(depth + 2, "No methods for this class");
		}
	}

	@Override
	public int validatePassTwo(int depth) {
		Logger.log(depth, "Validating Pass Two class: " + getCanonicalName());

		List<Method> tmpMethods = new ArrayList<Method>();
		tmpMethods.addAll(methods.values());
		for (Method method : tmpMethods) {
			method.validatePassTwo(depth + 1);
		}

		super.validatePassTwo(depth + 1);

		return depth + 1;
	}

	// A value > 0xFF for any color means that 
	// the value should not be used
	private int mixColorToRGBValue(int red, int green, int blue)
	{
		int color = 0xFFFFFF; // white
				
		if (red < 0x100 || green < 0x100 || blue < 0x100)
		{
			// Limit negative values
			red = Math.max(0, red);
			green = Math.max(0, green);
			blue = Math.max(0, blue);
			
			// Ignore color (use 0xFF) if > 0xFF
			if (red > 0xFF) {red = 0xFF;}
			if (green > 0xFF) {green = 0xFF;}
			if (blue > 0xFF) {blue = 0xFF;}
			
		}
		color = (red << 16) + (green << 8) + blue;
		
		
		return color;
	}
	
	private int getColor(int colorStart, int colorEnd, int numberOfsteps, int steps) {
		int colorStep = (colorStart - colorEnd) / numberOfsteps;
		return colorStart - (colorStep * steps);

	}
	
	public String createGraph(GraphvizRenderer renderer, JavaFilter filter, List<String> edgeList) {

		Logger.log(1, "Class: " + this.name);

		HashSet<String> refsToSkip = new HashSet<String>();

		StringBuffer sb = new StringBuffer();
		
		String color;
		int red, green, blue;
		red = green = blue = 0xFF;

		if (searchRank > 0) {
			// yellow
			red = getColor(0xFF, 0xFF, 10, searchRank);
			green = getColor(0xFF, 0xE1, 10, searchRank);
			blue = getColor(0xFF, 0x3B, 10, searchRank);
		} else if (filter.getDownstreamDependencyDepth() != null && this.downstreamReferenceDepth > 0 && filter.getUpstreamReferenceDepth() != null && this.upstreamReferenceDepth > 0) {
			// red
			red = getColor(0xFF, 0xEF, 6, Math.min(upstreamReferenceDepth, downstreamReferenceDepth));
			green = getColor(0xFF, 0x53, 6, Math.min(upstreamReferenceDepth, downstreamReferenceDepth));
			blue = getColor(0xFF, 0x50, 6, Math.min(upstreamReferenceDepth, downstreamReferenceDepth));

		} else if (filter.getDownstreamDependencyDepth() != null && this.downstreamReferenceDepth > 0) {
			// teal
			red = getColor(0xFF, 0x00, 6, downstreamReferenceDepth);
			green = getColor(0xFF, 0xAC, 6, downstreamReferenceDepth);
			blue = getColor(0xFF, 0xC1, 6, downstreamReferenceDepth);
		} else if (filter.getUpstreamReferenceDepth() != null && this.upstreamReferenceDepth > 0) {
			// blue
			red = getColor(0xFF, 0x03, 6, upstreamReferenceDepth);
			green = getColor(0xFF, 0x9B, 6, upstreamReferenceDepth);
			blue = getColor(0xFF, 0xE5, 6, upstreamReferenceDepth);
		}

		color = "#" + String.format("%06X", mixColorToRGBValue(red, green, blue));
		
		
		if ((filter.getDiagramType() == DiagramType.UNREFERENCED_CLASSES && this.referencedByClass
				.size() == 0)
				|| filter.getDiagramType() != DiagramType.UNREFERENCED_CLASSES) {
			if (isAnonymous) {
				sb.append(renderer.getBeginRecord(this.getCanonicalName(), "<anonymous>\r\n" + this.getName(), "", color));
			}else if (isInterface) {
					sb.append(renderer.getBeginRecord(this.getCanonicalName(), "<interface>\r\n" + this.getName(), "", color));
			} else {
				sb.append(renderer.getBeginRecord(this.getCanonicalName(), this.getName(), "", color));
			}
			
			if (filter.isShowFields()) {
				for (String field : this.varNameClassMap.keySet()) {
					Class clazz = this.varNameClassMap.get(field);
					sb.append(renderer.addRecordField(field, field + ": " + clazz.getName()));
				}
			}

			if (filter.isShowMethods()) {
				for (Method method : methods.values()) {
					sb.append(renderer.addRecordField(method.getName(), method.name));
				}
			}

			sb.append(renderer.getEndRecord());

			// Add edge for extending another class, if only 1 reference to that
			// class don't add a reference edge later
			if (extnds != null) {
				boolean exclude = filter.getPackagesToExclude().contains(extnds.pkg.name);
				exclude = exclude || filter.getClassesToExclude().contains(extnds.getCanonicalName());
				if (filter.isFromFile())
					exclude = exclude || (filter.isFromFile() && !extnds.fromFile);

				if (!exclude) {
					edgeList.add((String) renderer.addReversedEdge(this.getCanonicalName(),	extnds.getCanonicalName(), "", true));

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
				exclude = exclude || filter.getClassesToExclude().contains(intr.getCanonicalName());
				if (filter.isFromFile())
					exclude = exclude || (filter.isFromFile() && !intr.fromFile);

				if (!exclude) {
					edgeList.add((String) renderer.addReversedEdge(this.getCanonicalName(), intr.getCanonicalName(), "", true, false));

					Integer count = this.unresolvedClassCount.get(intr.name);
					if (count != null && count.intValue() == 1) {
						refsToSkip.add(intr.name);
					}
				}
			}

			for (Class clazz : this.classDependencies.values()) {
				if (!refsToSkip.contains(clazz.name)) {
					boolean exclude = filter.getPackagesToExclude().contains(clazz.pkg.name);
					exclude = exclude || filter.getClassesToExclude().contains(clazz.getCanonicalName());
					if (!exclude) {
						if ((filter.isFromFile() && clazz.fromFile)	|| !filter.isFromFile()) {
							if ((filter.getDiagramType() == DiagramType.UNREFERENCED_CLASSES && clazz.referencedByClass.size() == 0)
									|| filter.getDiagramType() != DiagramType.UNREFERENCED_CLASSES) {
								edgeList.add((String) renderer.addEdge(this.getCanonicalName(), clazz.getCanonicalName(), ""));
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
		referencedByPackage.add(referencingClass.pkg);
	}

	@Override
	public int hashCode() {
		return getCanonicalName().hashCode();
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
		return this.pkg;
	}

}