package eu.unicore.uas.security;
/*
 * Copyright (c) 2010 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 16-01-2011
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */


import de.fzj.unicore.wsrflite.Resource;
import eu.unicore.services.ws.impl.WSResourceHomeImpl;

public class ForwardServiceHome extends WSResourceHomeImpl
{
	protected Resource doCreateInstance()
	{
		return new ForwardService();
	}
}