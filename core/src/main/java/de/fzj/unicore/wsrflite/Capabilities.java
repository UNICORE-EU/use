package de.fzj.unicore.wsrflite;

/**
 * the Capabilities interface is used to automatically load extensions
 * using the Java service provider interface approach.
 * 
 * a Capabilities implementation provides a wrapper around a set of {@link Capability} instances 
 * 
 * @author schuller
 */
public interface Capabilities {

	/**
	 * returns the capabilities
	 */
	public Capability[] getCapabilities();
	
}
