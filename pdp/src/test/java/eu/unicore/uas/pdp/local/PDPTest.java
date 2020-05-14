/*
 * Copyright (c) 2010 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 08-11-2010
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */
package eu.unicore.uas.pdp.local;

import java.util.Properties;

import org.junit.Before;

import de.fzj.unicore.wsrflite.ContainerProperties;

import eu.unicore.uas.pdp.AbstractPDPTest;


public class PDPTest extends AbstractPDPTest
{
	@Before
	public void setUp() throws Exception
	{
		pdp = new LocalHerasafPDP();
		ContainerProperties cp = new ContainerProperties(new Properties(), false);
		pdp.initialize("src/test/resources/local/pdp2.conf", 
			cp, null, null);
	}
}
