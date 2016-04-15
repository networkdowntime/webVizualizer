package net.networkdowntime.javaAnalyzer.javaModel;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.networkdowntime.javaAnalyzer.AstVisitor;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;

import net.networkdowntime.javaAnalyzer.viewFilter.DiagramType;
import net.networkdowntime.javaAnalyzer.viewFilter.JavaFilter;
import net.networkdowntime.renderer.GraphvizDotRenderer;
import net.networkdowntime.renderer.GraphvizRenderer;

public class Project {

	private Set<File> files = new HashSet<File>();
	private HashSet<String> scannedFiles = new HashSet<String>();
	Map<String, Package> packages = new HashMap<String, Package>();

	public Project() {
		AstVisitor.log(0, "Creating a new Project");
		getOrCreateAndGetPackage(1, "java.lang", false);
	}

	public List<File> getFiles() {
		List<File> retval = new ArrayList<File>();
		retval.addAll(this.files);

		Collections.sort(retval, (File f1, File f2) -> f1.compareTo(f2));

		return retval;
	}

	/**
	 * Adds a file or directory containing files to the list of files to be scanned.
	 * 
	 * @param file
	 */
	public void addFile(File file) {
		if (file.exists()) {
			String type = ((file.isDirectory()) ? "directory" : "file");
			AstVisitor.log(1, "Attempting to add " + type + ": " + file.getAbsolutePath() + "; " + type + " exists: " + file.exists());
			files.add(file);
			scanFile(file);
		} else {
			AstVisitor.log(0, file.getAbsolutePath() + " does not exist");
		}
	}

	/**
	 * Removes the specified file or directory from the scanned files.
	 * 
	 * @param file
	 */
	public void removeFile(File file) {
		if (file.exists()) {
			files.remove(file);
			deindexFile(0, file);
		} else {
			AstVisitor.log(0, file.getAbsolutePath() + " does not exist");
		}
	}

	/**
	 * Scans the selected file or directory and adds it to the project
	 * 
	 * Preconditions: file exists
	 * 
	 * @param fileToScan
	 */
	private void scanFile(File fileToScan) {

		List<File> filesToScan = new ArrayList<File>();
		filesToScan.addAll(getFiles(fileToScan));

		for (File f : filesToScan) {
			try {
				if (f.getName().endsWith(".java")) {
					AstVisitor.log(0, "");
					AstVisitor.log(1, "Attempting to parse java file: " + f.getAbsolutePath());
					CompilationUnit cu = JavaParser.parse(f);
					if (cu.getTypes() == null) {
						AstVisitor.log(1, f.getAbsolutePath() + " has no classes");
					} else {
						scannedFiles.add(f.getAbsolutePath());
						AstVisitor.processTypeDeclarations(0, f.getName(), this, null, cu, cu.getTypes());
					}
				}
			} catch (ParseException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		AstVisitor.log(0, "\n");

	}

	private void deindexFile(int depth, File fileToDeindex) {
		// To Do Implement better deindexing
		// Simple implementation is to just redo everything

		scannedFiles = new HashSet<String>();
		packages = new HashMap<String, Package>();
		getOrCreateAndGetPackage(depth, "java.lang", false);

		for (File file : files) {
			scanFile(file);
		}
	}

	private static List<File> getFiles(File baseDir) {
		List<File> fileList = new ArrayList<File>();

		if (!baseDir.getAbsolutePath().contains(".svn")) {
			//			 AstVisitor.log(1, baseDir.getAbsolutePath() + ": exists " + baseDir.exists());
			if (baseDir.isDirectory()) {
				String[] files = baseDir.list();
				String path = baseDir.getPath();

				for (String s : files) {
					File file = new File(path + File.separator + s);
					//				AstVisitor.log(2, file.getAbsolutePath() + ": exists " + file.exists());
					if (file.isDirectory()) {
						fileList.addAll(getFiles(file));
					} else {
						fileList.add(file);
					}
				}
			} else {
				fileList.add(baseDir);
			}
		}
		return fileList;

	}

	public void addPackage(Package pkg) {
		if (!packages.containsKey(pkg.getName())) {
			packages.put(pkg.getName(), pkg);
		}
	}

	public Package getPackage(String name) {
		return packages.get(name);
	}

	public List<String> getPackageNames() {
		List<String> retval = new ArrayList<String>();

		for (Package p : packages.values()) {
			retval.add(p.name);
		}

		Collections.sort(retval, (String s1, String s2) -> s1.compareTo(s2));

		return retval;
	}

	public List<String> getClassNames(List<String> excludePackages) {
		List<String> retval = new ArrayList<String>();

		HashSet<String> excludeSet = new HashSet<String>();
		excludeSet.addAll(excludePackages);

		for (Package p : packages.values()) {

			boolean exclude = excludeSet.contains(p.name);

			if (!exclude)

				for (Class c : p.classes.values())
					retval.add(p.name + "." + c.name);
		}

		Collections.sort(retval, (String s1, String s2) -> s1.compareTo(s2));

		return retval;
	}

	public Package getOrCreateAndGetPackage(int depth, String name, boolean inPath) {
		Package pkg = packages.get(name);
		if (pkg == null) {
			pkg = new Package(depth, name, inPath);
			pkg.setProject(this);
			packages.put(name, pkg);
		}

		if (inPath) {
			pkg.inPath = inPath;
		}

		return pkg;
	}

	public Package getOrCreateAndGetPackage(int depth, String name, boolean inPath, boolean fileScanned) {
		Package pkg = getOrCreateAndGetPackage(depth, name, inPath);
		pkg.fromFile = fileScanned;
		return pkg;
	}

	public void validate() {
		int classCount = 0;
		AstVisitor.log(1, "Beginning Validation:");
		for (Package pkg : packages.values()) {
			pkg.validatePassOne(2);
			classCount += pkg.classes.size();
		}

		for (Package pkg : packages.values()) {
			pkg.validatePassTwo(2);
		}
		AstVisitor.log(1, "Validation Completed");

		AstVisitor.log(0, "Validated " + packages.size() + " packages");
		AstVisitor.log(0, "Validated " + classCount + " classes");
	}

	public Class searchForClass(int depth, String pkgDoingSearch, String name) {
		AstVisitor.log(depth, "Project: Searching for unresolved class: " + name);
		Class clazz = null;
		for (String pkgName : packages.keySet()) {
			if (!pkgDoingSearch.equals(pkgName)) {
				Package pkg = packages.get(pkgName);
				clazz = pkg.searchForUnresolvedClass(depth, null, name);
				if (clazz != null)
					break;
			}
		}

		if (clazz == null) {
			Package pkg = getOrCreateAndGetPackage(depth, "java.lang", false);
			clazz = pkg.getOrCreateAndGetClass(depth, name);
		}
		return clazz;
	}

	// public static final String[] excludePkgs = { "java.", "javax." };

	private void unexcludeDependentClasses(HashSet<String> originalExcludedClasses, HashMap<String, Integer> unExcludedClasses, Class cls, Integer depth, Integer maxDepth)
	{
		if (depth == null || maxDepth == null || depth > maxDepth)
		{
			return;
		}
		
		for (String dependentClassName : cls.classDependencies.keySet())
		{
			Class dependentClass = cls.classDependencies.get(dependentClassName);
			
			if (originalExcludedClasses.contains(dependentClass.getCanonicalName()))
			{
				Integer prevDepth = maxDepth;
				if (unExcludedClasses.containsKey(dependentClass.getCanonicalName()))
				{
					prevDepth = unExcludedClasses.get(dependentClass.getCanonicalName());
				}
				
				if (prevDepth > depth)
				{				
					unExcludedClasses.put(dependentClass.getCanonicalName(), depth);
					dependentClass.setDownstreamReferenceDepth(depth);
					unexcludeDependentClasses(originalExcludedClasses, unExcludedClasses, dependentClass, depth + 1, maxDepth);
				}
			}
		}
	}
	
	private void unexcludeReferencedByClasses(HashSet<String> originalExcludedClasses, HashMap<String, Integer> unExcludedClasses, Class cls, Integer depth, Integer maxDepth)
	{
		if (depth == null || maxDepth == null || depth > maxDepth)
		{
			return;
		}
		
		for (Class referencedByClass : cls.referencedByClass)
		{
			if (originalExcludedClasses.contains(referencedByClass.getCanonicalName()))
			{			
				Integer prevDepth = maxDepth;
				if (unExcludedClasses.containsKey(cls.getCanonicalName()))
				{
					prevDepth = unExcludedClasses.get(cls.getCanonicalName());
				}
				
				if (prevDepth > depth)
				{
					unExcludedClasses.put(referencedByClass.getCanonicalName(), depth);
					referencedByClass.setUpstreamReferenceDepth(depth);
					unexcludeReferencedByClasses(originalExcludedClasses, unExcludedClasses, referencedByClass, depth + 1, maxDepth);				
				}
			}
		}
	}
	
	private void unExcludeClassesBasedOnDepth(Integer maxDownDepth, Integer maxUpDepth, JavaFilter filter)
	{
		HashSet<String> excludedClasses = filter.getClassesToExclude();
		HashMap<String, Integer> unExcludedClasses = new HashMap<String, Integer>();
		for (String pkgName : packages.keySet()) 
		{
			Package pkg = packages.get(pkgName);

			if (!filter.getPackagesToExclude().contains(pkg.name))
			{
				for (Class cls : pkg.classes.values())
				{
					if (!excludedClasses.contains(cls.getCanonicalName()))
					{
						unexcludeDependentClasses(excludedClasses, unExcludedClasses, cls, 1, maxDownDepth);
						unexcludeReferencedByClasses(excludedClasses, unExcludedClasses, cls, 1, maxUpDepth);
					}
				}
			}
		}
		

		for (String name : unExcludedClasses.keySet())
		{
			if (excludedClasses.contains(name))
			{
				excludedClasses.remove(name);
			}
		}
		filter.setClassesToExclude(excludedClasses);
	}
	
	
	private void unexcludeDependentPackages(HashSet<String> originalExcludedPackages, HashMap<String, Integer> unExcludedPackages, Package pkg, Integer depth, Integer maxDepth)
	{
		if (depth == null || maxDepth == null || depth > maxDepth)
		{
			return;
		}
		
		for (Class cls : pkg.classes.values())
		{
			for (Package dependentPackage : cls.packageDependencies)
			{
				String dependentPackageName = dependentPackage.name;
				if (originalExcludedPackages.contains(dependentPackageName))
				{
					Integer prevDepth = maxDepth;
					if (unExcludedPackages.containsKey(dependentPackageName))
					{
						prevDepth = unExcludedPackages.get(dependentPackageName);
					}
					
					if (prevDepth > depth)
					{
						unExcludedPackages.put(dependentPackageName, depth);
						dependentPackage.setDownstreamReferenceDepth(depth);
						unexcludeDependentPackages(originalExcludedPackages, unExcludedPackages, dependentPackage, depth +1, maxDepth);
					}
				}
			}
		}
	}
	
	private void unexcludeReferencedByPackages(HashSet<String> originalExcludedPackages, HashMap<String, Integer> unExcludedPackages, Package pkg, Integer depth, Integer maxDepth)
	{
		if (depth == null || maxDepth == null || depth > maxDepth)
		{
			return;
		}
		
		for (Class referencedByClass : pkg.classes.values())
		{
			for (Package referencedByPackage : referencedByClass.referencedByPackage)
			{
				String referencedByPackageName = referencedByPackage.name;
				if (originalExcludedPackages.contains(referencedByPackageName))
				{			
					Integer prevDepth = maxDepth;
					if (unExcludedPackages.containsKey(referencedByPackageName))
					{
						prevDepth = unExcludedPackages.get(referencedByPackageName);
					}
					
					if (prevDepth > depth)
					{
						unExcludedPackages.put(referencedByPackageName, depth);
						referencedByPackage.setUpstreamReferenceDepth(depth);
						unexcludeReferencedByPackages(originalExcludedPackages, unExcludedPackages, referencedByPackage, depth +1, maxDepth);				
					}
				}
			}
		}
	}
	
	private void unExcludePackagesBasedOnDepth(Integer maxDownDepth, Integer maxUpDepth, JavaFilter filter)
	{
		HashSet<String> excludedPackages = filter.getPackagesToExclude();
		HashMap<String, Integer> unExcludedPackages = new HashMap<String, Integer>();
		for (String pkgName : packages.keySet()) 
		{
			Package pkg = packages.get(pkgName);

			if (!filter.getPackagesToExclude().contains(pkg.name))
			{
				unexcludeDependentPackages(excludedPackages, unExcludedPackages, pkg, 1, maxDownDepth);
				// Referenced By data is not tracked yet by the system for package relations
				unexcludeReferencedByPackages(excludedPackages, unExcludedPackages, pkg, 1, maxUpDepth);
			}
		}
		

		for (String name : unExcludedPackages.keySet())
		{
			if (excludedPackages.contains(name))
			{
				excludedPackages.remove(name);
			}
		}
		filter.setPackagesToExclude(excludedPackages);
	}
	
	public String createGraph(JavaFilter filter) {
		
		// Replace with test of filter for depth selection
		Integer downDepth = filter.getDownstreamDependencyDepth();
		Integer upDepth = filter.getUpstreamReferenceDepth();
		if ((downDepth != null && downDepth > 0) || 
			(upDepth   != null && upDepth   > 0))
		{			
			if (filter.getDiagramType() == DiagramType.CLASS_ASSOCIATION_DIAGRAM)
			{
				unExcludeClassesBasedOnDepth(downDepth, upDepth, filter);
			}
			else if (filter.getDiagramType() == DiagramType.PACKAGE_DIAGRAM)
			{
				unExcludePackagesBasedOnDepth(downDepth, upDepth, filter);
			}
		}	
		
		GraphvizRenderer renderer = new GraphvizDotRenderer();

		StringBuffer sb = new StringBuffer();

		sb.append(renderer.getHeader());

		
		for (String pkgName : packages.keySet()) {
			Package pkg = packages.get(pkgName);
			// if (pkg.inPath) {
			boolean exclude = filter.getPackagesToExclude().contains(pkg.name);		
			
			if (!exclude) {
				if ((filter.isFromFile() && pkg.fromFile) || !filter.isFromFile()) {
					sb.append(pkg.createGraph(renderer, filter));
				}
			}
		}

		sb.append(renderer.getFooter());

		if (AstVisitor.DEBUGGING_ENABLED) {
			try {
				FileWriter fw = new FileWriter("graphviz.gv");
				fw.write(sb.toString());
				fw.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return sb.toString();
	}

