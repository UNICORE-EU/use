package eu.unicore.services.security.pdp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import eu.unicore.security.Client;
import eu.unicore.security.OperationType;
import eu.unicore.services.security.pdp.PDPResult.Decision;
import eu.unicore.services.security.util.ResourceDescriptor;
import eu.unicore.util.Log;

/**
 * a PDP based on hard-coded rules and per-service rules that are registered
 * by the service deployment / feature mechanism
 */
public class DefaultPDP implements UnicoreXPDP {

	private static final Logger logger = Log.getLogger(Log.SECURITY, DefaultPDP.class);

	private final List<Rule> basicRules = new ArrayList<>();

	private final Map<String, List<Rule>> perServiceRules = new HashMap<>();

	public DefaultPDP() {
		configureBasicRules();
	}

	private void configureBasicRules() {
		basicRules.add(DENY_BANNED);
		basicRules.add(PERMIT_ADMIN);
		basicRules.add(PERMIT_OWNER);
		basicRules.add(PERMIT_BY_ACL);
		basicRules.add(DENY_MODIFICATION);
	}

	public void setServiceRules(String serviceName, List<Rule> rule) {
		perServiceRules.put(serviceName, rule);
	}

	@Override
	public PDPResult checkAuthorisation(Client c, ActionDescriptor a, ResourceDescriptor d) throws Exception
	{
		logger.debug("Policy check for client={} action={} resource={}", c, a, d);
		for(Rule r: basicRules) {
			Decision decision = r.apply(c, a, d);
			if(Decision.UNCLEAR!=decision) {
				return new PDPResult(decision, "");
			}
		}
		String serviceName = d.getServiceName();
		if(serviceName!=null) {
			List<Rule> rules = perServiceRules.get(serviceName);
			if(rules!=null && rules.size()>0) {
				for(Rule r: rules) {
					Decision decision = r.apply(c, a, d);
					if(Decision.UNCLEAR!=decision) {
						logger.debug("Access {} by rule for '{}'", decision, serviceName);
						return new PDPResult(decision, "");
					}
				}
			}
		}
		logger.debug("No rule match, returning with final DENY");
		return new PDPResult(Decision.DENY, "Access denied.");
	}

	public static interface Rule {

		public Decision apply(Client c, ActionDescriptor action, ResourceDescriptor d);

	}

	// some standard rules

	/**
	 * permit role "admin"
	 */
	public static Rule PERMIT_ADMIN = (c,a,d)-> {
		if(c!=null && c.getRole()!=null && "admin".equals(c.getRole().getName())) {
			logger.debug("PERMIT role 'admin'");
			return Decision.PERMIT;
		}
		return Decision.UNCLEAR;
	};

	/**
	 * deny role "banned"
	 */
	public static Rule DENY_BANNED = (c,a,d) -> {
		if(c!=null && c.getRole()!=null && "banned".equals(c.getRole().getName())) {
			logger.debug("DENY role 'banned'");
			return Decision.DENY;
		}
		return Decision.UNCLEAR;
	};

	/**
	 * allow if ACL check has passed
	 */
	public static Rule PERMIT_BY_ACL = (c, a, d) -> {
		if(d!=null && d.isAclCheckOK()) {
			logger.debug("PERMIT by ACL");
			return Decision.PERMIT;
		}
		return Decision.UNCLEAR;
	};

	/**
	 * allow for service owner
	 */
	public static Rule PERMIT_OWNER = (c,a,d) -> {
		if(c!=null && d!=null && c.getDistinguishedName()!=null
				&& c.getDistinguishedName().equals(d.getOwner())) 
		{
			logger.debug("PERMIT for owner");
			return Decision.PERMIT;
		}
		return Decision.UNCLEAR;
	};

	/**
	 * forbid delete and modify
	 */
	public static Rule DENY_MODIFICATION = (c,a,d) -> {
		if(a!=null) 
		{
			if(OperationType.read!=a.getActionType()) {
				logger.debug("DENY modification");
				return Decision.DENY;
			}
		}
		return Decision.UNCLEAR;
	};

	/**
	 * generic read access - useful for public endpoints
	 */
	public static Rule PERMIT_READ = (c,a,d)-> {
		if(OperationType.read==a.getActionType()) {
			logger.debug("PERMIT read access");
			return Decision.PERMIT;
		}
		return Decision.UNCLEAR;
	};

	/**
	 * permit role "user"
	 */
	public static Rule PERMIT_USER = (c,a,d)-> {
		if(c!=null && c.getRole()!=null && "user".equals(c.getRole().getName())) {
			logger.debug("PERMIT role 'user'");
			return Decision.PERMIT;
		}
		return Decision.UNCLEAR;
	};
}