/*
 * Copyright (c) 2010 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 08-11-2010
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */
package eu.unicore.uas.pdp.local;

import org.junit.Before;

import eu.unicore.uas.pdp.AbstractPDPTest;


public class PDPTest extends AbstractPDPTest
{
	@Before
	public void setUp() throws Exception
	{
		String f = "src/test/resources/local/pdp2.conf";
		pdp = new LocalHerasafPDP();
		((LocalHerasafPDP)pdp).initialize(f, "http://test123.local");
		((LocalHerasafPDP)pdp).lps.reload(f);
	}
}
