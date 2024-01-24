package eu.unicore.services.security;

import java.io.IOException;

import eu.unicore.security.SecurityTokens;
import eu.unicore.security.SubjectAttributesHolder;

/**
 * IAttributeSource provides the interface for UNICORE/X to retrieve authorisation information
 * (attributes) for a particular request from an attribute provider, based on information 
 * such as Client DN, certificate, etc, contained in an instance of {@link SecurityTokens}.
 * <p>
 * The attributes collected by implementations of this interface are static, i.e. are fixed for the user.
 * AIPs are used after authentication but before authorisation to collect attributes statically set for the user. 
 * The returned attributes determine authorisation (as role) and can also be used for incarnation.
 * 
 * @see IAttributeSource
 * @author schuller
 * @author golbi
 */
public interface IAttributeSource extends IAttributeSourceBase {

	/**
	 * UNICORE role attribute key. Only one may be selected.
	 */
	public static final String ATTRIBUTE_ROLE="role";

	/**
	 * UNIX login attribute key. Only one may be selected.
	 */
	public static final String ATTRIBUTE_XLOGIN="xlogin";

	/**
	 * UNIX primary group attribute key. Only one may be selected.
	 */
	public static final String ATTRIBUTE_GROUP="group";

	/**
	 * UNIX supplementary groups attribute key.
	 */
	public static final String ATTRIBUTE_SUPPLEMENTARY_GROUPS="supplementaryGroups";

	/**
	 * Add OS default groups.
	 */
	public static final String ATTRIBUTE_ADD_DEFAULT_GROUPS="addDefaultGroups";

	/**
	 * BSS queue attribute key.
	 */
	public static final String ATTRIBUTE_QUEUES="queue";

	/**
	 * Virtual Organisations  attribute key.
	 */
	public static final String ATTRIBUTE_VOS="virtualOrganisations";

	/**
	 * Selected Virtual Organisation attribute key.
	 */
	public static final String ATTRIBUTE_SELECTED_VO="selectedVirtualOrganisation";


	/**
	 * role attribute value: trusted agent as asserted by a SAML trust delegation assertion 
	 */
	public static final String ROLE_TRUSTED_AGENT="trusted-agent";

	/**
	 * role attribute value: admin
	 */
	public static final String ROLE_ADMIN="admin";

	/**
	 * Retrieves a map of attributes based on the supplied SecurityTokens.
	 * 
	 * Since authorisers can be chained, it might be sometimes useful to see attributes returned by 
	 * authorisers that have run previously. This information is supplied in the "otherAuthoriserInfo" map.
	 * 
	 * Attribute sources must not make any authorisation decisions. Thus, no exceptions must be thrown
	 * if no attributes are found. Only IOExceptions should be thrown in case of technical problems 
	 * contacting the actual attribute provider. This is to allow upstream code (i.e. the UNICORE/X 
	 * server) to log the error, or to take any other action (like notify an administrator). 
	 * If no attributes are found, an empty map or <code>null</code> must be returned.
	 * 
	 * @param tokens - security tokens for this request
	 * @param otherAuthoriserInfo - attributes returned by other authorisers, which may be <code>null</code>
	 * @return subject's attributes
	 * @throws IOException in case of technical problems
	 */
	public SubjectAttributesHolder getAttributes(final SecurityTokens tokens, 
			SubjectAttributesHolder otherAuthoriserInfo) throws IOException;	 

}
