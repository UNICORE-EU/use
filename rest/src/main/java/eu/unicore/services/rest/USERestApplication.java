package eu.unicore.services.rest;

import eu.unicore.services.Kernel;

/**
 * helper interface used to initialize a REST application
 *
 * @author schuller
 */
public interface USERestApplication {

	public void initialize(Kernel kernel) throws Exception;

}
