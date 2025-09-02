package eu.unicore.services.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.unicore.services.Model;
import eu.unicore.services.security.ACLEntry;

public class SecuredResourceModel implements Model {

	private static final long serialVersionUID = 1L;

	private String uniqueID;

	private String ownerDN;

	private List<ACLEntry> acl = new ArrayList<>();

	private Boolean publishServerCert = Boolean.FALSE;

	protected transient boolean dirty=false;

	// this map holds children keyed by service name
	private Map<String, List<String>> children = new HashMap<>();

	private String parentServiceName;

	private String parentUID;

	private Set<String> tags = new HashSet<>();

	@Override
	public String getUniqueID() {
		return uniqueID;
	}

	@Override
	public void setUniqueID(String id) {
		this.uniqueID=id;
	}

	@Override
	public String getParentUID() {
		return parentUID;
	}

	public void setParentUID(String parentUID) {
		this.parentUID = parentUID;
	}

	@Override
	public String getParentServiceName() {
		return parentServiceName;
	}

	public void setParentServiceName(String parentServiceName) {
		this.parentServiceName = parentServiceName;
	}

	@Override
	public Map<String, List<String>> getChildren() {
		return children;
	}

	@Override
	public void addChild(String serviceName, String uid) {
		List<String>cs=children.get(serviceName);
		if(cs==null)cs=new ArrayList<String>();
		cs.add(uid);
		children.put(serviceName, cs);
	}

	@Override
	public void removeAllChildren() {
		children.clear();
	}

	@Override
	public boolean removeChild(String uid) {
		for(Map.Entry<String,List<String>> e: children.entrySet()){
			Iterator<String>iter=e.getValue().iterator();
			while(iter.hasNext()){
				String i = iter.next();
				if(uid.equals(i)){
					iter.remove();
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public Collection<String> removeChildren(Collection<String>uids) {
		Collection<String>res = new HashSet<String>();
		for(Map.Entry<String,List<String>> e: children.entrySet()){
			Iterator<String>iter = e.getValue().iterator();
			while(iter.hasNext()){
				String id = iter.next();
				if(uids.contains(id)){
					iter.remove();
					res.add(id);
				}
			}
		}
		return res;
	}

	@Override
	public List<String> getChildren(String serviceName) {
		List<String>res = children.get(serviceName);
		if(res==null){
			res = new ArrayList<String>();
			children.put(serviceName, res);
		}
		return res;
	}

	public String getOwnerDN() {
		return ownerDN;
	}

	public void setOwnerDN(String ownerDN) {
		this.ownerDN = ownerDN;
	}

	public Boolean getPublishServerCert() {
		return publishServerCert;
	}

	public void setPublishServerCert(Boolean publishServerCert) {
		this.publishServerCert = publishServerCert;
	}

	public List<ACLEntry> getAcl() {
		return acl;
	}

	public void setAcl(List<ACLEntry> acl) {
		this.acl = acl;
	}

	public Set<String>getTags(){
		return tags;
	}

}
