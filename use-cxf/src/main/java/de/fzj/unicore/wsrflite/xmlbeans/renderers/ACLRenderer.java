package de.fzj.unicore.wsrflite.xmlbeans.renderers;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.logging.log4j.Logger;
import org.unigrids.services.atomic.types.PermitDocument;
import org.unigrids.services.atomic.types.ShareDocument;

import de.fzj.unicore.wsrflite.Home;
import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.exceptions.InvalidModificationException;
import de.fzj.unicore.wsrflite.impl.SecuredResourceImpl;
import de.fzj.unicore.wsrflite.security.ACLEntry;
import de.fzj.unicore.wsrflite.security.ACLEntry.MatchType;
import de.fzj.unicore.wsrflite.xmlbeans.AbstractXmlRenderer;
import de.fzj.unicore.wsrflite.xmlbeans.Modifiable;
import eu.unicore.security.AuthorisationException;
import eu.unicore.security.OperationType;
import eu.unicore.services.ws.impl.WSResourceImpl;
import eu.unicore.util.Log;

/**
 * WSRF rendering of resource ACL (and modification logic)
 * 
 * @author schuller
 */
public class ACLRenderer extends AbstractXmlRenderer implements Modifiable<ShareDocument> {

	private static final Logger logger = Log.getLogger(Log.SERVICES, ACLRenderer.class);

	public static final QName QNAME = ShareDocument.type.getDocumentElementName();

	protected final SecuredResourceImpl parent;

	public ACLRenderer(SecuredResourceImpl parent) {
		super(QNAME);
		this.parent=parent;
	}

	public ShareDocument[] render()throws Exception{
		ShareDocument d = ShareDocument.Factory.newInstance();
		d.addNewShare().setPermitArray(renderShare());
		return new ShareDocument[]{d};
	}
	
	@Override
	public int getNumberOfElements(){
		return parent.getModel().getAcl().size();
	}

	// appends all entries from the incoming doc
	@Override
	public void insert(ShareDocument o) throws InvalidModificationException {
		List<ACLEntry> updates = convert(o.getShare().getPermitArray());
		List<ACLEntry> acl = parent.getModel().getAcl();
		acl.addAll(updates);
	}

	@Override
	public synchronized void update(List<ShareDocument> o)throws InvalidModificationException {
		if(o.size()!=1)throw new InvalidModificationException("Cardinality must be == 1");
		List<ACLEntry> updates = convert(o.get(0).getShare().getPermitArray());
		updateInternal(updates);
	}

	protected void updateInternal(List<ACLEntry> updates)throws InvalidModificationException {
		if(!parent.isOwnerLevelAccess()){
			throw new AuthorisationException("Access denied! You have to be the resource's owner or server admin to modify the ACL.");
		}
		List<ACLEntry> acl = parent.getModel().getAcl();
		acl.clear();
		acl.addAll(updates);
	}
	
	@Override
	public void delete()throws InvalidModificationException{
		update(new ArrayList<ShareDocument>());
		updateChildren(new ArrayList<ACLEntry>());
	}

	protected List<ACLEntry> convert(PermitDocument.Permit[] xmlACL) throws InvalidModificationException {
		List<ACLEntry> ret = new ArrayList<ACLEntry>();
		for(PermitDocument.Permit a : xmlACL){
			try{
				ret.add(convert(a));
			}catch(Exception e){
				throw new InvalidModificationException(e);
			}
		}
		return ret;
	}
	
	protected ACLEntry convert(PermitDocument.Permit a){
		OperationType grant = OperationType.valueOf(a.getAllow());
		MatchType ofType = MatchType.valueOf(a.getWhen());
		return new ACLEntry(grant, a.getIs(), ofType);
	}
	
	private PermitDocument.Permit[] renderShare(){
		List<ACLEntry>acl = parent.getModel().getAcl();
		PermitDocument.Permit[]res = new PermitDocument.Permit[acl.size()];
		int i = 0;
		for(ACLEntry e: acl){
			res[i] = PermitDocument.Permit.Factory.newInstance();
			res[i].setAllow(String.valueOf(e.getAccessType()));
			res[i].setWhen(String.valueOf(e.getMatchType()));
			res[i].setIs(e.getRequiredValue());
			i++;
		}
		return res;
	}
	
	/**
	 * Performs recursive modification on all child resources, finally updating also
	 * this one. Before the update the resource logic is activated. In case of problems 
	 * the method tries to rollback all the changes. 
	 */
	public void updateChildren(List<ACLEntry> updates) throws InvalidModificationException {
		Map<String,List<String>> children = parent.getModel().getChildren();
		Kernel kernel=parent.getKernel();
		StringBuilder failed = new StringBuilder();
		if (children != null)
			for (Map.Entry<String,List<String>> child: children.entrySet()) {
				String serviceName=child.getKey();
				Home home=kernel.getHome(serviceName);
				if(home==null){
					continue;
				}
				for(String childUID: child.getValue()){
					WSResourceImpl securedChild = null;
					try{
						try {
							securedChild = (WSResourceImpl)home.getForUpdate(childUID);
							ACLRenderer rp = (ACLRenderer)securedChild.getRenderer(QNAME);
							if (rp == null)
								continue;
							rp.updateInternal(updates);
						}
						finally{
							if(securedChild!=null){
								home.persist(securedChild);
							}
						}
					} catch (Exception e) {
						logger.warn("ACL update was not successful on " + 
								serviceName + "-" + childUID
								+ ". Reason: " + e);
						failed.append(serviceName + "-" + childUID + 
								": " + e.toString() + "\n");
					}
				}
			}
		if (failed.length() > 0)
			throw new InvalidModificationException("The VO membership change was SUCCESSFUL for this resource, " +
					" but some of the child resources failed to update their mebership. The problematic resources are:\n" 
					+ failed);
	}

	@Override
	public boolean checkPermissions(int permissions) {
		return true;
	}

	@Override
	public void updateDigest(MessageDigest md) throws Exception {
		md.update(String.valueOf(parent.getModel().getAcl()).getBytes());
	}
	
}
