/*
 * Copyright (c) 2010 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 2010-11-13
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */
package eu.unicore.uas.pdp.local;

import java.util.Properties;

import org.junit.Before;

import de.fzj.unicore.wsrflite.ContainerProperties;
import eu.unicore.uas.pdp.AbstractPerformanceTester;

public class HerasafPerformanceCheck extends AbstractPerformanceTester
{
	@Before
	public void setup() throws Exception
	{
		ContainerProperties baseProps = new ContainerProperties(new Properties(), false);
		pdp = new LocalHerasafPDP();
		pdp.initialize("src/test/resources/local/pdp2.conf", baseProps, null, null);
		threads = 5;
		iterationsPerThread = 10000;
		createStandardSetup();
	}
	
	public static void main(String []args) throws Exception
	{
		HerasafPerformanceCheck obj = new HerasafPerformanceCheck();
		obj.setup();
		obj.testUser();
	}
}
