package eu.unicore.services;

/**
 * Helper interface to allow the USE container to inject the 
 * current {@link Kernel} instance, for example into
 * web service objects, security handlers, custom servlets and 
 * on-startup init tasks 
 * 
 * @author schuller
 */
public interface KernelInjectable {

	public void setKernel(Kernel kernel);
	
}
