package eu.unicore.services.rest.jwt;

import java.util.Properties;

import eu.unicore.services.rest.security.RESTSecurityProperties;
import eu.unicore.services.rest.security.jwt.JWTProperties;
import eu.unicore.util.configuration.DocumentationReferencePrefix;

public class JWTServerProperties extends JWTProperties {

	@DocumentationReferencePrefix
	public static final String PREFIX = RESTSecurityProperties.PREFIX + "jwt.";

	public JWTServerProperties(Properties properties) {
		super(PREFIX, properties);
	}
	
}
