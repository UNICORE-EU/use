package eu.unicore.services.security.pdp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import eu.unicore.security.Client;
import eu.unicore.security.OperationType;
import eu.unicore.services.Kernel;
import eu.unicore.services.security.pdp.PDPResult.Decision;
import eu.unicore.services.security.util.ResourceDescriptor;
import eu.unicore.util.Log;

import eu.emi.security.authn.x509.impl.X500NameUtils;

/**
 * a PDP based on hard-coded basic rules and per-service rules that are registered
 * by the service deployment / feature mechanism
 */
public class DefaultPDP implements UnicoreXPDP {

	private static final Logger logger = Log.getLogger(Log.SECURITY+".pdp", DefaultPDP.class);

	private final List<Rule> basicRules = new ArrayList<>();

	private final Map<String, List<Rule>> perServiceRules = new HashMap<>();

	public DefaultPDP() {
		configureBasicRules();
	}

	/**
	 * get the DefaultPDP for the kernel, or null if not available
	 */
	public static DefaultPDP get(Kernel kernel) {
		UnicoreXPDP pdp = kernel.getSecurityManager().getPdp();
		if(pdp!=null && pdp instanceof DefaultPDP) {
			return (DefaultPDP)pdp;
		}
		else return null;
	}

	private void configureBasicRules() {
		basicRules.add(DENY_BANNED);
		basicRules.add(PERMIT_ADMIN);
		basicRules.add(PERMIT_OWNER);
		basicRules.add(PERMIT_BY_ACL);
	}

	public void setServiceRules(String serviceName, List<Rule> rules) {
		perServiceRules.put(serviceName, new ArrayList<>(rules));
	}

	public void setServiceRules(String serviceName, Rule... rules) {
		List<Rule> rs = new ArrayList<>();
		for(Rule r: rules)rs.add(r);
		perServiceRules.put(serviceName, rs);
	}

	@Override
	public PDPResult checkAuthorisation(Client c, ActionDescriptor a, ResourceDescriptor d) throws Exception
	{
		logger.debug("Policy check for client={} action={} resource={}", c, a, d);
		for(Rule r: basicRules) {
			Decision decision = r.apply(c, a, d);
			if(Decision.UNCLEAR!=decision) {
				logger.debug("{} access by built-in rule", decision);
				return new PDPResult(decision, "Decision by built-in rule");
			}
		}
		String serviceName = d.getServiceName();
		if(serviceName!=null) {
			List<Rule> rules = perServiceRules.get(serviceName);
			if(rules!=null && rules.size()>0) {
				for(Rule r: rules) {
					Decision decision = r.apply(c, a, d);
					if(Decision.UNCLEAR!=decision) {
						logger.debug("{} access by rule for service <{}>", decision, serviceName);
						return new PDPResult(decision, "Decision by service rule");
					}
				}
			}
		}
		logger.debug("DENY access - no rule match.");
		return new PDPResult(Decision.DENY, "No rule matched");
	}

	public static interface Rule {

		public Decision apply(Client c, ActionDescriptor action, ResourceDescriptor d);

	}

	// some standard rules

	public static final class PermitByRole implements Rule {
		private final String role;

		public PermitByRole(String role) {
			this.role = role;
		}

		@Override
		public Decision apply(Client c, ActionDescriptor action, ResourceDescriptor d){
			if(c!=null && c.getRole()!=null && role.equals(c.getRole().getName())) {
				logger.debug("PERMIT role '{}'", role);
				return Decision.PERMIT;
			}
			return Decision.UNCLEAR;
		};
	}

	/**
	 * permit role "admin"
	 */
	public static final Rule PERMIT_ADMIN = new PermitByRole("admin");

	/**
	 * permit role "user"
	 */
	public static final Rule PERMIT_USER = new PermitByRole("user");

	/**
	 * deny role "banned"
	 */
	public static final Rule DENY_BANNED = (c,a,d) -> {
		if(c!=null && c.getRole()!=null && "banned".equals(c.getRole().getName())) {
			logger.debug("DENY role 'banned'");
			return Decision.DENY;
		}
		return Decision.UNCLEAR;
	};

	/**
	 * allow if ACL check has passed
	 */
	public static final Rule PERMIT_BY_ACL = (c, a, d) -> {
		if(d!=null && d.isAclCheckOK()) {
			logger.debug("PERMIT by ACL");
			return Decision.PERMIT;
		}
		return Decision.UNCLEAR;
	};

	/**
	 * allow for service owner
	 */
	public static final Rule PERMIT_OWNER = (c,a,d) -> {
		if(c!=null && d!=null && c.getDistinguishedName()!=null
				&& X500NameUtils.equal(c.getDistinguishedName(), d.getOwner()))
		{
			logger.debug("PERMIT for owner");
			return Decision.PERMIT;
		}
		return Decision.UNCLEAR;
	};

	private static final String[] mod = new String[]{"DELETE", "PUT"};

	/**
	 * forbid delete and modify
	 */
	public static final Rule DENY_MODIFICATION = (c,a,d) -> {
		if(a!=null) 
		{
			for(String m: mod) if(m.equals(a.getAction())) {
				logger.debug("DENY modification");
				return Decision.DENY;
			}
		}
		return Decision.UNCLEAR;
	};

	/**
	 * generic read access - useful for public endpoints
	 */
	public static final Rule PERMIT_READ = (c,a,d)-> {
		if(OperationType.read==a.getActionType()) {
			logger.debug("PERMIT read access");
			return Decision.PERMIT;
		}
		return Decision.UNCLEAR;
	};

	/**
	 * generic read access - useful for public endpoints
	 */
	public static final Rule PERMIT_READ_FOR_USER = (c,a,d)-> {
		if(a!=null && OperationType.read==a.getActionType()
				&& c!=null && "user".equals(c.getRole().getName())) {
			logger.debug("PERMIT read access for role 'user'");
			return Decision.PERMIT;
		}
		return Decision.UNCLEAR;
	};

	/**
	 * permit "POST" for role "user"
	 */
	public static final Rule PERMIT_POST_FOR_USER = (c,a,d)-> {
		if(c!=null && c.getRole()!=null && "user".equals(c.getRole().getName())
			&& a!=null && "POST".equals(a.getAction())
			) 
		{
			logger.debug("PERMIT POST for role 'user'");
			return Decision.PERMIT;
		}
		return Decision.UNCLEAR;
	};

}