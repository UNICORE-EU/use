package eu.unicore.services.ws.exampleservice;

import de.fzj.unicore.wsrflite.ContainerProperties;
import de.fzj.unicore.wsrflite.security.IContainerSecurityConfiguration;
import de.fzj.unicore.wsrflite.security.pdp.ActionDescriptor;
import de.fzj.unicore.wsrflite.security.pdp.PDPResult;
import de.fzj.unicore.wsrflite.security.pdp.UnicoreXPDP;
import de.fzj.unicore.wsrflite.security.pdp.PDPResult.Decision;
import de.fzj.unicore.wsrflite.security.util.ResourceDescriptor;
import eu.unicore.security.Client;
import eu.unicore.util.httpclient.IClientConfiguration;

/*
 * testing PDP which will forbid access to 
 * <ul>
 * <li>methods containing the strings "forbidden"
 * <li> the UpdateResourceProperties method
 * </ul>
 */
public class MockPDP implements UnicoreXPDP {

	@Override
	public PDPResult checkAuthorisation(Client c, ActionDescriptor action,
			ResourceDescriptor d) throws Exception {
		//System.out.println("PDP Check : "+action);
		if(action!=null && (action.getAction().contains("forbidden"))){
			return new PDPResult(Decision.DENY,"forbidden method: "+action);
		} else if (action != null && action.getAction().equals("InsertResourceProperties"))
			return new PDPResult(Decision.DENY,"forbidden method: "+action);
		else return new PDPResult(Decision.PERMIT,"OK");
	}

	@Override
	public void initialize(String configuration, ContainerProperties baseSettings,
			IContainerSecurityConfiguration securityConfiguration,
			IClientConfiguration clientConfiguration) throws Exception {
	}
}
