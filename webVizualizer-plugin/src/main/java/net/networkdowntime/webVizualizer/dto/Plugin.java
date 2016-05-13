package net.networkdowntime.webVizualizer.dto;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Plugin {
	String id;
	String label;
	String pluginClass;

	public Plugin() {
	}
	
	public Plugin(String id, String label, String pluginClass) {
		super();
		this.id = id;
		this.label = label;
		this.pluginClass = pluginClass;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getPluginClass() {
		return pluginClass;
	}

	public void setPluginClass(String pluginClass) {
		this.pluginClass = pluginClass;
	}

}
