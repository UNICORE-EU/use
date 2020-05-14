package eu.unicore.services.rest;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import de.fzj.unicore.wsrflite.Kernel;

import eu.unicore.services.rest.USERestApplication;

/**
 * REST application for local/shared Registry
 *
 * @author schuller
 */
public class RegistryApplication extends Application implements USERestApplication {

	@Override
	public void initialize(Kernel kernel) throws Exception {
		// NOP
	}

	@Override
	public Set<Class<?>> getClasses() {
		Set<Class<?>>classes=new HashSet<Class<?>>();
		classes.add(Registries.class);
		return classes;
	}
	
}
