package eu.unicore.services.rest.registry;

import java.util.HashSet;
import java.util.Set;

import eu.unicore.services.rest.USERestApplication;
import jakarta.ws.rs.core.Application;

/**
 * REST application for local/shared Registry
 *
 * @author schuller
 */
public class RegistryApplication extends Application implements USERestApplication {

	@Override
	public Set<Class<?>> getClasses() {
		Set<Class<?>>classes=new HashSet<>();
		classes.add(Registries.class);
		return classes;
	}

}
