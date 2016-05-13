package net.networkdowntime.javaAnalyzer.plugin;

import static org.junit.Assert.*;

import org.junit.Test;


public class JavaAnalyzerPluginTest {

	@Test
	public void getJavaScript() {
		JavaAnalyzerPlugin analyzer = new JavaAnalyzerPlugin();
		String str = analyzer.getJavaScript();
		assertNotNull(str);
		assertTrue("JS not empty", !str.isEmpty());
	}

	@Test
	public void getSideBar() {
		JavaAnalyzerPlugin analyzer = new JavaAnalyzerPlugin();
		String str = analyzer.getSideBar();
		assertNotNull(str);
		assertTrue("HTML not empty", !str.isEmpty());
	}

}
