package net.networkdowntime.webVizualizer.dto;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Status {
	boolean success;
	
	public Status(boolean success) {
		this.success = success;
	}
	
	public boolean getSuccess() {
		return success;
	}
	
	public void setSuccess(boolean success) {
		this.success = success;
	}
}
