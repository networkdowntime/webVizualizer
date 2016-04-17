package net.networkdowntime.javaAnalyzer.logger;

public class Logger {
	private static boolean ENABLE_LOGGING = false;
	
	public static void log(int depth, String str) {
		if (ENABLE_LOGGING) {
			for (int i = 0; i < depth; i++) {
				System.out.print("    ");
			}
			System.out.println(str);
		}
	}

}
