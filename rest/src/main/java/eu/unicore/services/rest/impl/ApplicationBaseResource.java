package eu.unicore.services.rest.impl;

import java.io.ByteArrayOutputStream;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.logging.log4j.Logger;

import eu.emi.security.authn.x509.X509Credential;
import eu.emi.security.authn.x509.impl.CertificateUtils;
import eu.emi.security.authn.x509.impl.CertificateUtils.Encoding;
import eu.unicore.security.AuthenticationException;
import eu.unicore.security.Client;
import eu.unicore.security.Queue;
import eu.unicore.security.Role;
import eu.unicore.security.Xlogin;
import eu.unicore.services.ExternalSystemConnector;
import eu.unicore.services.rest.jwt.JWTServerProperties;
import eu.unicore.services.rest.security.AuthNHandler;
import eu.unicore.services.rest.security.jwt.JWTUtils;
import eu.unicore.services.security.util.AuthZAttributeStore;
import eu.unicore.util.Log;

/**
 * base class for resources providing general information about client&server
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
			Map<String,String> claims = new HashMap<>();
			claims.put("etd", "true");
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
			String pem = "n/a";
			if(kernel.getContainerSecurityConfiguration().getCredential()!=null) {
				ByteArrayOutputStream os = new ByteArrayOutputStream();
				CertificateUtils.saveCertificate(os, 
						kernel.getContainerSecurityConfiguration().getCredential().getCertificate(), 
						Encoding.PEM);
				pem = os.toString("UTF-8");
			}
			return Response.ok().entity(pem).build();
		}
		catch(Exception ex) {
			return handleError("", ex, logger);
		}
	}

	@Override
	protected Map<String, Object> getProperties() throws Exception {
		Map<String,Object>props = new HashMap<>();
		props.put("client", renderClientProperties());
		props.put("server", renderServerProperties());
		return props;
	}
	
	/**
	 * info about the current client
	 */
	protected Map<String, Object> renderClientProperties() throws Exception {
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
			Map<String,Object>rProps = new HashMap<String, Object>();
			rProps.put("selected",r.getName());
			rProps.put("availableRoles",r.getValidRoles());
			props.put("role",rProps);
		}
		Queue q = c.getQueue();
		if(q!=null && q.getValidQueues()!=null && q.getValidQueues().length>0){
			Map<String,Object>rProps = new HashMap<String, Object>();
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
	

	/**
	 * info about the server
	 */
	protected Map<String, Object> renderServerProperties() throws Exception {
		Map<String,Object>props = new HashMap<>();
		if(kernel.getContainerSecurityConfiguration().getCredential()!=null){
			Map<String,Object>cred = new HashMap<>();
			try{
				X509Certificate cert = kernel.getContainerSecurityConfiguration().getCredential().getCertificate();
				cred.put("dn", cert.getSubjectX500Principal().getName());
				cred.put("issuer", cert.getIssuerX500Principal().getName());
				cred.put("expires", getISODateFormatter().format(cert.getNotAfter()));
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
		
		Map<String,Object>connectors = new HashMap<String, Object>();  
		for(ExternalSystemConnector ec: kernel.getExternalSystemConnectors()){
			connectors.put(ec.getExternalSystemName(), ec.getConnectionStatus());
		}
		props.put("externalConnections", connectors);
		try {
			String v = getClass().getPackage().getSpecificationVersion();
			props.put("version", v!=null? v : "DEVELOPMENT");
		}catch(Exception ex){}
		try {
			props.put("upSince", getISODateFormatter().format(kernel.getUpSince().getTime()));
		}catch(Exception ex){}
		
		return props;
	}
	
}
