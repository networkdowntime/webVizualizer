package net.networkdowntime.javaAnalyzer;

import static org.junit.Assert.*;

import java.util.HashSet;

import org.junit.Test;

public class TextScrubberTest {
	
	@Test
	public void testScrub1() {
		TextScrubber.setIncludeCamelCase(true);
		TextScrubber.setIncludeHyphenatedWords(false);
		TextScrubber.setStemWord(false);

		String testString = "System.out.println(\"print camelName\")";
		HashSet<String> results = new HashSet<String>(TextScrubber.scrub(testString));
		assertTrue(results.contains("println"));
		assertTrue(results.contains("print"));
		assertTrue(results.contains("camelname"));
		assertTrue(results.contains("camel"));
		assertTrue(results.contains("name"));
		assertFalse(results.contains("system")); // common class in java 
		assertFalse(results.contains("out")); // stop word 
	}
	
	@Test
	public void testScrubIncludeCamelCase() {
		TextScrubber.setIncludeCamelCase(true);
		TextScrubber.setIncludeHyphenatedWords(false);
		TextScrubber.setStemWord(false);

		String testString = "camelName";
		HashSet<String> results = new HashSet<String>(TextScrubber.scrub(testString));
		assertTrue(results.contains("camelname"));
		assertTrue(results.contains("camel"));
		assertTrue(results.contains("name"));
	}
	
	@Test
	public void testScrubExcludeCamelCase() {
		TextScrubber.setIncludeCamelCase(false);
		TextScrubber.setIncludeHyphenatedWords(false);
		TextScrubber.setStemWord(false);

		String testString = "camelName";
		HashSet<String> results = new HashSet<String>(TextScrubber.scrub(testString));
		assertFalse(results.contains("camelname"));
		assertTrue(results.contains("camel"));
		assertTrue(results.contains("name"));
	}
	
	@Test
	public void testScrubIncludeHyphenatedWords() {
		TextScrubber.setIncludeCamelCase(false);
		TextScrubber.setIncludeHyphenatedWords(true);
		TextScrubber.setStemWord(false);

		String testString = "camel-name";
		HashSet<String> results = new HashSet<String>(TextScrubber.scrub(testString));
		assertTrue(results.contains("camel-name"));
		assertTrue(results.contains("camel"));
		assertTrue(results.contains("name"));
	}
	
	@Test
	public void testScrubExcludeHyphenatedWords() {
		TextScrubber.setIncludeCamelCase(false);
		TextScrubber.setIncludeHyphenatedWords(false);
		TextScrubber.setStemWord(false);

		String testString = "camel-name";
		HashSet<String> results = new HashSet<String>(TextScrubber.scrub(testString));
		assertFalse(results.contains("camel-name"));
		assertTrue(results.contains("camel"));
		assertTrue(results.contains("name"));
	}
	
	@Test
	public void testScrubStemWords() {
		TextScrubber.setIncludeCamelCase(false);
		TextScrubber.setIncludeHyphenatedWords(false);
		TextScrubber.setStemWord(true);

		String testString = "welcoming";
		HashSet<String> results = new HashSet<String>(TextScrubber.scrub(testString));
		assertTrue(results.contains("welcom"));
		assertFalse(results.contains("welcoming"));
	}
	
	@Test
	public void testScrubStemWordsWithCamelCaseIncludeCamelCase() {
		TextScrubber.setIncludeCamelCase(true);
		TextScrubber.setIncludeHyphenatedWords(false);
		TextScrubber.setStemWord(true);

		String testString = "welcomingParty";
		HashSet<String> results = new HashSet<String>(TextScrubber.scrub(testString));
		assertTrue(results.contains("welcom"));
		assertTrue(results.contains("parti"));
		assertTrue(results.contains("welcomingparti"));
	}
	
	@Test
	public void testScrubStemWordsWithCamelCaseExcludeCamelCase() {
		TextScrubber.setIncludeCamelCase(false);
		TextScrubber.setIncludeHyphenatedWords(false);
		TextScrubber.setStemWord(true);

		String testString = "welcomingParty";
		HashSet<String> results = new HashSet<String>(TextScrubber.scrub(testString));
		assertTrue(results.contains("welcom"));
		assertTrue(results.contains("parti"));
		assertFalse(results.contains("welcomingParty"));
		assertFalse(results.contains("welcomingparti"));
	}
	
}
