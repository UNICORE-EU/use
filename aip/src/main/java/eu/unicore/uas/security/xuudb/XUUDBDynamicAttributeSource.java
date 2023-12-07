package eu.unicore.uas.security.xuudb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import de.fzJuelich.unicore.xuudb.GetAttributesRequestDocument;
import de.fzJuelich.unicore.xuudb.GetAttributesRequestType;
import de.fzJuelich.unicore.xuudb.GetAttributesResponseDocument;
import de.fzJuelich.unicore.xuudb.GetAttributesResponseType;
import de.fzJuelich.unicore.xuudb.SimplifiedAttributeType;
import de.fzj.unicore.xuudb.interfaces.IDynamicAttributesPublic;
import eu.unicore.security.Client;
import eu.unicore.security.SubjectAttributesHolder;
import eu.unicore.security.XACMLAttribute;
import eu.unicore.security.wsutil.client.WSClientFactory;
import eu.unicore.services.exceptions.SubsystemUnavailableException;
import eu.unicore.services.security.IAttributeSource;
import eu.unicore.services.security.IDynamicAttributeSource;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.IClientConfiguration;

public class XUUDBDynamicAttributeSource extends XUUDBBase<IDynamicAttributesPublic> implements
		IDynamicAttributeSource {

	@Override
	public String getName() { return "Dynamic attributes"; }	

	@Override
	protected IDynamicAttributesPublic createEndpoint() {
		try {
			IClientConfiguration sec = kernel.getClientConfiguration();
			return new WSClientFactory(sec).createPlainWSProxy(
					IDynamicAttributesPublic.class, getXUUDBUrl()
							+ IDynamicAttributesPublic.SERVICE_NAME);
		} catch (Exception e) {
			Log.logException("Can't make connection to " + name,
					e, logger);
			return null;
		}
	}

	private String getCacheKey(Client cl) {
		StringBuilder cInfo = new StringBuilder();
		if (isNotEmpty(cl.getDistinguishedName())) {
			cInfo.append("Name: ");
			cInfo.append(cl.getDistinguishedName());
		}
		if (cl.getXlogin() != null) {
			String xlogin = cl.getXlogin().getUserName();
			String group = cl.getXlogin().getGroup();
			if (isNotEmpty(xlogin)) {
				cInfo.append("|Xlogin: ");
				cInfo.append(xlogin);
			}
			if (isNotEmpty(group)) {
				cInfo.append("|Group: ");
				cInfo.append(group);

			}

		}
		if (cl.getRole() != null) {
			String role = cl.getRole().getName();
			if (isNotEmpty(role)) {
				cInfo.append("|Role: ");
				cInfo.append(role);
			}
		}
		if (cl.getQueue() != null)
			if (cl.getQueue().getValidQueues().length > 0) {
				cInfo.append("|Queues: ");
				cInfo.append(cl.getQueue());
			}
		if (cl.getVos().length > 0) {
			cInfo.append("|VOs: ");
			cInfo.append(Arrays.toString(cl.getVos()));
		}
		if (isNotEmpty(cl.getVo())) {
			cInfo.append("|Selected VO: ").append(cl.getVo());
		}

		for (XACMLAttribute a : cl.getSubjectAttributes().getXacmlAttributes()) {

			cInfo.append("|" + a.getName() + "=" + a.getValue());

		}

		return cInfo.toString();
	}

	@Override
	public SubjectAttributesHolder getAttributes(Client client,
			SubjectAttributesHolder otherAuthoriserInfo) throws IOException {
		if (!isEnabled)
			throw new SubsystemUnavailableException("The XUUDB attribute source is disabled");

		if(!cb.isOK())
			throw new SubsystemUnavailableException("Attribute source "+name+" is unavailable");
		
		SubjectAttributesHolder map;

		map = cacheCredentials ? cache.read(getCacheKey(client)) : null;
		if (map == null) {
			map = getDAPAttributes(client, otherAuthoriserInfo);
			if (map != null) {
				if (cacheCredentials) {
					cache.put(getCacheKey(client), map);
				}
			}
		}
		return map;
	}

	private SubjectAttributesHolder getDAPAttributes(Client client,
			SubjectAttributesHolder otherAuthoriserInfo) throws IOException {

		GetAttributesResponseDocument resp;
		try {
			GetAttributesRequestType req = GetAttributesRequestType.Factory
					.newInstance();
			if (isNotEmpty(client.getDistinguishedName())) {
				req.setUserDN(client.getDistinguishedName());
			}
			if (client.getRole() != null) {
				String role = client.getRole().getName();
				if (isNotEmpty(role)) {
					req.setRole(role);
				}
			}
			
			if (isNotEmpty(client.getVo())) {
				req.setVo(client.getVo());
			}

			if (client.getXlogin() != null) {
				String xlogin = client.getXlogin().getUserName();
				String group = client.getXlogin().getGroup();
				if (isNotEmpty(xlogin)) {
					req.setXlogin(xlogin);
				}
				if (isNotEmpty(group)) {
					req.setGid(group);
				}
			}
		
			String [] uids = client.getSubjectAttributes().getDefaultIncarnationAttributes().get(IAttributeSource.ATTRIBUTE_XLOGIN);
			if(uids!=null && uids.length>0 && isNotEmpty(uids[0])) {
				req.setXlogin(uids[0]);
			}
			String [] gids = client.getSubjectAttributes().getDefaultIncarnationAttributes().get(IAttributeSource.ATTRIBUTE_GROUP);
			if(gids!=null && gids.length>0 && isNotEmpty(gids[0])) {
				req.setGid(gids[0]);
			}
			
			// Extra Attr
			ArrayList<SimplifiedAttributeType> al = new ArrayList<>();
			for (XACMLAttribute a : client.getSubjectAttributes()
					.getXacmlAttributes()) {
				SimplifiedAttributeType ex = SimplifiedAttributeType.Factory
						.newInstance();
				ex.setName(a.getName());
				ex.addNewValue().setStringValue(a.getValue());
				al.add(ex);

			}
			if (al.size() > 0) {
				SimplifiedAttributeType[] ar = new SimplifiedAttributeType[al.size()];
				al.toArray(ar);
				req.setExtraAttributesArray(ar);
			}

			GetAttributesRequestDocument outXml = GetAttributesRequestDocument.Factory
					.newInstance();
			outXml.setGetAttributesRequest(req);

			if (logger.isDebugEnabled()) {
				logger.debug(name + " request: " + outXml.toString());
			}
			synchronized (xuudb) {
				resp = xuudb.getAttributes(outXml);
			}
		} catch (Exception e) {
			cb.notOK();
			throw new IOException("Error contacting "
					+name + ": " + e.getMessage(),e);
		}

		if (logger.isDebugEnabled()) {
			GetAttributesResponseType data = resp.getGetAttributesResponse();
			String reply = "[xlogin=" + data.getXlogin() + ", gid="
					+ data.getGid() + ", SupplementaryGids=";
			boolean sgt = false;
			for (String sg : data.getSupplementaryGidsArray()) {
				reply = reply + sg + ":";
				sgt = true;
			}
			if (sgt)
				reply = reply.substring(0, reply.length() - 1);
			reply = reply + "]";
			logger.debug(name + " reply: " + reply);
		}
		return makeAuthInfo(resp.getGetAttributesResponse());
	}

	private SubjectAttributesHolder makeAuthInfo(GetAttributesResponseType data) {
		String gid = data.getGid();
		String xlogin = data.getXlogin();
		String[] supGids = data.getSupplementaryGidsArray();

		Map<String, String[]> map = new HashMap<>();
		Map<String, String[]> mapDef = new HashMap<>();
		if (isNotEmpty(xlogin)) {
			String[] xlogins = new String[] { xlogin };
			map.put(IAttributeSource.ATTRIBUTE_XLOGIN, xlogins);
			mapDef.put(IAttributeSource.ATTRIBUTE_XLOGIN, xlogins);
		}
		if (isNotEmpty(gid)) {
			String[] gids = new String[] { gid };
			map.put(IAttributeSource.ATTRIBUTE_GROUP, gids);
			mapDef.put(IAttributeSource.ATTRIBUTE_GROUP, gids);
		}
		if (supGids != null && supGids.length > 0) {
			map.put(IAttributeSource.ATTRIBUTE_SUPPLEMENTARY_GROUPS, supGids);
			mapDef
					.put(IAttributeSource.ATTRIBUTE_SUPPLEMENTARY_GROUPS,
							supGids);
		}
		return new SubjectAttributesHolder(mapDef, map);
	}

}
