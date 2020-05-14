/*
 * Copyright (c) 2008 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 2008-12-22
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */

package eu.unicore.services.ws.security;

import org.apache.cxf.message.Message;

import de.fzj.unicore.wsrflite.security.DSignCheck;
import eu.unicore.security.wsutil.CXFUtils;
import eu.unicore.security.wsutil.DSigDecider;

/**
 * This class decides whether incoming message should have its digital signature checked or not. 
 * Note that if we want to verify a signature and it is not present,
 * the actual check must be done in server's code, as secutils won't deny a message.
 * 
 * @author golbi
 */
public class UASDSigDecider2 implements DSigDecider
{
	private final boolean disable;
	private final DSignCheck engine;
	
	public UASDSigDecider2(DSignCheck engine, boolean disable)
	{
		this.disable = disable;
		this.engine = engine;
	}
	
	public boolean isMessageDSigCandidate(Message message)
	{
		if (disable)
			return false;
		
		String action=null;
		try{
			action = CXFUtils.getAction(message);
		}catch(Exception ex){
			// for example in case of a ?wsdl request. 
			// TODO need better way to handle this
		}
		
		return action != null && engine.needSignature(action);
	}
}
