package edu.utdallas.cs6301_502.javaAnalyzer.javaModel;

import java.util.HashMap;
import java.util.Map;
import edu.utdallas.cs6301_502.javaAnalyzer.AstVisitor;

import edu.utdallas.cs6301_502.javaAnalyzer.viewFilter.DiagramType;
import edu.utdallas.cs6301_502.javaAnalyzer.viewFilter.JavaFilter;
import net.networkdowntime.renderer.GraphvizRenderer;


public class Package {

	protected String name;
	protected boolean inPath = false;
	Project prj;
	Map<String, Class> classes = new HashMap<String, Class>();
	boolean fromFile = false;

	public Package(int depth, String name, boolean inPath) {
		this.name = name;
		AstVisitor.log(depth, "Creating Package: " + name);
	}

	public void setProject(Project prj) {
		this.prj = prj;
	}

	public Class getOrCreateAndGetClass(int depth, String name) {
		Class clazz = classes.get(name);
		if (clazz == null) {
			clazz = new Class(depth + 1, this, name, false, false, false, false);
			classes.put(name, clazz);
		}
		return clazz;
	}

	public Class getOrCreateAndGetClass(int depth, String name, boolean fileScanned) {
		Class clazz = getOrCreateAndGetClass(depth, name);
		clazz.fromFile = fileScanned;
		return clazz;
	}

	public Class searchForUnresolvedClass(int depth, String classInitiatingSearch, String classToSearchFor) {
		Class clazz = classes.get(name + "." + classInitiatingSearch + "." + classToSearchFor);
		if (clazz == null) {
			clazz = classes.get(classToSearchFor);
		}
		if (clazz == null && classInitiatingSearch != null) {
			clazz = prj.searchForClass(depth, name, classToSearchFor);
		}

		return clazz;
	}

	public void removeClass(Class clazz) {
		if (!classes.containsKey(clazz.getName())) {
			classes.remove(clazz.getName());
		}
	}

	public String getName() {
		return name;
	}

	public void validatePassOne(int depth) {
		AstVisitor.log(depth, "Validate Pass One: package " + name);
		for (Class clazz : classes.values()) {
			clazz.validatePassOne(depth + 1);
		}
	}

	public void validatePassTwo(int depth) {
		AstVisitor.log(depth, "Validate Pass Two: package " + name);
		for (Class clazz : classes.values()) {
			clazz.validatePassTwo(depth + 1);
		}
	}

	public String createGraph(GraphvizRenderer renderer, JavaFilter filter) {
		AstVisitor.log(0, "Package: " + this.name);

		StringBuffer sb = new StringBuffer();

		if (filter.getDiagramType() == DiagramType.PACKAGE_DIAGRAM) {
			sb.append(renderer.getBeginRecord(this.name, this.name, ""));
			sb.append(renderer.getEndRecord());

			HashMap<String, Integer> referencedPackages = new HashMap<String, Integer>();
			for (Class c : classes.values()) {
				if ((filter.isFromFile() && c.fromFile) || !filter.isFromFile()) {
					for (Package p : c.packageDependencies) {
						if ((filter.isFromFile() && p.fromFile) || !filter.isFromFile()) {
							Integer count = referencedPackages.get(p.name);
							if (count == null)
								count = 0;
							referencedPackages.put(p.name, count);
						}
					}

					for (Class c1 : c.classDependencies.values()) {
						if ((filter.isFromFile() && c1.fromFile) || !filter.isFromFile()) {
							Integer count = referencedPackages.get(c1.pkg.name);
							if (count == null)
								count = 0;
							referencedPackages.put(c1.pkg.name, count + 1);
						}
					}
				}
			}

			for (String pkgName : referencedPackages.keySet()) {
				if (!filter.getPackagesToExclude().contains(pkgName)) {
					Integer count = referencedPackages.get(pkgName);
					sb.append(renderer.addEdge(this.name, pkgName, count.toString(), false));
				}
			}

		} else {

			sb.append(renderer.getBeginCluster(name));

			for (Class clazz : classes.values()) {

				if ((filter.isFromFile() && clazz.fromFile) || !filter.isFromFile()) {
					if (clazz.name == null) {
						System.err.println("!!!" + this.name + ": class with null name");
					} else {
						if (!filter.getClassesToExclude().contains(this.name + "." + clazz.name)) {
							sb.append(clazz.createGraph(renderer, filter));
						}
					}
				}
			}
			sb.append(renderer.getEndCluster());
		}

		return sb.toString();
	}
}