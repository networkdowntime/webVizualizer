package net.networkdowntime.javaAnalyzer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TextScrubber {

	private static Set<String> preProcessStopWords = new HashSet<String>();
	private static Set<String> stopWords = new HashSet<String>();
	private static int minWordSize = 2;
	private static boolean stemWords = false;
	private static boolean includeCamelCase = true;
	private static boolean includeHyphenatedWords = false;

	static {
		TextScrubber ts = new TextScrubber();
		stopWords.addAll(ts.loadWords("stop_words.xml"));

		addStopWords(ts.loadWords("java_keywords.xml"));
		addStopWords(ts.loadWords("java_common_classes.xml"));
		setIncludeHyphenatedWords(true);
	}

	public static void addStopWords(Set<String> stopWords) {
		TextScrubber.stopWords.addAll(stopWords);
	}

	public static void addPreProcessStopWords(Set<String> preProcessStopWords) {
		TextScrubber.preProcessStopWords.addAll(preProcessStopWords);
	}

	public static void setStemWord(boolean stemWords) {
		TextScrubber.stemWords = stemWords;
	}

	public static void setIncludeCamelCase(boolean includeCamelCase) {
		TextScrubber.includeCamelCase = includeCamelCase;
	}

	public static void setIncludeHyphenatedWords(boolean preserveHyphenatedWords) {
		TextScrubber.includeHyphenatedWords = preserveHyphenatedWords;
	}

	public static String scrubToString(String text) {
		StringBuilder output = new StringBuilder();

		for (String s : scrub(text)) {
			output.append(" " + s);
		}

		return output.toString().trim();
	}

	public static List<String> scrub(String text) {

		PorterStemmer stemmer = new PorterStemmer();

		List<String> output = new ArrayList<String>();

		text = text.trim();

		if (text.isEmpty()) {
			return output;
		}

		for (String preProcessWord : preProcessStopWords) {
			text = text.replaceAll(preProcessWord, " ");
		}

		// check for line with only // comments
		if (text.startsWith("//")) {
			text = text.substring(2).trim();
		}

		// check for javadoc style comments
		if (text.startsWith("/**")) {
			text = text.substring(3).trim();
		}

		// check for c style comments
		if (text.startsWith("/*")) {
			text = text.substring(2).trim();
		}

		// Remove URLS and XML tags
		// The official list of URI schemes from iana.orgm, plus hdfs since Hadoop is one of our systems
		text = text.replaceAll(
				"(hdfs:aaa|aaas|about|acap|acct|acr|adiumxtra|afp|afs|aim|appdata|apt|attachment|aw|barion|beshare|bitcoin|blob|bolo|callto|cap|chrome|chrome-extension|cid|coap|coaps|com-eventbrite-attendee|content|crid|cvs|data|dav|dict|dis|dlna-playcontainer|dlna-playsingle|dns|dntp|dtn|dvb|ed2k|example|facetime|fax|feed|feedready|file|filesystem|finger|fish|ftp|geo|gg|git|gizmoproject|go|gopher|gtalk|h323|ham|hcp|http|https|iax|icap|icon|im|imap|info|iotdisco|ipn|ipp|ipps|irc|irc6|ircs|iris|iris.beep|iris.lwz|iris.xpc|iris.xpcs|isostore|itms|jabber|jar|jms|keyparc|lastfm|ldap|ldaps|magnet|mailserver|mailto|maps|market|message|mid|mms|modem|ms-access|ms-drive-to|ms-excel|ms-getoffice|ms-help|ms-infopath|ms-media-stream-id|ms-project|ms-powerpoint|ms-publisher|ms-search-repair|ms-secondary-screen-controller|ms-secondary-screen-setup|ms-settings|ms-settings-airplanemode|ms-settings-bluetooth|ms-settings-camera|ms-settings-cellular|ms-settings-cloudstorage|ms-settings-emailandaccounts|ms-settings-language|ms-settings-location|ms-settings-lock|ms-settings-nfctransactions|ms-settings-notifications|ms-settings-power|ms-settings-privacy|ms-settings-proximity|ms-settings-screenrotation|ms-settings-wifi|ms-settings-workplace|ms-spd|ms-transit-to|ms-visio|ms-walk-to|ms-word|msnim|msrp|msrps|mtqp|mumble|mupdate|mvn|news|nfs|ni|nih|nntp|notes|oid|opaquelocktoken|pack|palm|paparazzi|pkcs11|platform|pop|pres|prospero|proxy|psyc|query|redis|rediss|reload|res|resource|rmi|rsync|rtmfp|rtmp|rtsp|rtsps|rtspu|secondlife|service|session|sftp|sgn|shttp|sieve|sip|sips|skype|smb|sms|smtp|snews|snmp|soap.beep|soap.beeps|soldat|spotify|ssh|steam|stun|stuns|submit|svn|tag|teamspeak|tel|teliaeid|telnet|tftp|things|thismessage|tip|tn3270|tool|turn|turns|tv|udp|unreal|urn|ut2004|v-event|vemmi|ventrilo|videotex|view-source|wais|webcal|ws|wss|wtai|wyciwyg|xcon|xcon-userid|xfire|xmlrpc.beep|xmlrpc.beeps|xmpp|xri|ymsgr|z39.50|z39.50r|z39.50s)\\:\\S+",
				" ");
		text = text.replaceAll("<[a-zA-Z_][a-zA-Z0-9-_\\.]+>", "");

		// explode punctuation to a space
		text = text.replaceAll("[\\{|\\}|\\(|\\)|=|+|*|<|\\[|\\]|\\>|\\^|\\$|\\&\\&|\\|\\||`|#|~|_|'|\"|\\.]", " ").trim();

		text = text.replaceAll("\\\\t", " ").trim();
		text = text.replaceAll("\\\\r", " ").trim();
		text = text.replaceAll("\\\\n", " ").trim();
		text = text.replaceAll("\\\\", " ").trim();
		text = text.replaceAll("(^\\s)[0-9]+\\.[0-9]+", " ").trim(); // decimal numbers
		text = text.replaceAll("(^\\s)[0-9]+f", " ").trim(); // integer numbers as a float
		text = text.replaceAll("(^\\s)[0-9]+d", " ").trim(); // integer numbers as a double
		text = text.replaceAll("(^\\s)0[x|X][0-9a-fA-F]+", " ").trim(); // integer numbers as hex
		text = text.replaceAll("(^\\s)[0-9]+", " ").trim(); // integer numbers
		text = text.replaceAll("\\s+", " ");

		// Split CamelCase
		if (!text.isEmpty()) {
			for (String word : text.split("\\s+")) {

				if (word.length() >= minWordSize) {
					if (!stopWords.contains(word.toLowerCase())) {

						String[] explodedWord = word.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])"); // CamelCase splitter

						if (explodedWord.length > 1) {
							if (includeCamelCase) {
								handleHyphens(output, stemmer, word);
							}

							for (String w : explodedWord) {
								if (w.length() >= minWordSize && !stopWords.contains(w.toLowerCase())) {
									handleHyphens(output, stemmer, w);
								}
							}
						} else {
							handleHyphens(output, stemmer, word);
						}
					}
				}
			}
		}

		return output;
	}
	
	private static void handleHyphens(List<String> output, PorterStemmer stemmer, String word) {
		if (includeHyphenatedWords) {
			if (word.contains("-")) { // add the hyphenated word
				output.add((stemWords) ? stemmer.stem(word.toLowerCase()) : word.toLowerCase());
			}
			for (String w : word.split("-")) { // split and add the component words
				if (!stopWords.contains(word.toLowerCase())) {
					output.add((stemWords) ? stemmer.stem(w.toLowerCase()) : w.toLowerCase());
				}
			}
		} else { // don't include the hyphenated word
			for (String w : word.split("-")) { // split and add the component words
				if (!stopWords.contains(word.toLowerCase())) {
					output.add((stemWords) ? stemmer.stem(w.toLowerCase()) : w.toLowerCase());
				}
			}
		}		
	}

	private Set<String> loadWords(String resource) {
		Set<String> words = new HashSet<String>();

		ClassLoader classLoader = getClass().getClassLoader();
		File file = new File(classLoader.getResource(resource).getFile());

		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			while (reader.ready()) {
				String line = reader.readLine().trim();
				if (line.startsWith("<word>") && line.endsWith("</word>")) {
					words.add(line.substring(6, line.length() - 7).toLowerCase());
				}

			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return words;
	}

	@SuppressWarnings("unused")
	private String readResourceFile(String resourceName) {
		ClassLoader classLoader = this.getClass().getClassLoader();
		File file = new File(classLoader.getResource(resourceName).getFile());
		StringBuilder builder = new StringBuilder();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			while (reader.ready()) {
				builder.append(reader.readLine());
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return builder.toString();
	}

}
