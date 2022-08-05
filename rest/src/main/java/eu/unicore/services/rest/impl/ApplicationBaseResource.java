package eu.unicore.services.rest.impl;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.unicore.security.Client;
import eu.unicore.security.Queue;
import eu.unicore.security.Role;
import eu.unicore.security.Xlogin;
import eu.unicore.services.ExternalSystemConnector;
import eu.unicore.services.security.util.AuthZAttributeStore;

/**
 * base class for resources providing general information about client&server
 * 
 * @author schuller
 */
public class ApplicationBaseResource extends RESTRendererBase {

	@Override
	protected Map<String, Object> getProperties() throws Exception {
		Map<String,Object>props = new HashMap<String, Object>();
		props.put("client", renderClientProperties());
		props.put("server", renderServerProperties());
		return props;
	}
	
	/**
	 * info about the current client
	 */
	protected Map<String, Object> renderClientProperties() throws Exception {
		Map<String,Object>props = new HashMap<String, Object>();
		Client c = AuthZAttributeStore.getClient();
		props.put("dn", c.getDistinguishedName());
		Xlogin xl = c.getXlogin();
		if(xl!=null){
			Map<String,Object>xlProps = new HashMap<String, Object>();
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
