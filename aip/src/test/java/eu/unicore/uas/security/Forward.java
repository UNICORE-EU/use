package eu.unicore.uas.security;
/*
 * Copyright (c) 2010 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 16-01-2011
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */


import javax.jws.WebMethod;

import de.fzj.unicore.wsrflite.xfire.ClientException;
import de.fzj.unicore.wsrflite.xmlbeans.BaseFault;
import de.fzj.unicore.wsrflite.xmlbeans.ResourceProperties;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.ResourceUnavailableFault;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.ResourceUnknownFault;
import eu.unicore.security.wsutil.RequiresSignature;


/**
 * A test service. Gets security configuration as input. Implementation shall 
 * invoke a getREsourcePropertyDocument operation at url given.
 * @author golbi
 *
 */
public interface Forward extends ResourceProperties
{
	@WebMethod(action="forwardAction")
	public void forward(String urlTo) throws BaseFault, ResourceUnknownFault, ResourceUnavailableFault, ClientException;
	
	@RequiresSignature
	@WebMethod(action="withDsig")
	public void withDSig(String in) throws BaseFault,ResourceUnknownFault, ResourceUnavailableFault;
}
