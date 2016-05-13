package net.networkdowntime.webVizualizer.plugin;

public interface PluginInterface {

	/**
	 * This is the id of the main div for the sidebar, used to show, hide, fix sizes, etc.
	 * @return
	 */
	public String getId();

	/**
	 * This is the label for the plugin that will show up in the main menu
	 * @return
	 */
	public String getLabel();

	/**
	 * Returns the text for the JavaScript file that handle the client side plugin behaviors
	 * @return
	 */
	public String getJavaScript();

	/**
	 * Returns the HTML for the sidebar divs
	 * @return
	 */
	public String getSideBar();

}
