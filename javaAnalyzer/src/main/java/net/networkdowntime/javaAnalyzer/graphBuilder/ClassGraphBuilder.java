package net.networkdowntime.javaAnalyzer.graphBuilder;

import java.util.HashMap;
import java.util.List;

import net.networkdowntime.javaAnalyzer.javaModel.Block;
import net.networkdowntime.javaAnalyzer.javaModel.Class;
import net.networkdowntime.javaAnalyzer.javaModel.Method;
import net.networkdowntime.javaAnalyzer.javaModel.ModelVisitor;
import net.networkdowntime.javaAnalyzer.javaModel.Package;
import net.networkdowntime.javaAnalyzer.javaModel.Project;
import net.networkdowntime.javaAnalyzer.logger.Logger;
import net.networkdowntime.javaAnalyzer.viewFilter.DiagramType;
import net.networkdowntime.javaAnalyzer.viewFilter.JavaFilter;
import net.networkdowntime.renderer.GraphvizRenderer;

public class ClassGraphBuilder extends ModelVisitor implements GraphBuilder {
	private JavaFilter filter;
	private StringBuilder graph = new StringBuilder();

	@Override
	public void visit(Block visitor) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(Class visitor) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(Method visitor) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(Package visitor) {

	}

	public String createGraph(Project project, JavaFilter filter) {
		this.filter = filter;
		
		project.resetReferenceDepths();
		
		this.visit(project);
		return graph.toString();
	}

// Project createGraph
//		public String createGraph(JavaFilter filter) {
//			if (filter.getAdvancedSearchQuery() != null) {
//				String query = filter.getAdvancedSearchQuery();
//				if (!query.isEmpty()) {
//					if (filter.getDiagramType() == DiagramType.PACKAGE_DIAGRAM) {
//						filter.setPackagesToExclude(new HashSet<String>(getPackageNames()));
//						filter.setClassesToExclude(new HashSet<String>(new ArrayList<String>()));
//					} else {
//						filter.setPackagesToExclude(new HashSet<String>());
//						filter.setClassesToExclude(new HashSet<String>(getClassNames(new ArrayList<String>())));
//					}
//					int rank = 0;
//					LinkedHashMap<String, Float> results = new LinkedHashMap<String, Float>(search.query(query, 10, filter.getDiagramType() != DiagramType.PACKAGE_DIAGRAM));
//					results = results.entrySet().stream()
//							.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
//							.collect(Collectors.toMap(
//									Map.Entry::getKey,
//									Map.Entry::getValue,
//									(x, y) -> {
//										throw new AssertionError();
//									},
//									LinkedHashMap::new));
//
//					for (String name : results.keySet()) {
//						if (filter.getDiagramType() == DiagramType.PACKAGE_DIAGRAM) {
//							filter.getPackagesToExclude().remove(name);
//						} else {
//							filter.getClassesToExclude().remove(name);
//							for (Package pkg : this.packages.values()) {
//								for (Class c : pkg.getClasses().values()) {
//									if (c.getCanonicalName().equals(name)) {
//										c.searchRank = rank;
//									}
//								}
//							}
//						}
//						rank++;
//					}
//				}
//			}
//
//			// Replace with test of filter for depth selection
//			Integer downDepth = filter.getDownstreamDependencyDepth();
//			Integer upDepth = filter.getUpstreamReferenceDepth();
//			if ((downDepth != null) || (upDepth != null)) {
//				if (filter.getDiagramType() == DiagramType.CLASS_ASSOCIATION_DIAGRAM) {
//					unExcludeClassesBasedOnDepth(downDepth, upDepth, filter);
//				} else if (filter.getDiagramType() == DiagramType.PACKAGE_DIAGRAM) {
//					unExcludePackagesBasedOnDepth(downDepth, upDepth, filter);
//				}
//			}
//
//			GraphvizRenderer renderer = new GraphvizDotRenderer();
//
//			StringBuffer sb = new StringBuffer();
//
//			List<String> edgeList = new ArrayList<String>();
//
//			sb.append(renderer.getHeader());
//
//			for (String pkgName : packages.keySet()) {
//				Package pkg = packages.get(pkgName);
//				// if (pkg.inPath) {
//				boolean exclude = filter.getPackagesToExclude().contains(pkg.name);
//
//				if (!exclude) {
//					if ((filter.isFromFile() && pkg.isFromFile()) || !filter.isFromFile()) {
//						sb.append(pkg.createGraph(renderer, filter, edgeList));
//					}
//				}
//			}
//
//			for (String edge : edgeList) {
//				sb.append(edge);
//			}
//
//			sb.append(renderer.getFooter());
//
//			return sb.toString();
//		}

// Package createGraph
//	public String createGraph(GraphvizRenderer renderer, JavaFilter filter, List<String> edgeList) {
//		Logger.log(0, "Package: " + this.name);
//
//		String color;
//		int red, green, blue;
//		red = green = blue = 0xFF;
//
//		if (searchRank > 0) {
//			// yellow
//			red = getColor(0xFF, 0xFF, 10, searchRank);
//			green = getColor(0xFF, 0xE1, 10, searchRank);
//			blue = getColor(0xFF, 0x3B, 10, searchRank);
//		} else if (filter.getDownstreamDependencyDepth() != null && this.downstreamReferenceDepth > 0 && filter.getUpstreamReferenceDepth() != null && this.upstreamReferenceDepth > 0) {
//			// red
//			red = getColor(0xFF, 0xEF, 6, Math.min(upstreamReferenceDepth, downstreamReferenceDepth));
//			green = getColor(0xFF, 0x53, 6, Math.min(upstreamReferenceDepth, downstreamReferenceDepth));
//			blue = getColor(0xFF, 0x50, 6, Math.min(upstreamReferenceDepth, downstreamReferenceDepth));
//
//		} else if (filter.getDownstreamDependencyDepth() != null && this.downstreamReferenceDepth > 0) {
//			// teal
//			red = getColor(0xFF, 0x00, 6, downstreamReferenceDepth);
//			green = getColor(0xFF, 0xAC, 6, downstreamReferenceDepth);
//			blue = getColor(0xFF, 0xC1, 6, downstreamReferenceDepth);
//		} else if (filter.getUpstreamReferenceDepth() != null && this.upstreamReferenceDepth > 0) {
//			// blue
//			red = getColor(0xFF, 0x03, 6, upstreamReferenceDepth);
//			green = getColor(0xFF, 0x9B, 6, upstreamReferenceDepth);
//			blue = getColor(0xFF, 0xE5, 6, upstreamReferenceDepth);
//		}
//
//		color = "#" + String.format("%06X", mixColorToRGBValue(red, green, blue));
//
//		StringBuffer sb = new StringBuffer();
//
//		if (filter.getDiagramType() == DiagramType.CLASS_DIAGRAM) {
//			sb.append(renderer.getBeginCluster(name));
//
//			for (Class clazz : classes.values()) {
//
//				if ((filter.isFromFile() && clazz.fromFile) || !filter.isFromFile()) {
//					if (clazz.name == null) {
//						System.err.println("!!!" + this.name + ": class with null name");
//					} else {
//						if (!filter.getClassesToExclude().contains(clazz.getCanonicalName())) {
//							sb.append(clazz.createGraph(renderer, filter, edgeList));
//						}
//					}
//				}
//			}
//			sb.append(renderer.getEndCluster());
//		}
//
//		return sb.toString();
//	}
	
// Class createGraph
//		public String createGraph(GraphvizRenderer renderer, JavaFilter filter, List<String> edgeList) {
//
//			Logger.log(1, "Class: " + this.name);
//
//			HashSet<String> refsToSkip = new HashSet<String>();
//
//			StringBuffer sb = new StringBuffer();
//			
//			String color;
//			int red, green, blue;
//			red = green = blue = 0xFF;
//
//			if (searchRank > 0) {
//				// yellow
//				red = getColor(0xFF, 0xFF, 10, searchRank);
//				green = getColor(0xFF, 0xE1, 10, searchRank);
//				blue = getColor(0xFF, 0x3B, 10, searchRank);
//			} else if (filter.getDownstreamDependencyDepth() != null && this.downstreamReferenceDepth > 0 && filter.getUpstreamReferenceDepth() != null && this.upstreamReferenceDepth > 0) {
//				// red
//				red = getColor(0xFF, 0xEF, 6, Math.min(upstreamReferenceDepth, downstreamReferenceDepth));
//				green = getColor(0xFF, 0x53, 6, Math.min(upstreamReferenceDepth, downstreamReferenceDepth));
//				blue = getColor(0xFF, 0x50, 6, Math.min(upstreamReferenceDepth, downstreamReferenceDepth));
//
//			} else if (filter.getDownstreamDependencyDepth() != null && this.downstreamReferenceDepth > 0) {
//				// teal
//				red = getColor(0xFF, 0x00, 6, downstreamReferenceDepth);
//				green = getColor(0xFF, 0xAC, 6, downstreamReferenceDepth);
//				blue = getColor(0xFF, 0xC1, 6, downstreamReferenceDepth);
//			} else if (filter.getUpstreamReferenceDepth() != null && this.upstreamReferenceDepth > 0) {
//				// blue
//				red = getColor(0xFF, 0x03, 6, upstreamReferenceDepth);
//				green = getColor(0xFF, 0x9B, 6, upstreamReferenceDepth);
//				blue = getColor(0xFF, 0xE5, 6, upstreamReferenceDepth);
//			}
//
//			color = "#" + String.format("%06X", mixColorToRGBValue(red, green, blue));
//			
//			
//			if ((filter.getDiagramType() == DiagramType.UNREFERENCED_CLASSES && this.referencedByClass.size() == 0)
//					|| filter.getDiagramType() != DiagramType.UNREFERENCED_CLASSES) {
//				if (isAnonymous) {
//					sb.append(renderer.getBeginRecord(this.getCanonicalName(), "<anonymous>\r\n" + this.getName(), "", color));
//				}else if (isInterface) {
//						sb.append(renderer.getBeginRecord(this.getCanonicalName(), "<interface>\r\n" + this.getName(), "", color));
//				} else {
//					sb.append(renderer.getBeginRecord(this.getCanonicalName(), this.getName(), "", color));
//				}
//				
//				if (filter.isShowFields()) {
//					for (String field : this.varNameClassMap.keySet()) {
//						Class clazz = this.varNameClassMap.get(field);
//						sb.append(renderer.addRecordField(field, field + ": " + clazz.getName()));
//					}
//				}
//
//				if (filter.isShowMethods()) {
//					for (Method method : methods.values()) {
//						sb.append(renderer.addRecordField(method.getName(), method.name));
//					}
//				}
//
//				sb.append(renderer.getEndRecord());
//
//				// Add edge for extending another class, if only 1 reference to that
//				// class don't add a reference edge later
//				if (extnds != null) {
//					boolean exclude = filter.getPackagesToExclude().contains(extnds.pkg.name);
//					exclude = exclude || filter.getClassesToExclude().contains(extnds.getCanonicalName());
//					if (filter.isFromFile())
//						exclude = exclude || (filter.isFromFile() && !extnds.fromFile);
//
//					if (!exclude) {
//						edgeList.add((String) renderer.addReversedEdge(this.getCanonicalName(),	extnds.getCanonicalName(), "", true));
//
//						Integer count = this.unresolvedClassCount.get(extnds.name);
//						if (count != null && count.intValue() == 1) {
//							refsToSkip.add(extnds.name);
//						}
//					}
//				}
//
//				// Add edges for implementing interfaces, if only 1 reference to
//				// that class don't add a reference edge later
//				for (Class intr : this.impls) {
//					boolean exclude = filter.getPackagesToExclude().contains(intr.pkg.name);
//					exclude = exclude || filter.getClassesToExclude().contains(intr.getCanonicalName());
//					if (filter.isFromFile())
//						exclude = exclude || (filter.isFromFile() && !intr.fromFile);
//
//					if (!exclude) {
//						edgeList.add((String) renderer.addReversedEdge(this.getCanonicalName(), intr.getCanonicalName(), "", true, false));
//
//						Integer count = this.unresolvedClassCount.get(intr.name);
//						if (count != null && count.intValue() == 1) {
//							refsToSkip.add(intr.name);
//						}
//					}
//				}
//
//				for (Class clazz : this.classDependencies.values()) {
//					if (!refsToSkip.contains(clazz.name)) {
//						boolean exclude = filter.getPackagesToExclude().contains(clazz.pkg.name);
//						exclude = exclude || filter.getClassesToExclude().contains(clazz.getCanonicalName());
//						if (!exclude) {
//							if ((filter.isFromFile() && clazz.fromFile)	|| !filter.isFromFile()) {
//								if ((filter.getDiagramType() == DiagramType.UNREFERENCED_CLASSES && clazz.referencedByClass.size() == 0)
//										|| filter.getDiagramType() != DiagramType.UNREFERENCED_CLASSES) {
//									edgeList.add((String) renderer.addEdge(this.getCanonicalName(), clazz.getCanonicalName(), ""));
//								}
//							}
//						}
//					}
//				}
//
//			}
//			return sb.toString();
//		}
	
}
