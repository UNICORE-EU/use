package eu.unicore.services.rest.admin;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Iterables;

import eu.unicore.persist.Persist;
import eu.unicore.services.Kernel;
import eu.unicore.services.admin.AdminAction;
import eu.unicore.services.admin.AdminActionResult;
import eu.unicore.services.persistence.InstanceInfoBean;
import eu.unicore.services.rest.impl.Tokens;
import eu.unicore.util.Log;

/**
 * Allows to revoke API token(s) based on subject or token ID
 *
 * @author schuller
 */
public class RevokeAPITokens implements AdminAction {

	@Override
	public String getDescription() {
		return "parameters: tokenID | subject | subjectsMatch ";
	}

	@Override
	public AdminActionResult invoke(Map<String, String> params, Kernel kernel) {
		try {
			String subject = params.remove("subject");
			String subjectsMatch = params.remove("subjectsMatch");
			String tokenID = params.remove("tokenID");
			Iterable<String> iterable = Arrays.asList(subject, subjectsMatch, tokenID);
			int nullCount = Iterables.frequency(iterable, null);
			if(nullCount==3) {
				throw new IllegalArgumentException("Parameter 'tokenID', 'subject' or 'subjectsMatch' required.");
			}
			if(nullCount<2) {
				throw new IllegalArgumentException("Only one parameter is supported.");
			}
			if(params.size()>0)throw new IllegalArgumentException("Unknown parameter(s): "+params.keySet());
			int tokensRevoked = 0;
			Persist<InstanceInfoBean> store = kernel.getPersistenceManager().getInfoStore();
			if(tokenID!=null) {
				if(store.read(tokenID)!=null) {
					store.remove(tokenID);
					tokensRevoked++;
				}
			}
			else {
				List<String>tokenIDs = store.getIDs("service", Tokens.service);
				if(subjectsMatch!=null) {
					List<String>potentialMatches = store.findIDs("data", subjectsMatch);
					tokenIDs.retainAll(potentialMatches);
					tokensRevoked = tokenIDs.size();
					for(String i: tokenIDs)store.remove(i);
				}
				else for(String id: tokenIDs) {
					var i = store.read(id);
					if(subject.equals(i.about)) {
						store.remove(id);
						tokensRevoked++;
					}
				}
			}
			return new AdminActionResult(true, "OK, <"+tokensRevoked+"> token(s) revoked");
		}catch(Exception e) {
			return new AdminActionResult(false, Log.getDetailMessage(e));
		}
	}

}
