import java.util.HashMap;
import java.util.Map;

import net.networkdowntime.renderer.GraphvizRenderer;


public class Project {

	Map<String, Package> packages = new HashMap<String, Package>();

	public Project() {
		Package pkg = getOrCreateAndGetPackage("java.lang", false);
		// System.out.println("Creating Project");
	}

	public void addPackage(Package pkg) {
		if (!packages.containsKey(pkg.getName())) {
			packages.put(pkg.getName(), pkg);
		}
	}

	public Package getPackage(String name) {
		return packages.get(name);
	}

	public Package getOrCreateAndGetPackage(String name, boolean inPath) {
		Package pkg = packages.get(name);
		if (pkg == null) {
			pkg = new Package(name, inPath);
			pkg.setProject(this);
			packages.put(name, pkg);
		}

		if (inPath) {
			pkg.inPath = inPath;
		}

		return pkg;
	}

	public void validate() {
		for (Package pkg : packages.values()) {
			pkg.validate();
		}
	}

	public Class searchForClass(String pkgDoingSearch, String name) {
		// System.out.println("Project: Searching for unresolved class: " + name);
		Class clazz = null;
		for (String pkgName : packages.keySet()) {
			if (!pkgDoingSearch.equals(pkgName)) {
				Package pkg = packages.get(pkgName);
				clazz = pkg.searchForUnresolvedClass(null, name);
				if (clazz != null)
					break;
			}
		}

		if (clazz == null) {
			Package pkg = getOrCreateAndGetPackage("java.lang", false);
			clazz = pkg.getOrCreateAndGetClass(name);
		}
		return clazz;
	}

	public static final String[] excludePkgs = {"java.", "javax."};
	
	public String createGraph(GraphvizRenderer renderer) {
		StringBuffer sb = new StringBuffer();

		sb.append(renderer.getHeader());

		for (String pkgName : packages.keySet()) {
			Package pkg = packages.get(pkgName);
//			if (pkg.inPath) {
			boolean exclude = false;
			for (String excludePkg : Project.excludePkgs) {
				if (pkg.name.startsWith(excludePkg)) {
					System.out.println("Excluding " + pkg.name);
					exclude = true;
				}
			}
			if (!exclude)
				sb.append(pkg.createGraph(renderer));
		}

		sb.append(renderer.getFooter());

		return sb.toString();
	}

}
