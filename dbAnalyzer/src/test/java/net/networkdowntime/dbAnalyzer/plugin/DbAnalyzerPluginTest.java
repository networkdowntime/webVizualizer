package net.networkdowntime.dbAnalyzer.plugin;

import static org.junit.Assert.*;

import org.junit.Test;


public class DbAnalyzerPluginTest {

	@Test
	public void getJavaScript() {
		DbAnalyzerPlugin analyzer = new DbAnalyzerPlugin();
		String str = analyzer.getJavaScript();
		assertNotNull(str);
		assertTrue("JS not empty", !str.isEmpty());
	}

	@Test
	public void getSideBar() {
		DbAnalyzerPlugin analyzer = new DbAnalyzerPlugin();
		String str = analyzer.getSideBar();
		assertNotNull(str);
		assertTrue("HTML not empty", !str.isEmpty());
	}

}
