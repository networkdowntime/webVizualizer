import java.util.HashMap;
import java.util.Map;



public class Project {

	Map<String, Package> packages = new HashMap<String, Package>();
	
	public Project() {
		Package pkg = getOrCreateAndGetPackage("java.lang");
//		System.out.println("Creating Project");
	}
	
	public void addPackage(Package pkg) {
		if (!packages.containsKey(pkg.getName())) {
			packages.put(pkg.getName(), pkg);
		}
	}
	
	public Package getPackage(String name) {
		return packages.get(name);
	}
	
	public Package getOrCreateAndGetPackage(String name) {
		Package pkg = packages.get(name);
		if (pkg == null) {
			pkg = new Package(name);
			pkg.setProject(this);
			packages.put(name, pkg);
		}
		return pkg;
	}

	public void validate() {
		for (Package pkg : packages.values()) {
			pkg.validate();
		}
	}

	public Class searchForClass(String pkgDoingSearch, String name) {
//		System.out.println("Project: Searching for unresolved class: " + name);
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
			Package pkg = getOrCreateAndGetPackage("java.lang");
			clazz = pkg.getOrCreateAndGetClass(name);
		}
		return clazz;
	}
	
}
