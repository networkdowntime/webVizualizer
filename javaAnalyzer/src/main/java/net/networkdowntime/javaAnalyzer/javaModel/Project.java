package net.networkdowntime.javaAnalyzer.javaModel;

import java.io.File;
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.networkdowntime.javaAnalyzer.search.Search;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.TokenMgrError;
import com.github.javaparser.ast.CompilationUnit;

public class Project {
	private static final Logger LOGGER = LogManager.getLogger("javaModel");

	private Set<File> files = new HashSet<File>();
	private Set<String> scannedFiles = new HashSet<String>();
	private Map<String, Package> packages = new HashMap<String, Package>();
	private Search search = new Search();
	private boolean isValidated = false;

	public Project() {
		logIndented(0, "Creating a new Project");
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
			String type = file.isDirectory() ? "directory" : "file";
			logIndented(1, "Attempting to add " + type + ": " + file.getAbsolutePath() + "; " + type + " exists: " + file.exists());
			files.add(file);
			scanFile(file);
		} else {
			logIndented(0, file.getAbsolutePath() + " does not exist");
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
			logIndented(0, file.getAbsolutePath() + " does not exist");
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
					logIndented(0, "");
					logIndented(1, "Attempting to parse java file: " + f.getAbsolutePath());
					CompilationUnit cu = JavaParser.parse(f);

					if (cu.getTypes() == null) {
						logIndented(1, f.getAbsolutePath() + " has no classes");
					} else {
						scannedFiles.add(f.getAbsolutePath());
						new AstVisitor(0, f.getAbsolutePath(), this, cu);
					}
				}
			} catch (ParseException e) {
				LOGGER.error("Unrecoverable ParseException when attempting to parse: " + f.getAbsolutePath());
			} catch (RuntimeException ex) {
				LOGGER.error("Unrecoverable RuntimeException when attempting to parse: " + f.getAbsolutePath());
			} catch (Exception e) {
				LOGGER.error("Unrecoverable Exception when attempting to parse: " + f.getAbsolutePath());
			} catch (TokenMgrError e) {
				LOGGER.error("Unrecoverable TokenMgrError when attempting to parse: " + f.getAbsolutePath());
			} catch (StackOverflowError e) {
				LOGGER.error("Unrecoverable StackOverflowError when attempting to parse: " + f.getAbsolutePath());
			}
		}
		search.finishedIndexing();
		logIndented(0, "\n");

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

		if (!baseDir.isHidden()) {
			if (baseDir.isDirectory()) {
				String[] files = baseDir.list();
				String path = baseDir.getPath();

				for (String s : files) {
					File file = new File(path + File.separator + s);
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
			isValidated = false;
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

		Set<String> excludeSet = new HashSet<String>();
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
			isValidated = false;
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

	public boolean isValidated() {
		return isValidated;
	}
	
	public void validate() {
		//		scannedFiles = new HashSet<String>();
		//		packages = new HashMap<String, Package>();
		//		getOrCreateAndGetPackage(0, "java.lang", false, false);
		//
		//		for (File file : files) {
		//			scanFile(file);
		//		}

		if (!isValidated)
		{
			int classCount = 0;
			logIndented(1, "Beginning Validation:");
			for (Package pkg : packages.values()) {
				pkg.validatePassOne(2);
				classCount += pkg.getClasses().size();
			}

			for (Package pkg : packages.values()) {
				pkg.validatePassTwo(2);
			}

			isValidated = true;

			logIndented(1, "Validation Completed");

			logIndented(0, "Validated " + packages.size() + " packages");
			logIndented(0, "Validated " + classCount + " classes");
		}
		else
		{
			logIndented(1, "Validation Requested, but is already completed.");
		}
	}

	public Class searchForClass(int depth, String pkgDoingSearch, String name) {
		logIndented(depth, "Project: Searching for unresolved class: " + name);
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
		logIndented(0, "Project: Searching for file for class: " + className);
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

	public void resetSearchRank() {
		for (Package pkg : this.packages.values()) {
			pkg.searchRank = 0;

			for (Class c : pkg.getClasses().values()) {
				c.searchRank = 0;
			}
		}
	}

	/**
	 * Returns a sorted list of results from the search engine based on the parameters.
	 * 
	 * @param queryString The search query string
	 * @param topNResults How many results to return
	 * @param classesNotPackages The type of search results you want returned
	 * 
	 * @return Query results ordered and ranked from 0 to N-1
	 */
	public Map<String, Integer> searchQuery(String queryString, int topNResults, boolean classesNotPackages) {
		Map<String, Float> rawResults = new LinkedHashMap<String, Float>(search.query(queryString, topNResults, classesNotPackages));
		rawResults = rawResults.entrySet().stream()
				.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						Map.Entry::getValue,
						(x, y) -> {
							throw new AssertionError();
						},
						LinkedHashMap::new));
		Map<String, Integer> results = new LinkedHashMap<String, Integer>();
		int rank = 1;
		for (String name : rawResults.keySet()) {
			results.put(name, rank);
			rank++;
		}

		return results;
	}

	public void logIndented(int depth, String str) {
		if (LOGGER.isDebugEnabled()) {
			String retval = "";
			for (int i = 0; i < depth; i++) {
				retval += "    ";
			}
			LOGGER.debug(retval + str);
		}
	}

}