package eu.unicore.services.restclient.oidc;

import java.util.Properties;

import org.json.JSONObject;

public class OIDCAgentAuthN extends TokenBasedAuthN {

	OIDCAgentProperties agentProperties; 

	private OIDCAgentProxy ap;

	public void setProperties(Properties p) {
		super.setProperties(p);
		agentProperties = new OIDCAgentProperties(p);
	}

	@Override
	protected void retrieveToken() {
		boolean success = true;
		String error = "";
		try {
			setupOIDCAgent();
			JSONObject request = new JSONObject();
			request.put("request", "access_token");
			request.put("account", agentProperties.getAccount());
			JSONObject reply = new JSONObject(ap.send(request.toString()));
			success = "success".equalsIgnoreCase(reply.getString("status"));
			token = reply.getString("access_token");
			if(!success){
				error = reply.optString("error", reply.toString());
			}
		}catch(Exception ex) {
			throw new RuntimeException("Error accessing oidc-agent", ex);
		}
		if(!success) {
			throw new RuntimeException("Error received from oidc-agent: <"+error+">");
		}
	}

	@Override
	protected void refreshTokenIfNecessary() throws Exception {
		long instant = System.currentTimeMillis() / 1000;
		long interval = agentProperties.getIntValue(OIDCAgentProperties.REFRESH_INTERVAL);
		if(instant < lastRefresh + interval){
			return;
		}
		lastRefresh = instant;
		log.debug("Refreshing token (after <{}> seconds.", interval);
		retrieveToken();
	}

	protected void setupOIDCAgent() {
		if(ap==null) {
			if(!OIDCAgentProxy.isConnectorAvailable())throw new RuntimeException("oidc-agent is not available");
			ap = new OIDCAgentProxy();
		}
	}

	// unit testing
	public void setAgentProxy(OIDCAgentProxy ap) {
		this.ap = ap;
	}
}
