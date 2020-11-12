package eu.unicore.uas.security.vo;


import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.Phase;
import org.apache.logging.log4j.Logger;

import eu.unicore.security.SecurityTokens;
import eu.unicore.security.wsutil.AuthInHandler;
import eu.unicore.security.wsutil.ETDInHandler;
import eu.unicore.uas.security.vo.conf.IPullConfiguration;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.IClientConfiguration;



/**
 * This handler uses {@link VOAttributeFetcher} class to load authenticated User's 
 * attributes from the configured VO service.
 * 
 * @see VOAttributeFetcher
 * @author K. Benedyczak
 */
public class SAMLAttributePullInHandler extends AbstractSoapInterceptor
{
	private static final Logger log = Log.getLogger(IPullConfiguration.LOG_PFX, 
			SAMLAttributePullInHandler.class);
	private VOAttributeFetcher fetcher;
	
	public SAMLAttributePullInHandler(IPullConfiguration cc,
			IClientConfiguration clientSettings) throws Exception
	{
		super(Phase.PRE_INVOKE);
		getAfter().add(AuthInHandler.class.getName());
		getAfter().add(ETDInHandler.class.getName());
		fetcher = new VOAttributeFetcher(cc, clientSettings);
	}
	
	
	public void handleMessage(SoapMessage message)
	{
		SecurityTokens tokens = (SecurityTokens) message.get(SecurityTokens.KEY);
		if (tokens == null)
		{
			log.error("Handlers are badly configured. " + 
				SAMLAttributePullInHandler.class.getName() + 
				" must be invoked AFTER " + 
				AuthInHandler.class.getName());
			return;
		}
		try{
			fetcher.authorise(tokens);
		}catch(Exception ex){
			throw new Fault(ex);
		}
	}
}


