package eu.unicore.services.security;

import eu.emi.security.authn.x509.impl.X500NameUtils;
import eu.unicore.security.Client;
import eu.unicore.security.OperationType;

/**
 * ACL entries allows access to a resource if conditions are met.
 * The possible conditions are very simple. Access is granted if either:
 * <ul>
 *   <li>the requesting user's DN matches (TODO should we support DN patterns??)</li>
 *   <li>the requesting user's VO</li>	
 *   <li>the requesting user's Unix group matches</li>
 *   <li>the requesting user's Unix login (uid) matches</li>
 *   <li>the requesting user's role matches</li>
 * </ul>
 * Only one condition can be selected per ACL entry
 * @author schuller
 */
public class ACLEntry {

	public static enum MatchType {
		DN, VO, GROUP, UID, ROLE;
	}

	// the operation type allowed by this entry
	private final OperationType accessType;

	// the required value
	private final String requiredValue;

	// the ACL entry type
	private final MatchType matchType;

	public ACLEntry(OperationType grant, String clientAttribute, MatchType ofType){
		if(grant==null||clientAttribute==null||ofType==null){
			throw new IllegalArgumentException("Parameter(s) cannot be null");
		}
		this.accessType = grant;
		this.requiredValue = clientAttribute;
		this.matchType = ofType;
	}

	public boolean allowed(OperationType type, Client c){
		boolean allow = false;

		// WARNING this assumes that operation types are ordered 
		// by access level, i.e. "modify" access > "read" access
		if(accessType.ordinal()>=type.ordinal()){
			String clientAttribute;
			switch(matchType){
			case DN:
				clientAttribute = c.getDistinguishedName();
				allow = X500NameUtils.equal(clientAttribute, requiredValue);
				break;
			case VO:
				clientAttribute = c.getVo();
				allow = clientAttribute!=null && clientAttribute.startsWith(requiredValue);
				break;
			case GROUP:
				if(c.getXlogin().getGroups()!=null){
					for(String g: c.getXlogin().getGroups()){
						if(requiredValue.equals(g)){
							allow = true;
							break;
						}
					}
				}
				break;
			case UID:
				if(c.getXlogin().getLogins()!=null){
					for(String g: c.getXlogin().getLogins()){
						if(requiredValue.equals(g)){
							allow = true;
							break;
						}
					}
				}
				break;
			case ROLE:
				allow = requiredValue.equals(c.getRole().getName());
				break;
			}	
		}
		return allow;
	}

	public OperationType getAccessType() {
		return accessType;
	}

	public String getRequiredValue() {
		return requiredValue;
	}

	public MatchType getMatchType() {
		return matchType;
	}

	public String toString(){
		return "ACLEntry['"+accessType+"' if "+matchType+" matches " + requiredValue+"]";
	}

	/**
	 * parse the given string which is expected to be in the form 
	 * "accessType:matchType:requiredValue" 
	 * @param acl - string representation of an ACLEntry
	 */
	public static ACLEntry parse(String acl){
		try{
			String[]tokens = acl.split(":", 3);
			return new ACLEntry(OperationType.valueOf(tokens[0]), tokens[2], MatchType.valueOf(tokens[1]));
		}catch(Exception ex){
			throw new IllegalArgumentException("Wrong format, expecting 'accessType:matchType:requiredValue'", ex);
		}
	}
}
