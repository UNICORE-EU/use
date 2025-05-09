package eu.unicore.services.aip.xuudb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hc.core5.net.URIBuilder;
import org.json.JSONException;
import org.json.JSONObject;

import eu.unicore.security.SecurityTokens;
import eu.unicore.security.SubjectAttributesHolder;
import eu.unicore.services.exceptions.SubsystemUnavailableException;
import eu.unicore.services.security.IAttributeSource;
import eu.unicore.util.Log;

/**
 * get user attributes from an XUUDB's REST/JSON interface
 * 
 * @author schuller
 * @author golbi
 */
public class XUUDBJSONAttributeSource extends XUUDBBase<String> implements
		IAttributeSource {

	private String infoURL;

	@Override
	protected String createEndpoint() {
		infoURL = host + ":" + port + "/rest/xuudb/info";
		xuudbURL = host + ":" + port + "/rest/xuudb/query/"+gcID;
		return xuudbURL;
	}

	@Override
	public SubjectAttributesHolder getAttributes(SecurityTokens tokens,
			SubjectAttributesHolder otherAuthoriserInfo) throws IOException {
		if (!isEnabled) {
			throw new SubsystemUnavailableException("Attribute source "+name+" is disabled");
		}
		if(!cb.isOK()) {
			throw new SubsystemUnavailableException("Attribute source "+name+" unavailable");
		}
		SubjectAttributesHolder map = cacheCredentials ?
				cache.read(tokens.getEffectiveUserName()) : null;
		if (map == null) {
			map = checkDN(tokens);
			if (cacheCredentials) {
				cache.put(tokens.getEffectiveUserName(), map);
			}
		}
		return map;
	}

	/**
	 * retrieves user attributes using the users's DN
	 * 
	 * @param tokens
	 * @return SubjectAttributesHolder
	 */
	private SubjectAttributesHolder checkDN(final SecurityTokens tokens)
			throws IOException {
		String dn = tokens.getEffectiveUserName();
		addAccessorName(dn);
		try {
			URIBuilder ub = new URIBuilder(getXUUDBUrl());
			ub.addParameter("dn", dn);
			String url = ub.build().toString();
			return makeAuthInfo(new JSONObject(doGet(url)));
		} catch (Exception e) {
			cb.notOK();
			Log.logException("Error contacting " + name,e, logger);
			throw new IOException("Error contacting "+name, e);
		}
	}

	/**
	 * parse reply from uudb and return a map of auth info
	 */
	public SubjectAttributesHolder makeAuthInfo(JSONObject login) {
		String role = login.getString("role");
		String xlogin = login.getString("xlogin");
		String groups = login.getString("projects");
		Map<String, String[]> map = new HashMap<>();
		Map<String, String[]> mapDef = new HashMap<>();
		if (isNotEmpty(xlogin)) {
			String[] xlogins = decode(xlogin);
			map.put(IAttributeSource.ATTRIBUTE_XLOGIN, xlogins);
			if (xlogins.length > 0)
				mapDef.put(IAttributeSource.ATTRIBUTE_XLOGIN,
						new String[] { xlogins[0] });
		}
		if (isNotEmpty(role)) {
			String[] roles = decode(role);
			map.put(IAttributeSource.ATTRIBUTE_ROLE, roles);
			mapDef.put(IAttributeSource.ATTRIBUTE_ROLE,
					new String[] { roles[0] });
		}
		if (isNotEmpty(groups)) {
			String[] gids = decode(groups);
			map.put(IAttributeSource.ATTRIBUTE_GROUP, gids);
			mapDef.put(IAttributeSource.ATTRIBUTE_GROUP,
					new String[] { gids[0] });
		}
		return new SubjectAttributesHolder(mapDef, map);
	}

	private String[] decode(String in) {
		List<String> res = new ArrayList<>();
		for (String s : in.split(":")) {
			String v = s.trim();
			if (v.length() != 0)
				res.add(v);
		}
		return res.toArray(new String[res.size()]);
	}

	@Override
	protected String checkXUUDBAlive() {
		try {
			JSONObject info = new JSONObject(doGet(infoURL));
			return "connected to XUUDB v"+info.optString("version", "")+
					" "+xuudbURL;
		}catch(IOException e) {
			return null;
		}
		catch(JSONException je) {
			return "connected to "+xuudbURL;
		}
	}
}
