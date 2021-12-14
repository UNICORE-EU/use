package eu.unicore.services.rest.jwt;

import java.util.Properties;

import eu.unicore.services.rest.security.RESTSecurityProperties;
import eu.unicore.services.rest.security.jwt.JWTProperties;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertyMD;

public class JWTServerProperties extends JWTProperties {
	
	public static final String TRUSTED_ISSUER_CERT_LOCATIONS = "trustedIssuerCert.";
	
	@DocumentationReferencePrefix
	public static final String PREFIX = RESTSecurityProperties.PREFIX + "jwt.";

	static {
		META.put(TRUSTED_ISSUER_CERT_LOCATIONS, new PropertyMD().setList(false).
				setDescription("List of trusted issuer certificates locations."));
	}

	public JWTServerProperties(Properties properties) {
		super(PREFIX, properties);
	}
}
