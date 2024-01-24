package eu.unicore.services.utils.deployment;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Dummy implementation used when no real services config is provided (e.g.
 * when all services are deployed via API or during tests)
 * @author K. Benedyczak
 */
public class NullServiceConfigurator implements IServiceConfigurator {

	@Override
	public Properties loadProperties() throws IOException {
		return new Properties();
	}

	@Override
	public void configureServices() throws Exception {
	}

	@Override
	public List<Runnable> getInitTasks() {
		return Collections.emptyList();
	}

}
