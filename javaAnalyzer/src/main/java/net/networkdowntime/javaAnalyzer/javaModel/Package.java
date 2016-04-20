package net.networkdowntime.javaAnalyzer.javaModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.networkdowntime.javaAnalyzer.logger.Logger;
import net.networkdowntime.javaAnalyzer.viewFilter.DiagramType;
import net.networkdowntime.javaAnalyzer.viewFilter.JavaFilter;
import net.networkdowntime.renderer.GraphvizRenderer;


public class Package {

	protected String name;
	protected boolean inPath = false;
	private Project prj;
	private Map<String, Class> classes = new HashMap<String, Class>();
	private boolean fromFile = false;

	Integer searchRank = new Integer(0);
	Integer upstreamReferenceDepth = new Integer(0);
	Integer downstreamReferenceDepth = new Integer(0);
		
	public Package(int depth, String name, boolean inPath, boolean fileScanned) {
		this.name = name;
		this.fromFile = fileScanned;
		Logger.log(depth, "Creating Package: " + name);
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

	public Class getOrCreateAndGetClass(int depth, String name, boolean fileScanned) {
		Class clazz = getOrCreateAndGetClass(depth, name);
		clazz.fromFile = fileScanned;
		return clazz;
	}

	public Class searchForUnresolvedClass(int depth, String classInitiatingSearch, String classToSearchFor) {
		Class clazz = classes.get(name + "." + classInitiatingSearch + "." + classToSearchFor);
		if (clazz == null) {
			clazz = classes.get(classToSearchFor);
		}
		if (clazz == null && classInitiatingSearch != null) {
			clazz = prj.searchForClass(depth, name, classToSearchFor);
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

	public Integer getUpstreamReferenceDepth() {
		return upstreamReferenceDepth;
	}

	public void setUpstreamReferenceDepth(Integer upstreamReferenceDepth) {
		this.upstreamReferenceDepth = upstreamReferenceDepth;
	}

	public Integer getDownstreamReferenceDepth() {
		return downstreamReferenceDepth;
	}

	public void setDownstreamReferenceDepth(Integer downstreamReferenceDepth) {
		this.downstreamReferenceDepth = downstreamReferenceDepth;
	}
	
	public void validatePassOne(int depth) {
		Logger.log(depth, "Validate Pass One: package " + name);
		for (Class clazz : classes.values()) {
			clazz.validatePassOne(depth + 1);
		}
	}

	public void validatePassTwo(int depth) {
		Logger.log(depth, "Validate Pass Two: package " + name);
		for (Class clazz : classes.values()) {
			clazz.validatePassTwo(depth + 1);
		}
	}

	// A value > 0xFF for any color means that 
	// the value should not be used
	private int mixColorToRGBValue(int red, int green, int blue)
	{
		int color = 0xFFFFFF; // white
				
		if (red < 0x100 || green < 0x100 || blue < 0x100)
		{
			// Limit negative values
			red = Math.max(0, red);
			green = Math.max(0, green);
			blue = Math.max(0, blue);
			
			// Ignore color (use 0x00) if > 0xFF
			if (red > 0xFF) {red = 0;}
			if (green > 0xFF) {green = 0;}
			if (blue > 0xFF) {blue = 0;}
			
			color = (red << 16) + (green << 8) + blue;
		}
		
		
		return color;
	}
	
	private int getColor(int colorStart, int colorEnd, int numberOfsteps, int steps) {
		int colorStep = (colorStart - colorEnd) / numberOfsteps;
		return colorStart - (colorStep * steps);

	}
	
	public String createGraph(GraphvizRenderer renderer, JavaFilter filter, List<String> edgeList) {
		Logger.log(0, "Package: " + this.name);

		String color;
		int red, green, blue;
		red = green = blue = 0xFF;

		if (searchRank > 0) {
			// yellow
			red = getColor(0xFF, 0xFF, 10, searchRank);
			green = getColor(0xFF, 0xE1, 10, searchRank);
			blue = getColor(0xFF, 0x3B, 10, searchRank);
		} else if (filter.getDownstreamDependencyDepth() != null && this.downstreamReferenceDepth > 0 && filter.getUpstreamReferenceDepth() != null && this.upstreamReferenceDepth > 0) {
			// red
			red = getColor(0xFF, 0xEF, 6, Math.min(upstreamReferenceDepth, downstreamReferenceDepth));
			green = getColor(0xFF, 0x53, 6, Math.min(upstreamReferenceDepth, downstreamReferenceDepth));
			blue = getColor(0xFF, 0x50, 6, Math.min(upstreamReferenceDepth, downstreamReferenceDepth));

		} else if (filter.getDownstreamDependencyDepth() != null && this.downstreamReferenceDepth > 0) {
			// teal
			red = getColor(0xFF, 0x00, 6, downstreamReferenceDepth);
			green = getColor(0xFF, 0xAC, 6, downstreamReferenceDepth);
			blue = getColor(0xFF, 0xC1, 6, downstreamReferenceDepth);
		} else if (filter.getUpstreamReferenceDepth() != null && this.upstreamReferenceDepth > 0) {
			// blue
			red = getColor(0xFF, 0x03, 6, upstreamReferenceDepth);
			green = getColor(0xFF, 0x9B, 6, upstreamReferenceDepth);
			blue = getColor(0xFF, 0xE5, 6, upstreamReferenceDepth);
		}

		color = "#" + String.format("%06X", mixColorToRGBValue(red, green, blue));
		
		
		StringBuffer sb = new StringBuffer();

		if (filter.getDiagramType() == DiagramType.PACKAGE_DIAGRAM) {
			sb.append(renderer.getBeginRecord(this.name, this.name, "", color));
			sb.append(renderer.getEndRecord());

			HashMap<String, Integer> referencedPackages = new HashMap<String, Integer>();
			for (Class c : classes.values()) {
				if ((filter.isFromFile() && c.fromFile) || !filter.isFromFile()) {
					for (Package p : c.packageDependencies) {
						if ((filter.isFromFile() && p.fromFile) || !filter.isFromFile()) {
							Integer count = referencedPackages.get(p.name);
							if (count == null)
								count = 0;
							referencedPackages.put(p.name, count);
						}
					}

					for (Class c1 : c.classDependencies.values()) {
						if ((filter.isFromFile() && c1.fromFile) || !filter.isFromFile()) {
							Integer count = referencedPackages.get(c1.pkg.name);
							if (count == null)
								count = 0;
							referencedPackages.put(c1.pkg.name, count + 1);
						}
					}
				}
			}

			for (String pkgName : referencedPackages.keySet()) {
				if (!filter.getPackagesToExclude().contains(pkgName)) {
					Integer count = referencedPackages.get(pkgName);
					edgeList.add((String) renderer.addEdge(this.name, pkgName, count.toString(), false));
				}
			}

		} else {

			sb.append(renderer.getBeginCluster(name));

			for (Class clazz : classes.values()) {

				if ((filter.isFromFile() && clazz.fromFile) || !filter.isFromFile()) {
					if (clazz.name == null) {
						System.err.println("!!!" + this.name + ": class with null name");
					} else {
						if (!filter.getClassesToExclude().contains(this.name + "." + clazz.name)) {
							sb.append(clazz.createGraph(renderer, filter, edgeList));
						}
					}
				}
			}
			sb.append(renderer.getEndCluster());
		}

		return sb.toString();
	}
	
	public boolean isFromFile() {
		return fromFile;
	}
	
	public void setFromFile(boolean scannedFile) {
		this.fromFile = scannedFile;
	}
}