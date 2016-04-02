package edu.utdallas.cs6301_502.javaAnalyzer.javaModel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import edu.utdallas.cs6301_502.javaAnalyzer.AstVisitor;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;

import edu.utdallas.cs6301_502.javaAnalyzer.viewFilter.JavaFilter;
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

	public String createGraph(JavaFilter filter) {
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

		return sb.toString();
	}

}