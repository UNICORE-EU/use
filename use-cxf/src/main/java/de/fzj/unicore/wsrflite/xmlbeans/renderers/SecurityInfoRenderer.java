package de.fzj.unicore.wsrflite.xmlbeans.renderers;

import java.io.StringWriter;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;

import org.apache.logging.log4j.Logger;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.unigrids.services.atomic.types.AcceptedCAsType;
import org.unigrids.services.atomic.types.SecurityDocument;
import org.unigrids.services.atomic.types.SecurityType;
import org.unigrids.services.atomic.types.SelectedXGroupType;
import org.unigrids.services.atomic.types.ValidRolesType;
import org.unigrids.services.atomic.types.ValidVOsType;
import org.unigrids.services.atomic.types.ValidXgroupsType;
import org.unigrids.services.atomic.types.ValidXloginsType;

import de.fzj.unicore.wsrflite.impl.SecuredResourceImpl;
import de.fzj.unicore.wsrflite.security.IContainerSecurityConfiguration;
import de.fzj.unicore.wsrflite.security.util.AuthZAttributeStore;
import de.fzj.unicore.wsrflite.xmlbeans.AbstractXmlRenderer;
import eu.unicore.security.Client;
import eu.unicore.security.Xlogin;
import eu.unicore.util.Log;

/**
 * Publishes server security info (server identity, trusted CAs, 
 * trusted VOs) and properties of the current client (available and 
 * chosen xlogins and groups) 
 * 
 * @author schuller
 * @author golbi
 */
public class SecurityInfoRenderer extends AbstractXmlRenderer {

	private static final Logger logger=Log.getLogger(Log.SERVICES, SecurityInfoRenderer.class);

	protected final SecuredResourceImpl parent;
	
	/**
	 * create a new security info renderer
	 * @param parent - the parent resource
	 */
	public SecurityInfoRenderer(SecuredResourceImpl parent){
		super(SecurityDocument.type.getDocumentElementName());
		this.parent=parent;
	}

	@Override
	public SecurityDocument[] render() {
		SecurityDocument[] xdoc=new SecurityDocument[1];
		xdoc[0]=SecurityDocument.Factory.newInstance();
		xdoc[0].addNewSecurity();
		doAddInfo(xdoc[0]);
		return xdoc;
	}
	
	protected void doAddInfo(SecurityDocument doc){
		IContainerSecurityConfiguration securityCfg = parent.getKernel().
				getContainerSecurityConfiguration();
		//optionally add server cert in pem format
		boolean addServerCert=parent.getModel().getPublishServerCert();
		if(addServerCert){
			try{
				X509Certificate c=securityCfg.getCredential().getCertificate();
				if(c!=null){
					StringWriter writer=new StringWriter();
					JcaPEMWriter pw=new JcaPEMWriter(writer);
					pw.writeObject(c);
					pw.flush();
					pw.close();
					doc.getSecurity().setServerCertificate(writer.toString());
				}
			}catch(Exception ex){
				Log.logException("Error getting server cert", ex, logger);
			}
		}
		
		//Server identity
		if (securityCfg.getCredential() != null) {
			String serverDN = securityCfg.getCredential().getSubjectName();
			doc.getSecurity().setServerDN(serverDN);
		}
		
		//Trusted CAs
		String[] trustedIssuers = getTrustedIssuers(securityCfg);
		if (trustedIssuers.length>0) {
			AcceptedCAsType acceptedCAs = AcceptedCAsType.Factory.newInstance();
			acceptedCAs.setAcceptedCAArray(trustedIssuers);
			doc.getSecurity().setAcceptedCAs(acceptedCAs);
		}
		
		//valid settings for the actual client
		addValidSettings(doc);
		
		//currently selected values for the actual client
		addSelectedSettings(doc);
		

		if(parent instanceof SecuredResourceImpl){
			try{
				// resource owner
				String owner=((SecuredResourceImpl)parent).getOwner();
				doc.getSecurity().setOwnerDN(owner);
			}catch(Exception ex){}
		}
	}
	
	protected String[] getTrustedIssuers(IContainerSecurityConfiguration securityCfg) {
		if (securityCfg.getValidator() == null)
			return new String[0];
		X509Certificate[] trusted = securityCfg.getValidator().getTrustedIssuers();
		if(trusted!=null && trusted.length>0) {
			String []trustedIssuers = new String[trusted.length];
			for (int i=0; i<trusted.length; i++)
				trustedIssuers[i] = trusted[i].getSubjectX500Principal().getName();
			return trustedIssuers; 
		}
		return new String[0];
	}
	
	
	protected void addValidSettings(SecurityDocument doc) {
		Client client = AuthZAttributeStore.getClient();
		
		String[] vos = client.getVos();
		if (vos != null && vos.length > 0) {
			ValidVOsType clientValidVos = ValidVOsType.Factory.newInstance();
			clientValidVos.setVOArray(vos);
			doc.getSecurity().setClientValidVOs(clientValidVos);
		}
		
		String[] groups = client.getXlogin().getGroups();
		if (groups != null && groups.length > 0) {
			ValidXgroupsType validXgroups = ValidXgroupsType.Factory.newInstance();
			validXgroups.setXgroupArray(groups);
			doc.getSecurity().setClientValidXgroups(validXgroups);
		}
		
		String[] xlogins = client.getXlogin().getLogins();
		if (xlogins != null && xlogins.length > 0) {
			ValidXloginsType validXlogins = ValidXloginsType.Factory.newInstance();
			validXlogins.setXloginArray(xlogins);
			doc.getSecurity().setClientValidXlogins(validXlogins);
		}

		String[] validRoles = client.getRole().getValidRoles();
		if (validRoles != null && validRoles.length > 0) {
			ValidRolesType validRolesXml = ValidRolesType.Factory.newInstance();
			validRolesXml.setRoleArray(validRoles);
			doc.getSecurity().setClientValidRoles(validRolesXml);
		}
	}
	
	protected void addSelectedSettings(SecurityDocument doc) {
		Client client = AuthZAttributeStore.getClient();
		SecurityType sec=doc.getSecurity();
		
		String role = client.getRole().getName();
		sec.setClientSelectedRole(role);
		
		String xlogin = client.getSelectedXloginName();
		if (xlogin != null)
			sec.setClientSelectedXlogin(xlogin);
		
		String vo = client.getVo();
		if (vo != null)
			sec.setClientSelectedVO(vo);
		
		addSelectedGroup(doc);
		
		sec.setClientDN(client.getDistinguishedName());
	}
	
	protected void addSelectedGroup(SecurityDocument doc) {
		Client client = AuthZAttributeStore.getClient();
		Xlogin xloginO = client.getXlogin();
		boolean useOs = xloginO.isAddDefaultGroups();
		SelectedXGroupType selectedXGroup = SelectedXGroupType.Factory.newInstance();
		selectedXGroup.setUseOSDefaults(useOs);
		if (xloginO.isGroupSelected())
			selectedXGroup.setPrimaryGroup(xloginO.getGroup());
		String[] supGroups = xloginO.getSelectedSupplementaryGroups(); 
		if (supGroups != null && supGroups.length > 0) {
			selectedXGroup.setSupplementaryGroupArray(supGroups);
		}
		doc.getSecurity().setClientSelectedXgroup(selectedXGroup);
	}
	
	@Override
	public int getNumberOfElements(){
		return 1;
	}
	
	@Override
	public void updateDigest(MessageDigest md) throws Exception {
		md.update(String.valueOf(render()[0]).getBytes());
	}
}
