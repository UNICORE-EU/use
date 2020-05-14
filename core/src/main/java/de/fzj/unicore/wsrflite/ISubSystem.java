package de.fzj.unicore.wsrflite;

/**
 * used to show the status of some subsystem
 * 
 * TODO add control, restart etc
 * 
 * @author schuller
 */
public interface ISubSystem {

	/**
	 * description for admins / logfiles
	 */
	public String getStatusDescription();

	/**
	 * the name of this subsystem
	 */
	public String getName();
	
	/**
	 * (re)start this subsystem
	 */
	default public void start(Kernel kernel) throws Exception {}

}
