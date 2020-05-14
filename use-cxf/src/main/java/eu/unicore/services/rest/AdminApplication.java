package eu.unicore.services.rest;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import de.fzj.unicore.wsrflite.Kernel;

import eu.unicore.services.rest.USERestApplication;

/**
 * REST application for server administration
 *
 * @author schuller
 */
public class AdminApplication extends Application implements USERestApplication {

	@Override
	public void initialize(Kernel kernel) throws Exception {
		// NOP
	}

	@Override
	public Set<Class<?>> getClasses() {
		Set<Class<?>>classes=new HashSet<Class<?>>();
		classes.add(Admin.class);
		return classes;
	}
	
}
