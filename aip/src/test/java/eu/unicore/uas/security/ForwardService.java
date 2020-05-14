package eu.unicore.uas.security;
/*
 * Copyright (c) 2010 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 16-01-2011
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */



import javax.security.auth.x500.X500Principal;
import javax.xml.namespace.QName;

import org.oasisOpen.docs.wsrf.sg2.ServiceGroupRPDocument;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.wsrflite.Home;
import de.fzj.unicore.wsrflite.InitParameters;
import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.exceptions.ResourceNotCreatedException;
import de.fzj.unicore.wsrflite.security.ETDAssertionForwarding;
import de.fzj.unicore.wsrflite.xfire.ClientException;
import de.fzj.unicore.wsrflite.xmlbeans.BaseFault;
import de.fzj.unicore.wsrflite.xmlbeans.client.BaseWSRFClient;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.ResourceUnavailableFault;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.ResourceUnknownFault;
import eu.unicore.services.ws.impl.WSResourceImpl;
import eu.unicore.util.httpclient.IClientConfiguration;

public class ForwardService extends WSResourceImpl implements Forward
{
	public static final String TEST_SERVICE = "TestService";
	
	public static void createInstance(Kernel kernel) throws ResourceNotCreatedException
	{
		InitParameters init = new InitParameters("default");
		Home home = kernel.getHome(TEST_SERVICE);
		home.createResource(init);
	}
	
	@Override
	public QName getResourcePropertyDocumentQName()
	{
		return ServiceGroupRPDocument.type.getDocumentElementName();
	}

	@Override
	public void forward(String url) throws BaseFault, ResourceUnknownFault, ResourceUnavailableFault, ClientException
	{
		System.out.println("\n\nPERFORMING A SERVER-SIDE CALL\n");
		IClientConfiguration sec = kernel.getClientConfiguration();
		if (!ETDAssertionForwarding.configureETD(getClient(), sec))
			throw BaseFault.createFault("No ETD received from the client");
		EndpointReferenceType epr = EndpointReferenceType.Factory.newInstance();
		epr.addNewAddress().setStringValue(url);
		BaseWSRFClient client;
		String requestedUser = sec.getETDSettings().getRequestedUser();
		if (requestedUser == null)
			throw BaseFault.createFault("Wrong requested user was set: null");
		String reqUser = requestedUser;
		if (!reqUser.equals(new X500Principal(getClient().getDistinguishedName())) || 
			!reqUser.equals(getClient().getSecurityTokens().getConsignorName()))
			throw BaseFault.createFault("Wrong requested user was set: " 
					+ requestedUser + "\n client's DN: " + getClient().getDistinguishedName());
		
		
		try
		{
			client = new BaseWSRFClient(url, epr, sec);
		} catch (Exception e)
		{
			throw BaseFault.createFault(e.getMessage(), e);
		}
		client.getResourcePropertyDocument();
	}

	@Override
	public void withDSig(String in) throws BaseFault,
			ResourceUnknownFault,
			ResourceUnavailableFault
	{
	}
}
