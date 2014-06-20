import java.util.HashMap;
import java.util.Map;

import net.networkdowntime.renderer.GraphvizRenderer;


public class Package {

	String name;
	boolean inPath = false;
	Project prj;
	Map<String, Class> classes = new HashMap<String, Class>();

	public Package(String name, boolean inPath) {
		this.name = name;
		Viewer.log(1, "Creating Package: " + name);
	}

	public void setProject(Project prj) {
		this.prj = prj;
	}

	public Class getOrCreateAndGetClass(String name) {
		Class clazz = classes.get(name);
		if (clazz == null) {
			clazz = new Class(this, name, false, false);
			classes.put(name, clazz);
		}
		return clazz;
	}

	public Class searchForUnresolvedClass(String classInitiatingSearch, String classToSearchFor) {
		// System.out.println("Package " + name + ": Searching for unresolved class: " + classToSearchFor + " by " +
		// classInitiatingSearch);

		// for (String s : classes.keySet()) {
		// System.out.println("\t\tContains: " + s);
		// }

		// System.out.println("\tChecking for: " + name + "." + classInitiatingSearch + "." + classToSearchFor);
		Class clazz = classes.get(name + "." + classInitiatingSearch + "." + classToSearchFor);
		if (clazz == null) {
			// System.out.println("\tChecking for: " + name + "." + classToSearchFor);

			clazz = classes.get(classToSearchFor);
		}
		if (clazz == null && classInitiatingSearch != null) {
			clazz = prj.searchForClass(name, classToSearchFor);
		}

		// if (clazz == null) {
		// System.out.println("Package " + name + ": No match found for " + classToSearchFor);
		// }
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

	public void validate() {
		for (Class clazz : classes.values()) {
			clazz.validate();
		}
	}

	public String createGraph(GraphvizRenderer renderer) {
		System.out.println("Package: " + this.name);
		
		StringBuffer sb = new StringBuffer();
		sb.append(renderer.getBeginCluster(name));

		for (Class clazz : classes.values()) {

			if (clazz.name == null) {
				System.err.println("!!!" + this.name + ": class with null name");
			} else {
				sb.append(clazz.createGraph(renderer));
			}
		}

		sb.append(renderer.getEndCluster());
		return sb.toString();
	}
}
