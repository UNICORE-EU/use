package eu.unicore.services.rest.impl;

import java.io.ByteArrayOutputStream;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hc.core5.http.HttpStatus;
import org.apache.logging.log4j.Logger;

import eu.emi.security.authn.x509.X509Credential;
import eu.emi.security.authn.x509.impl.CertificateUtils;
import eu.emi.security.authn.x509.impl.CertificateUtils.Encoding;
import eu.unicore.security.AuthenticationException;
import eu.unicore.security.Client;
import eu.unicore.security.Queue;
import eu.unicore.security.Role;
import eu.unicore.security.SecurityTokens;
import eu.unicore.security.Xlogin;
import eu.unicore.services.ContainerProperties;
import eu.unicore.services.ExternalSystemConnector;
import eu.unicore.services.ISubSystem;
import eu.unicore.services.Kernel;
import eu.unicore.services.rest.jwt.JWTServerProperties;
import eu.unicore.services.rest.security.AuthNHandler;
import eu.unicore.services.restclient.jwt.JWTUtils;
import eu.unicore.services.restclient.utils.UnitParser;
import eu.unicore.services.security.AuthAttributesCollector;
import eu.unicore.services.security.AuthAttributesCollector.BasicAttributeHolder;
import eu.unicore.services.security.util.AuthZAttributeStore;
import eu.unicore.util.Log;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * base class for resources providing general information
 * about client and server
 *
 * @author schuller
 */
public class ApplicationBaseResource extends RESTRendererBase {

	private static final Logger logger = Log.getLogger(Log.SERVICES, ApplicationBaseResource.class);

	@GET
	@Path("/token")
	@Produces(MediaType.TEXT_PLAIN)
	public Response getToken(@QueryParam("lifetime")String lifetimeParam,
			@QueryParam("renewable")String renewable,
			@QueryParam("limited")String limited)
			throws Exception {
		try {
			String method = (String)AuthZAttributeStore.getTokens().getContext().get(AuthNHandler.USER_AUTHN_METHOD);
			if("ETD".equals(method)) {
				if(!(Boolean)AuthZAttributeStore.getTokens().getContext().get(AuthNHandler.ETD_RENEWABLE)) {
					throw new AuthenticationException("Cannot create token when authenticating with a non-renewable token!");
				}
			}
			JWTServerProperties jwtProps = new JWTServerProperties(kernel.getContainerProperties().getRawProperties());
			String user = AuthZAttributeStore.getClient().getDistinguishedName();
			X509Credential issuerCred =  kernel.getContainerSecurityConfiguration().getCredential();
			long lifetime = lifetimeParam!=null? Long.valueOf(lifetimeParam): jwtProps.getTokenValidity();
			// make sure it's not longer than the remaining credential lifetime
			Date notAfter = issuerCred.getCertificate().getNotAfter();
			long remainingCredentialLifetime = Math.max(0, notAfter.getTime() - System.currentTimeMillis());
			// if user requested a longer lifetime than is possible, we should fault
			if(lifetimeParam!=null && lifetime>remainingCredentialLifetime) {
				return createErrorResponse(HttpStatus.SC_BAD_REQUEST,
						"Requested token lifetime is longer than the remaining server certificate validity.");
			}
			lifetime = Math.min(lifetime, remainingCredentialLifetime);
			Map<String,String> claims = new HashMap<>();
			claims.put("etd", "true");
			try {
				SecurityTokens tokens = AuthZAttributeStore.getTokens();
				BasicAttributeHolder attr = (BasicAttributeHolder)tokens.getContext().get(AuthAttributesCollector.ATTRIBUTES);
				if(attr.uid!=null) {
					claims.put("uid", attr.uid);
				}
			}catch(Exception e) {}
			if(Boolean.parseBoolean(renewable)) {
				claims.put("renewable", "true");
			}
			if(Boolean.parseBoolean(limited)) {
				claims.put("aud", issuerCred.getSubjectName());
			}
			String token = JWTUtils.createJWTToken(user, lifetime,
					issuerCred.getSubjectName(), issuerCred.getKey(),
					claims);
			return Response.ok().entity(token).build();
		}
		catch(Exception ex) {
			return handleError("Error creating token", ex, logger);
		}
	}

	@GET
	@Path("/certificate")
	@Produces(MediaType.TEXT_PLAIN)
	public Response getCert() throws Exception {
		try {
			if(kernel.getContainerSecurityConfiguration().getCredential()!=null) {
				ByteArrayOutputStream os = new ByteArrayOutputStream();
				CertificateUtils.saveCertificate(os, 
						kernel.getContainerSecurityConfiguration().getCredential().getCertificate(), 
						Encoding.PEM);
				return Response.ok().entity(os.toString("UTF-8")).build();
			}
			return Response.noContent().build();
		}
		catch(Exception ex) {
			return handleError("", ex, logger);
		}
	}

	@Override
	protected Map<String, Object> getProperties() throws Exception {
		Map<String,Object>props = new HashMap<>();
		if(wantProperty("client"))props.put("client", renderClientProperties());
		if(wantProperty("server"))props.put("server", renderServerProperties());
		return props;
	}

	/**
	 * info about the current client
	 */
	protected Map<String, Object> renderClientProperties() throws Exception {
		return getBaseClientProperties();
	}

	/**
	 * info about the server
	 */
	protected Map<String, Object> renderServerProperties() throws Exception {
		return getBaseServerProperties(kernel);
	}

	public static Map<String, Object> getBaseClientProperties() throws Exception {
		Map<String,Object>props = new HashMap<>();
		Client c = AuthZAttributeStore.getClient();
		props.put("dn", c.getDistinguishedName());
		Xlogin xl = c.getXlogin();
		if(xl!=null){
			Map<String,Object>xlProps = new HashMap<>();
			xlProps.put("UID",xl.getUserName());
			xlProps.put("availableUIDs",xl.getLogins());
			xlProps.put("group",xl.getGroup());
			xlProps.put("availableGroups",xl.getGroups());
			xlProps.put("selectedSupplementaryGroups",xl.getSelectedSupplementaryGroups());
			props.put("xlogin",xlProps);
		}
		Role r = c.getRole();
		if(r!=null){
			Map<String,Object>rProps = new HashMap<>();
			rProps.put("selected",r.getName());
			rProps.put("availableRoles",r.getValidRoles());
			props.put("role",rProps);
		}
		Queue q = c.getQueue();
		if(q!=null && q.getValidQueues()!=null && q.getValidQueues().length>0){
			Map<String,Object>rProps = new HashMap<>();
			rProps.put("selected",q.getSelectedQueue());
			rProps.put("availableQueues",q.getValidQueues());
			props.put("queues",rProps);
		}
		try {
			String method = (String)AuthZAttributeStore.getTokens().getContext()
					.get(AuthNHandler.USER_AUTHN_METHOD);
			if(method!=null) {
				props.put("authenticationMethod", method);
			}
		}catch(Exception e) {}
		return props;
	}
	
	public static Map<String, Object> getBaseServerProperties(Kernel kernel){
		Map<String,Object>props = new HashMap<>();
		DateFormat df = UnitParser.getISO8601();
		try {
			String name = kernel.getContainerProperties().getValue(ContainerProperties.VSITE_NAME_PROPERTY);
			if(name!=null)props.put("siteName", name);
		}catch(Exception ex) {}
		if(kernel.getContainerSecurityConfiguration().getCredential()!=null){
			Map<String,Object>cred = new HashMap<>();
			try{
				X509Certificate cert = kernel.getContainerSecurityConfiguration().getCredential().getCertificate();
				cred.put("dn", cert.getSubjectX500Principal().getName());
				cred.put("issuer", cert.getIssuerX500Principal().getName());
				cred.put("expires", df.format(cert.getNotAfter()));
			}catch(Exception ex) {}
			props.put("credential", cred);
		}
		List<String>trusted = new ArrayList<>();
		try{
			X509Certificate[] trustedCAs = kernel.getContainerSecurityConfiguration().getValidator().getTrustedIssuers();
			for(X509Certificate c: trustedCAs) {
				trusted.add(c.getSubjectX500Principal().getName());
			}
		}catch(Exception ex) {}
		props.put("trustedCAs",trusted);
		List<String>trustedSAML = new ArrayList<>();
		try{
			X509Certificate[] trustedCAs = kernel.getContainerSecurityConfiguration().getTrustedAssertionIssuers().getTrustedIssuers();
			for(X509Certificate c: trustedCAs) {
				trustedSAML.add(c.getSubjectX500Principal().getName());
			}
		}catch(Exception ex) {}
		props.put("trustedSAMLIssuers",trustedSAML);
		Map<String,Object>connectors = new HashMap<>();
		for(ISubSystem sub: kernel.getSubSystems()){
			for(ExternalSystemConnector ec: sub.getExternalConnections()){
				connectors.put(ec.getExternalSystemName(), ec.getConnectionStatus());
			}
		}
		props.put("externalConnections", connectors);
		try {
			String v = ApplicationBaseResource.class.getPackage().getSpecificationVersion();
			props.put("version", v!=null? v : "DEVELOPMENT");
		}catch(Exception ex){}
		try {
			props.put("upSince", df.format(kernel.getUpSince().getTime()));
		}catch(Exception ex){}
		try {
			props.put("currentTime", df.format(new Date()));
		}catch(Exception ex) {}
		return props;
	}
}