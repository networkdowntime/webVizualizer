package net.networkdowntime.javaAnalyzer.javaModel;

public abstract class ModelVisitor {

	public abstract void visit(Block block);

	public abstract void visit(Class clazz);

	public abstract void visit(Method method);

	public abstract void visit(Package pkg);

	public void visit(Project visitor) {
		
		for (String packageName : visitor.getPackageNames()) {
			Package pkg = visitor.getPackage(packageName);
			
			visit(pkg);
			
			for (Class clazz : pkg.getClasses().values()) {
				visit(clazz);
				
				for (Method method : clazz.methods.values()) {
					visit(method);
					
					for (Block block : method.childBlocks) {
						visit(block);
					}
				}
			}
		}
	}
	
	
}
