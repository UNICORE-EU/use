package eu.unicore.services.ws.testutils;

import javax.annotation.Resource;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.ContextUtils;

import eu.unicore.security.SecurityTokens;
import eu.unicore.services.ws.AdminActionRequestDocument;
import eu.unicore.services.ws.AdminActionResponseDocument;
import eu.unicore.services.ws.AdminActionResponseDocument.AdminActionResponse;
import eu.unicore.services.ws.AdminActionValueType;
import eu.unicore.services.ws.GetServiceInstancesRequestDocument;
import eu.unicore.services.ws.GetServiceInstancesResponseDocument;

public class SimpleServiceImpl implements SimpleService{
	
	@Resource
	private WebServiceContext context;
	
	public GetServiceInstancesResponseDocument foo(GetServiceInstancesRequestDocument in){
		GetServiceInstancesResponseDocument res=GetServiceInstancesResponseDocument.Factory.newInstance();
		String input=in.getGetServiceInstancesRequest().getServiceName();
		res.addNewGetServiceInstancesResponse().setServiceNamespace(input);
		return res;
	}

	private SecurityTokens getTokens()
	{
		MessageContext ctx = context.getMessageContext();
		SecurityTokens tokens = (SecurityTokens)ctx.get(SecurityTokens.KEY);
		return tokens;
	}
	
	/**
	 * outputs some detected WS-A headers
	 */
	public AdminActionResponseDocument bar(AdminActionRequestDocument in){
		AdminActionResponseDocument res=AdminActionResponseDocument.Factory.newInstance();
		AdminActionResponse ar=res.addNewAdminActionResponse();
		AdminActionValueType v1=ar.addNewResults();
		AddressingProperties add=(AddressingProperties)
				context.getMessageContext().get(ContextUtils.getMAPProperty(false, false, false));
		boolean log = false;
		try{
			log = Boolean.parseBoolean(in.getAdminActionRequest().getParameterArray(0).getValue());
		}catch(Exception ex){}
		if(log){
			System.out.println("***");
			System.out.println("security tokens : "+getTokens());
			System.out.println("wsa properties  : "+add);
			System.out.println("***");
		}
		v1.setName("wsa-to");
		v1.setValue(add.getTo().getValue());
		return res;
	}
	
}
