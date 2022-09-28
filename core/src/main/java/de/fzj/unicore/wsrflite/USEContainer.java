package de.fzj.unicore.wsrflite;

import eu.unicore.util.Log;

@Deprecated
public class USEContainer extends eu.unicore.services.USEContainer {

	public USEContainer(String configFile, String name) throws Exception {
		super(configFile, name);
		throw new IllegalStateException("Use 'eu.unicore.services.USEContainer' instead.");
	}

	public static void main(String[] args) throws Exception {
		Log.getLogger("unicore").warn("DEPRECATION WARNING: please use main class "
				+ "'eu.unicore.services.USEContainer' in start.sh!");
		eu.unicore.services.USEContainer.main(args);
	}
}
