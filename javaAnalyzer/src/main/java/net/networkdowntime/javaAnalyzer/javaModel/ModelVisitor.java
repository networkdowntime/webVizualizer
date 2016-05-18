package net.networkdowntime.javaAnalyzer.javaModel;

/**
 * Abstract visitor class for walking the scanned Java Model.
 * 
 * There are before(), visit() and after() methods for each element of the model.
 * This allows the implementation to choose which hooks they would like to receive callbacks on.
 * The visitor is started by calling startVisiting(Project).
 * If you override the visit methods it is up to the implementer to call the super method to continue
 * processing.
 * 
 * @author rwiles
 *
 */
public abstract class ModelVisitor {

	public void before(Project project) {
	}

	public void after(Project project) {
	}

	public void before(Package pkg) {
	}

	public void after(Package pkg) {
	}

	public void before(Class clazz) {
	}

	public void after(Class clazz) {
	}

	public void before(Method method) {
	}

	public void after(Method method) {
	}

	public void before(Block block) {
	}

	public void after(Block block) {
	}

	public void visit(Project project) {
		for (String packageName : project.getPackageNames()) {
			Package pkg = project.getPackage(packageName);
	
			before(pkg);
			visit(pkg);
			after(pkg);
		}
	}

	public void visit(Package pkg) {
		for (Class clazz : pkg.getClasses().values()) {
			before(clazz);
			visit(clazz);
			after(clazz);
		}
	}

	public void visit(Class clazz) {
		for (Method method : clazz.getMethods().values()) {
			before(method);
			visit(method);
			after(method);
		}
	}

	public void visit(Method method) {
		for (Block block : method.childBlocks) {
			before(block);
			visit(block);
			after(block);
		}
	}

	public void visit(Block block) {
	}

	public final void startVisiting(Project project) {
		before(project);
		visit(project);
		after(project);
	}
}
