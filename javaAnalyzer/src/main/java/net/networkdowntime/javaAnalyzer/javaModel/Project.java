package net.networkdowntime.javaAnalyzer.javaModel;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.networkdowntime.javaAnalyzer.AstVisitor;
import net.networkdowntime.javaAnalyzer.Search;
import net.networkdowntime.javaAnalyzer.logger.Logger;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.TokenMgrError;
import com.github.javaparser.ast.CompilationUnit;

import net.networkdowntime.javaAnalyzer.viewFilter.DiagramType;
import net.networkdowntime.javaAnalyzer.viewFilter.JavaFilter;
import net.networkdowntime.renderer.GraphvizDotRenderer;
import net.networkdowntime.renderer.GraphvizRenderer;

public class Project {

	private Set<File> files = new HashSet<File>();
	private HashSet<String> scannedFiles = new HashSet<String>();
	private Map<String, Package> packages = new HashMap<String, Package>();
	private Search search = new Search();

	public Project() {
		Logger.log(0, "Creating a new Project");
		getOrCreateAndGetPackage(1, "java.lang", false, false);
	}

	public void accept(ModelVisitor visitor) {
		visitor.visit(this);
	}
	
	public List<File> getFiles() {
		List<File> retval = new ArrayList<File>();
		retval.addAll(this.files);

		Collections.sort(retval, (File f1, File f2) -> f1.compareTo(f2));

		return retval;
	}

	public void addSearchIndex(String packageName, String className, String text) {
		search.addDocument(packageName, className, text);
	}

	/**
	 * Adds a file or directory containing files to the list of files to be scanned.
	 * 
	 * @param file
	 */
	public void addFile(File file) {
		if (file.exists()) {
			String type = ((file.isDirectory()) ? "directory" : "file");
			Logger.log(1, "Attempting to add " + type + ": " + file.getAbsolutePath() + "; " + type + " exists: " + file.exists());
			files.add(file);
			scanFile(file);
		} else {
			Logger.log(0, file.getAbsolutePath() + " does not exist");
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
			Logger.log(0, file.getAbsolutePath() + " does not exist");
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
					Logger.log(0, "");
					Logger.log(1, "Attempting to parse java file: " + f.getAbsolutePath());
					CompilationUnit cu = JavaParser.parse(f);

					if (cu.getTypes() == null) {
						Logger.log(1, f.getAbsolutePath() + " has no classes");
					} else {
						scannedFiles.add(f.getAbsolutePath());
						AstVisitor.processTypeDeclarations(0, f.getAbsolutePath(), this, cu);
					}
				}
			} catch (ParseException e) {
				System.err.println("Unrecoverable ParseException when attempting to parse: " + f.getAbsolutePath());
			} catch (RuntimeException ex) {
				System.err.println("Unrecoverable RuntimeException when attempting to parse: " + f.getAbsolutePath());
			} catch (Exception e) {
				System.err.println("Unrecoverable Exception when attempting to parse: " + f.getAbsolutePath());
			} catch (TokenMgrError e) {
				System.err.println("Unrecoverable TokenMgrError when attempting to parse: " + f.getAbsolutePath());
			} catch (StackOverflowError e) {
				System.err.println("Unrecoverable StackOverflowError when attempting to parse: " + f.getAbsolutePath());
			}
		}
		search.finalize();
		Logger.log(0, "\n");

	}

	private void deindexFile(int depth, File fileToDeindex) {
		// To Do Implement better deindexing
		// Simple implementation is to just redo everything

		scannedFiles = new HashSet<String>();
		packages = new HashMap<String, Package>();
		getOrCreateAndGetPackage(depth, "java.lang", false, false);

		for (File file : files) {
			scanFile(file);
		}
	}

	private static List<File> getFiles(File baseDir) {
		List<File> fileList = new ArrayList<File>();

		if (!baseDir.getAbsolutePath().contains(".svn")) {
			//			 Logger.log(1, baseDir.getAbsolutePath() + ": exists " + baseDir.exists());
			if (baseDir.isDirectory()) {
				String[] files = baseDir.list();
				String path = baseDir.getPath();

				for (String s : files) {
					File file = new File(path + File.separator + s);
					//				Logger.log(2, file.getAbsolutePath() + ": exists " + file.exists());
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

				for (Class c : p.getClasses().values())
					retval.add(c.getCanonicalName());
		}

		Collections.sort(retval, (String s1, String s2) -> s1.compareTo(s2));

		return retval;
	}

	public Package getOrCreateAndGetPackage(int depth, String name, boolean inPath, boolean fileScanned) {
		Package pkg = packages.get(name);
		if (pkg == null) {
			pkg = new Package(depth, name, inPath, fileScanned);
			pkg.setProject(this);
			packages.put(name, pkg);
		}

		if (inPath) {
			pkg.inPath = inPath;
		}

		if (fileScanned && !pkg.isFromFile()) {
			pkg.setFromFile(fileScanned);
		}

		return pkg;
	}

	public void validate() {
//		scannedFiles = new HashSet<String>();
//		packages = new HashMap<String, Package>();
//		getOrCreateAndGetPackage(0, "java.lang", false, false);
//
//		for (File file : files) {
//			scanFile(file);
//		}

		int classCount = 0;
		Logger.log(1, "Beginning Validation:");
		for (Package pkg : packages.values()) {
			pkg.validatePassOne(2);
			classCount += pkg.getClasses().size();
		}

		for (Package pkg : packages.values()) {
			pkg.validatePassTwo(2);
		}
		Logger.log(1, "Validation Completed");

		Logger.log(0, "Validated " + packages.size() + " packages");
		Logger.log(0, "Validated " + classCount + " classes");
	}

	public Class searchForClass(int depth, String pkgDoingSearch, String name) {
		Logger.log(depth, "Project: Searching for unresolved class: " + name);
		Class clazz = null;

		if (name.contains(".")) {
			String pkgName = name.substring(0, name.lastIndexOf("."));
			Package pkg = packages.get(pkgName);
			if (pkg != null) {
				clazz = pkg.searchForUnresolvedClass(depth, null, name.substring(name.lastIndexOf(".") + 1), false);
				if (clazz == null) {
					clazz = pkg.getOrCreateAndGetClass(depth, name.substring(name.lastIndexOf(".") + 1));
				}
			}
		} else {
			for (String pkgName : packages.keySet()) {
				if (!pkgDoingSearch.equals(pkgName)) {
					Package pkg = packages.get(pkgName);
					clazz = pkg.searchForUnresolvedClass(depth, null, name, false);
					if (clazz != null)
						break;
				}
			}
		}
		if (clazz == null) {
			Package pkg = getOrCreateAndGetPackage(depth, "java.lang", false, false);
			clazz = pkg.getOrCreateAndGetClass(depth, name);
		}
		return clazz;
	}

	public String searchForFileOfClass(String className) {
		Logger.log(0, "Project: Searching for file for class: " + className);
		String fileName = null;

		for (Package p : packages.values()) {
			for (Class c : p.getClasses().values()) {
				if (c.fromFile && c.getCanonicalName().equals(className)) {
					fileName = c.fileName;
				}
			}
		}
		return fileName;
	}

	// public static final String[] excludePkgs = { "java.", "javax." };

	private void unexcludeDependentClasses(HashSet<String> originalExcludedClasses, HashMap<String, Integer> unExcludedClasses, Class cls, Integer depth, Integer maxDepth) {
		if (depth == null || maxDepth == null || depth > maxDepth) {
			return;
		}

		for (String dependentClassName : cls.classDependencies.keySet()) {
			Class dependentClass = cls.classDependencies.get(dependentClassName);

			if (dependentClass == cls) {
				continue;
			}

			if (originalExcludedClasses.contains(dependentClass.getCanonicalName())) {
				Integer prevDepth = maxDepth + 1;
				if (unExcludedClasses.containsKey(dependentClass.getCanonicalName())) {
					prevDepth = dependentClass.getDownstreamReferenceDepth();
					if (prevDepth == 0) {
						prevDepth = maxDepth + 1;
					}
				} else {
					// Clear reference depths.
					dependentClass.setDownstreamReferenceDepth(0);
					dependentClass.setUpstreamReferenceDepth(0);
				}

				if (prevDepth > depth) {
					unExcludedClasses.put(dependentClass.getCanonicalName(), depth);
					dependentClass.setDownstreamReferenceDepth(depth);
					unexcludeDependentClasses(originalExcludedClasses, unExcludedClasses, dependentClass, depth + 1, maxDepth);
				}
			}
		}
	}

	private void unexcludeReferencedByClasses(HashSet<String> originalExcludedClasses, HashMap<String, Integer> unExcludedClasses, Class cls, Integer depth, Integer maxDepth) {
		if (depth == null || maxDepth == null || depth > maxDepth) {
			return;
		}

		for (Class referencedByClass : cls.referencedByClass) {
			if (referencedByClass == cls) {
				continue;
			}

			if (originalExcludedClasses.contains(referencedByClass.getCanonicalName())) {
				Integer prevDepth = maxDepth + 1;
				if (unExcludedClasses.containsKey(referencedByClass.getCanonicalName())) {
					prevDepth = referencedByClass.getUpstreamReferenceDepth();
					if (prevDepth == 0) {
						prevDepth = maxDepth + 1;
					}
				} else {
					// Clear reference depths.
					referencedByClass.setDownstreamReferenceDepth(0);
					referencedByClass.setUpstreamReferenceDepth(0);
				}

				if (prevDepth > depth) {
					unExcludedClasses.put(referencedByClass.getCanonicalName(), depth);
					referencedByClass.setUpstreamReferenceDepth(depth);
					unexcludeReferencedByClasses(originalExcludedClasses, unExcludedClasses, referencedByClass, depth + 1, maxDepth);
				}
			}
		}
	}

	private void unExcludeClassesBasedOnDepth(Integer maxDownDepth, Integer maxUpDepth, JavaFilter filter) {
		HashSet<String> excludedClasses = filter.getClassesToExclude();
		HashMap<String, Integer> unExcludedClasses = new HashMap<String, Integer>();
		for (String pkgName : packages.keySet()) {
			Package pkg = packages.get(pkgName);

			if (!filter.getPackagesToExclude().contains(pkg.name)) {
				for (Class cls : pkg.getClasses().values()) {
					if (!excludedClasses.contains(cls.getCanonicalName())) {
						cls.setDownstreamReferenceDepth(0);
						cls.setUpstreamReferenceDepth(0);
						unexcludeDependentClasses(excludedClasses, unExcludedClasses, cls, 1, maxDownDepth);
						unexcludeReferencedByClasses(excludedClasses, unExcludedClasses, cls, 1, maxUpDepth);
					}
				}
			}
		}

		for (String name : unExcludedClasses.keySet()) {
			if (excludedClasses.contains(name)) {
				excludedClasses.remove(name);
			}
		}
		filter.setClassesToExclude(excludedClasses);
	}

	private void unexcludeDependentPackages(HashSet<String> originalExcludedPackages, HashMap<String, Integer> unExcludedPackages, Package pkg, Integer depth, Integer maxDepth) {
		if (depth == null || maxDepth == null || depth > maxDepth) {
			return;
		}

		for (Class cls : pkg.getClasses().values()) {
			for (Package dependentPackage : cls.packageDependencies) {
				if (dependentPackage == pkg) {
					continue;
				}
				String dependentPackageName = dependentPackage.name;
				if (originalExcludedPackages.contains(dependentPackageName)) {
					//System.out.println("==============================");
					//System.out.println("Was Excluded (DP): " + dependentPackageName);
					Integer prevDepth = maxDepth + 1;
					if (unExcludedPackages.containsKey(dependentPackageName)) {
						prevDepth = dependentPackage.getDownstreamReferenceDepth();
						if (prevDepth == 0) {
							prevDepth = maxDepth + 1;
						}

						//System.out.println("Previously unexcluded, prevDepth set to: " + prevDepth);
					} else {
						//System.out.println("Not yet unexculded, clearing depths");
						// Clear reference depths.
						dependentPackage.setDownstreamReferenceDepth(0);
						dependentPackage.setUpstreamReferenceDepth(0);
					}

					if (prevDepth > depth) {
						//System.out.println("Processing children.");
						unExcludedPackages.put(dependentPackageName, depth);
						dependentPackage.setDownstreamReferenceDepth(depth);
						unexcludeDependentPackages(originalExcludedPackages, unExcludedPackages, dependentPackage, depth + 1, maxDepth);
					}
				}
			}
		}
	}

	private void unexcludeReferencedByPackages(HashSet<String> originalExcludedPackages, HashMap<String, Integer> unExcludedPackages, Package pkg, Integer depth, Integer maxDepth) {
		if (depth == null || maxDepth == null || depth > maxDepth) {
			return;
		}

		for (Class referencedByClass : pkg.getClasses().values()) {
			for (Package referencedByPackage : referencedByClass.referencedByPackage) {
				if (referencedByPackage == pkg) {
					continue;
				}
				String referencedByPackageName = referencedByPackage.name;
				if (originalExcludedPackages.contains(referencedByPackageName)) {
					//System.out.println("==============================");
					//System.out.println("Was Excluded (RB): " + referencedByPackageName);

					Integer prevDepth = maxDepth + 1;
					if (unExcludedPackages.containsKey(referencedByPackageName)) {
						prevDepth = referencedByPackage.getUpstreamReferenceDepth();
						if (prevDepth == 0) {
							prevDepth = maxDepth + 1;
						}
						//System.out.println("Previously unexcluded, prevDepth set to: " + prevDepth);
					} else {
						//System.out.println("Not yet unexculded, clearing depths");
						// Clear reference depths.
						referencedByPackage.setDownstreamReferenceDepth(0);
						referencedByPackage.setUpstreamReferenceDepth(0);
					}

					if (prevDepth > depth) {
						//System.out.println("Processing children.");
						unExcludedPackages.put(referencedByPackageName, depth);
						referencedByPackage.setUpstreamReferenceDepth(depth);
						unexcludeReferencedByPackages(originalExcludedPackages, unExcludedPackages, referencedByPackage, depth + 1, maxDepth);
					}
				}
			}
		}
	}

	public void unExcludePackagesBasedOnDepth(Integer maxDownDepth, Integer maxUpDepth, JavaFilter filter) {
		HashSet<String> excludedPackages = filter.getPackagesToExclude();
		HashMap<String, Integer> unExcludedPackages = new HashMap<String, Integer>();
		for (String pkgName : packages.keySet()) {
			Package pkg = packages.get(pkgName);

			if (!filter.getPackagesToExclude().contains(pkg.name)) {
				//System.out.println("+++++++++++++++++++++++++");
				//System.out.println("Processing children of included: " + pkg.name);
				pkg.setDownstreamReferenceDepth(0);
				pkg.setUpstreamReferenceDepth(0);
				unexcludeDependentPackages(excludedPackages, unExcludedPackages, pkg, 1, maxDownDepth);
				unexcludeReferencedByPackages(excludedPackages, unExcludedPackages, pkg, 1, maxUpDepth);
			}
		}

		for (String name : unExcludedPackages.keySet()) {
			if (excludedPackages.contains(name)) {
				excludedPackages.remove(name);
			}
		}
		filter.setPackagesToExclude(excludedPackages);
	}

	public String createGraph(JavaFilter filter) {
		for (Package pkg : this.packages.values()) {
			pkg.searchRank = 0;
			pkg.downstreamReferenceDepth = 0;
			pkg.upstreamReferenceDepth = 0;

			for (Class c : pkg.getClasses().values()) {
				c.searchRank = 0;
				c.downstreamReferenceDepth = 0;
				c.upstreamReferenceDepth = 0;
			}
		}
		if (filter.getAdvancedSearchQuery() != null) {
			String query = filter.getAdvancedSearchQuery();
			if (!query.isEmpty()) {
				if (filter.getDiagramType() == DiagramType.PACKAGE_DIAGRAM) {
					filter.setPackagesToExclude(new HashSet<String>(getPackageNames()));
					filter.setClassesToExclude(new HashSet<String>(new ArrayList<String>()));
				} else {
					filter.setPackagesToExclude(new HashSet<String>());
					filter.setClassesToExclude(new HashSet<String>(getClassNames(new ArrayList<String>())));
				}
				int rank = 0;
				LinkedHashMap<String, Float> results = new LinkedHashMap<String, Float>(search.query(query, 10, filter.getDiagramType() != DiagramType.PACKAGE_DIAGRAM));
				results = results.entrySet().stream()
						.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
						.collect(Collectors.toMap(
								Map.Entry::getKey,
								Map.Entry::getValue,
								(x, y) -> {
									throw new AssertionError();
								},
								LinkedHashMap::new));

				for (String name : results.keySet()) {
					if (filter.getDiagramType() == DiagramType.PACKAGE_DIAGRAM) {
						filter.getPackagesToExclude().remove(name);
					} else {
						filter.getClassesToExclude().remove(name);
						for (Package pkg : this.packages.values()) {
							for (Class c : pkg.getClasses().values()) {
								if (c.getCanonicalName().equals(name)) {
									c.searchRank = rank;
								}
							}
						}
					}
					rank++;
				}
			}
		}

		// Replace with test of filter for depth selection
		Integer downDepth = filter.getDownstreamDependencyDepth();
		Integer upDepth = filter.getUpstreamReferenceDepth();
		if ((downDepth != null) || (upDepth != null)) {
			if (filter.getDiagramType() == DiagramType.CLASS_ASSOCIATION_DIAGRAM) {
				unExcludeClassesBasedOnDepth(downDepth, upDepth, filter);
			} else if (filter.getDiagramType() == DiagramType.PACKAGE_DIAGRAM) {
				unExcludePackagesBasedOnDepth(downDepth, upDepth, filter);
			}
		}

		GraphvizRenderer renderer = new GraphvizDotRenderer();

		StringBuffer sb = new StringBuffer();

		List<String> edgeList = new ArrayList<String>();

		sb.append(renderer.getHeader());

		for (String pkgName : packages.keySet()) {
			Package pkg = packages.get(pkgName);
			// if (pkg.inPath) {
			boolean exclude = filter.getPackagesToExclude().contains(pkg.name);

			if (!exclude) {
				if ((filter.isFromFile() && pkg.isFromFile()) || !filter.isFromFile()) {
					sb.append(pkg.createGraph(renderer, filter, edgeList));
				}
			}
		}

		for (String edge : edgeList) {
			sb.append(edge);
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

	public void resetReferenceDepths() {
		for (Package pkg : this.packages.values()) {
			pkg.downstreamReferenceDepth = 0;
			pkg.upstreamReferenceDepth = 0;

			for (Class c : pkg.getClasses().values()) {
				c.downstreamReferenceDepth = 0;
				c.upstreamReferenceDepth = 0;
			}
		}
	}

	public void resetSearchRank() {
		for (Package pkg : this.packages.values()) {
			pkg.searchRank = 0;

			for (Class c : pkg.getClasses().values()) {
				c.searchRank = 0;
			}
		}
	}

	public Map<String, Float> searchQuery(String queryString, int topNResults, boolean classesNotPackages) {
		return search.query(queryString, topNResults, classesNotPackages);
	}

}