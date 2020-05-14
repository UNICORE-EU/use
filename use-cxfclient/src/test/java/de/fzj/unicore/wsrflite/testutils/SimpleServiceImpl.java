package de.fzj.unicore.wsrflite.testutils;

import javax.annotation.Resource;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.ContextUtils;

import de.fzj.unicore.wsrflite.xmlbeans.AdminActionRequestDocument;
import de.fzj.unicore.wsrflite.xmlbeans.AdminActionResponseDocument;
import de.fzj.unicore.wsrflite.xmlbeans.AdminActionResponseDocument.AdminActionResponse;
import de.fzj.unicore.wsrflite.xmlbeans.AdminActionValueType;
import de.fzj.unicore.wsrflite.xmlbeans.GetServiceInstancesRequestDocument;
import de.fzj.unicore.wsrflite.xmlbeans.GetServiceInstancesResponseDocument;
import eu.unicore.security.SecurityTokens;

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
