package net.networkdowntime.javaAnalyzer.javaModel;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Package {
	private static final Logger LOGGER = LogManager.getLogger("javaModel");

	protected String name;
	protected boolean inPath = false;
	private Project prj;
	private Map<String, Class> classes = new HashMap<String, Class>();
	private boolean fromFile = false;

	Integer searchRank = new Integer(0);

	public Package(int depth, String name, boolean inPath, boolean fileScanned) {
		this.name = name;
		this.fromFile = fileScanned;
		logIndented(depth, "Creating Package: " + name);
	}

	public void accept(ModelVisitor visitor) {
		visitor.visit(this);
	}
	
	public void setProject(Project prj) {
		this.prj = prj;
	}

	public Package getOrCreateAndGetPackage(int depth, String name, boolean inPath, boolean fileScanned) {
		return prj.getOrCreateAndGetPackage(depth, name, inPath, fileScanned);
	}

	public Class getOrCreateAndGetClass(int depth, String name) {
		Class clazz = classes.get(name);
		if (clazz == null) {
			clazz = new Class(depth + 1, this, name, false, false, false, false);
			classes.put(name, clazz);
		}
		return clazz;
	}

	public Class getOrCreateAndGetClass(int depth, String name, boolean fileScanned, String fileName) {
		Class clazz = getOrCreateAndGetClass(depth, name);
		clazz.fromFile = fileScanned;
		clazz.fileName = fileName;
		return clazz;
	}

	public Class searchForUnresolvedClass(int depth, String classInitiatingSearch, String classToSearchFor, boolean searchProject) {
		Class clazz = null;

		if (classToSearchFor.contains(".")) {
			String classPackage = classToSearchFor.substring(0, classToSearchFor.lastIndexOf("."));
			if (!name.equals(classPackage)) {
				clazz = prj.searchForClass(depth, name, classToSearchFor);
			}
		} else {
			clazz = classes.get(name + "." + classInitiatingSearch + "." + classToSearchFor);
			if (clazz == null) {
				clazz = classes.get(classToSearchFor);
			}
			if (searchProject && clazz == null && classInitiatingSearch != null) {
				clazz = prj.searchForClass(depth, name, classToSearchFor);
			}
		}
		return clazz;
	}

	public Map<String, Class> getClasses() {
		return classes;
	}

	public void removeClass(Class clazz) {
		if (!classes.containsKey(clazz.name)) {
			classes.remove(clazz.name);
		}
	}

	public String getName() {
		return name;
	}

	public void validatePassOne(int depth) {
		logIndented(depth, "Validate Pass One: package " + name);
		for (Class clazz : classes.values()) {
			clazz.validatePassOne(depth + 1);
		}
	}

	public void validatePassTwo(int depth) {
		logIndented(depth, "Validate Pass Two: package " + name);
		for (Class clazz : classes.values()) {
			clazz.validatePassTwo(depth + 1);
		}
	}

	public boolean isFromFile() {
		return fromFile;
	}

	public void setFromFile(boolean scannedFile) {
		this.fromFile = scannedFile;
	}

	public int getSearchRank() {
		return this.searchRank;
	}

	public void setSearchRank(int searchRank) {
		this.searchRank = searchRank;
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