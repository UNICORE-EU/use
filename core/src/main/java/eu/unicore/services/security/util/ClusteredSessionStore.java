package eu.unicore.services.security.util;

import de.fzj.unicore.persist.cluster.Cluster;
import eu.unicore.security.wsutil.SecuritySession;
import eu.unicore.security.wsutil.SecuritySessionStore;

public class ClusteredSessionStore extends SecuritySessionStore {

	public ClusteredSessionStore(int maxPerUser, Cluster cluster){
		super(maxPerUser);
		sessions = cluster.getMap("USE.SESSION_STORE", String.class, SecuritySession.class);
	}

	@Override
	public synchronized SecuritySession getSession(String sessionID) {
		SecuritySession s = super.getSession(sessionID);
		if(s!=null){
			s.setLastAccessed(System.currentTimeMillis());
			sessions.put(sessionID, s);
		}
		return s;
	}
	
	
}
