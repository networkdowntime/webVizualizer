package net.networkdowntime.javaAnalyzer.viewFilter;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public enum DiagramType implements Serializable {
	PACKAGE_DIAGRAM, CLASS_ASSOCIATION_DIAGRAM, UNREFERENCED_CLASSES, METHOD_CALL_DIAGRAM
}
