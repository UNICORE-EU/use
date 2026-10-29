package eu.unicore.services.rest.admin;

import java.util.List;
import java.util.Map;

import eu.unicore.persist.Persist;
import eu.unicore.services.Kernel;
import eu.unicore.services.admin.AdminAction;
import eu.unicore.services.admin.AdminActionResult;
import eu.unicore.services.persistence.InstanceInfoBean;
import eu.unicore.services.rest.impl.Tokens;
import eu.unicore.util.Log;

/**
 * Allows to revoken API token(s) based on subject or token ID
 *
 * @author schuller
 */
public class ListAPITokens implements AdminAction {

	@Override
	public String getDescription() {
		return "parameters: [subject | subjectsMatch] ";
	}

	@Override
	public AdminActionResult invoke(Map<String, String> params, Kernel kernel) {
		try {
			String subjectsMatch = params.remove("subjectsMatch");
			if(params.size()>0)throw new IllegalArgumentException("Unknown parameter(s): "+params.keySet());
			Persist<InstanceInfoBean> store = kernel.getPersistenceManager().getInfoStore();
			boolean all = subjectsMatch==null;
			List<String>tokenIDs = store.getIDs("service", Tokens.service);
			if(!all) {
				List<String>potentialMatches = store.findIDs("data", subjectsMatch);
				tokenIDs.retainAll(potentialMatches);
			}
			AdminActionResult res = new AdminActionResult(true, "OK, <"+tokenIDs.size()+"> token(s) found");
			for(String id: tokenIDs) {
				var i = store.read(id);
				res.addResult(i.uniqueID, String.format("user=<%s> expires %s", i.about, i.terminates));
			}
			return res;
		}catch(Exception e) {
			return new AdminActionResult(false, Log.getDetailMessage(e));
		}
	}

}
