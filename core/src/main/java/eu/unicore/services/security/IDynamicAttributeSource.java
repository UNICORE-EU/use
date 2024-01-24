package eu.unicore.services.security;

import java.io.IOException;

import eu.unicore.security.Client;
import eu.unicore.security.SubjectAttributesHolder;

/**
 * IAttributeSource provides the interface for UNICORE/X to retrieve authorisation information
 * (attributes) for a particular request from an attribute provider.
 * <p>
 * The dynamic AIPs, called also DAPs (dynamic attribute points), are called after the authorisation step to provide
 * attributes dynamically assigned to the user basing on previously established static attributes. 
 * Such dynamic attributes can be used for incarnation only (e.g. xlogin).
 * 
 * @see IAttributeSource
 * @author schuller
 * @author golbi
 */
public interface IDynamicAttributeSource extends IAttributeSourceBase {

	/**
	 * Retrieves a map of attributes based on the supplied Client object.
	 * 
	 * Since DAPs can be chained, it might be sometimes useful to see attributes returned by 
	 * DAPs that have run previously. This information is supplied in the "otherAuthoriserInfo" map.
	 * 
	 * Attribute sources must not make any authorisation decisions. Thus, no exceptions must be thrown
	 * if no attributes are found. Only IOExceptions should be thrown in case of technical problems 
	 * contacting the actual attribute provider. This is to allow upstream code (i.e. the UNICORE/X 
	 * server) to log the error, or to take any other action (like notify an administrator). 
	 * If no attributes are found, an empty map or <code>null</code> must be returned.
	 * 
	 * @param client contains data established by the static AIPs, which undergone authorisation. The
	 * object state should not be modified by implementations.
	 * @param otherAuthoriserInfo - attributes returned by other DAPs, which may be <code>null</code>
	 * @return subject's dynamic attributes
	 * @throws IOException in case of technical problems
	 */
	public SubjectAttributesHolder getAttributes(final Client client, 
			SubjectAttributesHolder otherAuthoriserInfo) throws IOException;	 
}
