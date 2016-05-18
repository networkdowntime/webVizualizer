package net.networkdowntime.javaAnalyzer.visitors;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.networkdowntime.javaAnalyzer.javaModel.Class;
import net.networkdowntime.javaAnalyzer.javaModel.ModelVisitor;
import net.networkdowntime.javaAnalyzer.javaModel.Package;
import net.networkdowntime.javaAnalyzer.javaModel.Project;

public class DependencyDepthVisitor extends ModelVisitor {

	public static enum DepthType { PACKAGE, CLASS };
	
	private HashMap<String, Integer> upstreamReferenceDepth = new HashMap<String, Integer>();
	private HashMap<String, Integer> downstreamReferenceDepth = new HashMap<String, Integer>();
	private Set<String> classesToExclude = new HashSet<String>();
	private Set<String> packagesToExclude = new HashSet<String>();
	private DepthType depthType;
	private Integer maxUpDepth; 
	private Integer maxDownDepth;
	
	private Map<String, Integer> unExcludedClasses = new HashMap<String, Integer>();
	private Map<String, Integer> unExcludedPackages = new HashMap<String, Integer>();

	public DependencyDepthVisitor() {
	}
	
	public DependencyDepthVisitor(Project project, DepthType depthType, Integer maxUpDepth, Integer maxDownDepth, Set<String> classesToExclude, Set<String> packagesToExclude) {
		this.depthType = depthType;
		this.classesToExclude = classesToExclude;
		this.packagesToExclude = packagesToExclude;
		this.maxUpDepth = maxUpDepth;
		this.maxDownDepth = maxDownDepth;
		
		// Replace with test of filter for depth selection
		if ((maxDownDepth != null) || (maxUpDepth != null)) {
			this.visit(project);
		}

		packagesToExclude.removeAll(unExcludedPackages.keySet());
		classesToExclude.removeAll(unExcludedClasses.keySet());
	}
	
	public int getUpstreamReferenceDepth(String name) {
		return upstreamReferenceDepth.containsKey(name) ? upstreamReferenceDepth.get(name) : 0;
	}
	
	public int getDownstreamReferenceDepth(String name) {
		return downstreamReferenceDepth.containsKey(name) ? downstreamReferenceDepth.get(name) : 0;
	}
	
	public Set<String> getUpdatedClassesToExclude() {
		return this.classesToExclude;
	}

	public Set<String> getUpdatedPackagesToExclude() {
		return this.packagesToExclude;
	}

	@Override
	public void visit(Package pkg) {
		if (depthType == DepthType.PACKAGE) {
			if (!packagesToExclude.contains(pkg.getName())) {
				//System.out.println("+++++++++++++++++++++++++");
				//System.out.println("Processing children of included: " + pkg.name);
				upstreamReferenceDepth.put(pkg.getName(), 0);
				downstreamReferenceDepth.put(pkg.getName(), 0);
				unexcludeDependentPackages(packagesToExclude, unExcludedPackages, pkg, 1);
				unexcludeReferencedByPackages(packagesToExclude, unExcludedPackages, pkg, 1);
			}
		} else if (depthType == DepthType.CLASS) {
			if (!packagesToExclude.contains(pkg.getName())) {
				super.visit(pkg);
			}			
		}
	}

	@Override
	public void visit(Class clazz) {
		if (depthType == DepthType.CLASS) {
			if (!classesToExclude.contains(clazz.getCanonicalName())) {
				upstreamReferenceDepth.put(clazz.getCanonicalName(), 0);
				downstreamReferenceDepth.put(clazz.getCanonicalName(), 0);
				unexcludeDependentClasses(classesToExclude, unExcludedClasses, clazz, 1);
				unexcludeReferencedByClasses(classesToExclude, unExcludedClasses, clazz, 1);
			}
		}
		super.visit(clazz);
	}

	private void unexcludeDependentClasses(Set<String> originalExcludedClasses, Map<String, Integer> unExcludedClasses, Class clazz, Integer depth) {
		if (depth == null || this.maxDownDepth == null || depth > this.maxDownDepth) {
			return;
		}

		for (Class dependentClass : clazz.getClassDependencies().values()) {
			if (dependentClass == clazz) {
				continue;
			}

			if (originalExcludedClasses.contains(dependentClass.getCanonicalName())) {
				Integer prevDepth = this.maxDownDepth + 1;
				if (unExcludedClasses.containsKey(dependentClass.getCanonicalName())) {
					prevDepth = getDownstreamReferenceDepth(dependentClass.getCanonicalName());
					if (prevDepth == 0) {
						prevDepth = this.maxDownDepth + 1;
					}
				} else {
					// Clear reference depths.
					upstreamReferenceDepth.put(dependentClass.getCanonicalName(), 0);
					downstreamReferenceDepth.put(dependentClass.getCanonicalName(), 0);
				}

				if (prevDepth > depth) {
					unExcludedClasses.put(dependentClass.getCanonicalName(), depth);
					downstreamReferenceDepth.put(dependentClass.getCanonicalName(), 0);
					unexcludeDependentClasses(originalExcludedClasses, unExcludedClasses, dependentClass, depth + 1);
				}
			}
		}
	}

	private void unexcludeReferencedByClasses(Set<String> originalExcludedClasses, Map<String, Integer> unExcludedClasses, Class cls, Integer depth) {
		if (depth == null || this.maxUpDepth == null || depth > this.maxUpDepth) {
			return;
		}

		for (Class referencedByClass : cls.getReferencedByClass()) {
			if (referencedByClass == cls) {
				continue;
			}

			if (originalExcludedClasses.contains(referencedByClass.getCanonicalName())) {
				Integer prevDepth = this.maxUpDepth + 1;
				if (unExcludedClasses.containsKey(referencedByClass.getCanonicalName())) {
					prevDepth = getUpstreamReferenceDepth(referencedByClass.getCanonicalName());
					if (prevDepth == 0) {
						prevDepth = this.maxUpDepth + 1;
					}
				} else {
					// Clear reference depths.
					upstreamReferenceDepth.put(referencedByClass.getCanonicalName(), 0);
					downstreamReferenceDepth.put(referencedByClass.getCanonicalName(), 0);
				}

				if (prevDepth > depth) {
					unExcludedClasses.put(referencedByClass.getCanonicalName(), depth);
					upstreamReferenceDepth.put(referencedByClass.getCanonicalName(), depth);
					unexcludeReferencedByClasses(originalExcludedClasses, unExcludedClasses, referencedByClass, depth + 1);
				}
			}
		}
	}

	private void unexcludeDependentPackages(Set<String> originalExcludedPackages, Map<String, Integer> unExcludedPackages, Package pkg, Integer depth) {
		if (depth == null || this.maxDownDepth == null || depth > this.maxDownDepth) {
			return;
		}

		for (Class cls : pkg.getClasses().values()) {
			for (Package dependentPackage : cls.getPackageDependencies()) {
				if (dependentPackage == pkg) {
					continue;
				}
				String dependentPackageName = dependentPackage.getName();
				if (originalExcludedPackages.contains(dependentPackageName)) {
					//System.out.println("==============================");
					//System.out.println("Was Excluded (DP): " + dependentPackageName);
					Integer prevDepth = this.maxDownDepth + 1;
					if (unExcludedPackages.containsKey(dependentPackageName)) {
						prevDepth = getDownstreamReferenceDepth(dependentPackage.getName());
						if (prevDepth == 0) {
							prevDepth = this.maxDownDepth + 1;
						}

						//System.out.println("Previously unexcluded, prevDepth set to: " + prevDepth);
					} else {
						//System.out.println("Not yet unexculded, clearing depths");
						// Clear reference depths.
						upstreamReferenceDepth.put(dependentPackage.getName(), 0);
						downstreamReferenceDepth.put(dependentPackage.getName(), 0);
					}

					if (prevDepth > depth) {
						//System.out.println("Processing children.");
						unExcludedPackages.put(dependentPackageName, depth);
						downstreamReferenceDepth.put(dependentPackage.getName(), depth);
						unexcludeDependentPackages(originalExcludedPackages, unExcludedPackages, dependentPackage, depth + 1);
					}
				}
			}
		}
	}

	private void unexcludeReferencedByPackages(Set<String> originalExcludedPackages, Map<String, Integer> unExcludedPackages, Package pkg, Integer depth) {
		if (depth == null || this.maxUpDepth == null || depth > this.maxUpDepth) {
			return;
		}

		for (Class referencedByClass : pkg.getClasses().values()) {
			for (Package referencedByPackage : referencedByClass.getReferencedByPackage()) {
				if (referencedByPackage == pkg) {
					continue;
				}
				String referencedByPackageName = referencedByPackage.getName();
				if (originalExcludedPackages.contains(referencedByPackageName)) {
					//System.out.println("==============================");
					//System.out.println("Was Excluded (RB): " + referencedByPackageName);

					Integer prevDepth = this.maxUpDepth + 1;
					if (unExcludedPackages.containsKey(referencedByPackageName)) {
						prevDepth = getUpstreamReferenceDepth(referencedByPackage.getName());
						if (prevDepth == 0) {
							prevDepth = this.maxUpDepth + 1;
						}
						//System.out.println("Previously unexcluded, prevDepth set to: " + prevDepth);
					} else {
						//System.out.println("Not yet unexculded, clearing depths");
						// Clear reference depths.
						upstreamReferenceDepth.put(referencedByPackage.getName(), 0);
						downstreamReferenceDepth.put(referencedByPackage.getName(), 0);
					}

					if (prevDepth > depth) {
						//System.out.println("Processing children.");
						unExcludedPackages.put(referencedByPackageName, depth);
						upstreamReferenceDepth.put(referencedByPackage.getName(), depth);
						unexcludeReferencedByPackages(originalExcludedPackages, unExcludedPackages, referencedByPackage, depth + 1);
					}
				}
			}
		}
	}

}
