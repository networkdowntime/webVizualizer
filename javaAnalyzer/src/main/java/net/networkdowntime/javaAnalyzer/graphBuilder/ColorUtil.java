package net.networkdowntime.javaAnalyzer.graphBuilder;

public class ColorUtil {

	/**
	 * Mixes RGB values between 0-255 into a single RGB integer value for conversion to a hexadecimal value.
	 * Performs bounds clipping on each component color.
	 * 
	 * @param red
	 * @param green
	 * @param blue
	 * @return
	 */
	public static int mixColorToRGBValue(int red, int green, int blue) {
		// Limit negative values
		red = Math.max(0, red);
		green = Math.max(0, green);
		blue = Math.max(0, blue);

		// Limit positive values
		red = Math.min(255, red);
		green = Math.min(255, green);
		blue = Math.min(255, blue);

		return (red << 16) + (green << 8) + blue;
	}

	/**
	 * Based on a start and end range with a specified number of steps, calculates the color value for the current step.
	 *  
	 * @param colorStart
	 * @param colorEnd
	 * @param numberOfsteps
	 * @param steps
	 * @return
	 */
	public static int getColor(int colorStart, int colorEnd, int numberOfsteps, int steps) {
		int colorStep = (colorStart - colorEnd) / numberOfsteps;
		return colorStart - (colorStep * steps);

	}
}
