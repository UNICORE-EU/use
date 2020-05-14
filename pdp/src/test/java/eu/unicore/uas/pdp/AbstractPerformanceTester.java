/*
 * Copyright (c) 2010 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 08-11-2010
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */
package eu.unicore.uas.pdp;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import de.fzj.unicore.wsrflite.security.pdp.ActionDescriptor;
import de.fzj.unicore.wsrflite.security.pdp.PDPResult;
import de.fzj.unicore.wsrflite.security.pdp.PDPResult.Decision;
import de.fzj.unicore.wsrflite.security.pdp.UnicoreXPDP;
import de.fzj.unicore.wsrflite.security.util.ResourceDescriptor;
import eu.unicore.security.Client;
import eu.unicore.security.OperationType;

import static org.junit.Assert.*;


public abstract class AbstractPerformanceTester implements Runnable
{
	protected UnicoreXPDP pdp;
	protected int threads;
	protected Client[] clients;
	protected ResourceDescriptor[] resDescs;
	protected String[] actions;
	protected Decision[] decisions;
	protected int iterationsPerThread;
	
	@Before
	public void setupLog()
	{
		Logger log = Logger.getRootLogger();
		log.setLevel(Level.ERROR);
	}
	
	public void run()
	{
		try
		{
			for (int i=0, j=0; i<iterationsPerThread; i++, j=(j+1)%clients.length)
			{
				PDPResult result = pdp.checkAuthorisation(clients[j], new ActionDescriptor(actions[j],
						OperationType.modify), resDescs[j]);
				assertTrue(result.getDecision().equals(decisions[j]));
			}
		} catch (Exception e)
		{
			e.printStackTrace();
			fail(e.toString());
		}
	}
	
	@Test
	public void testUser() throws InterruptedException 
	{
		Thread t[] = new Thread[threads];
		for (int i=0; i<threads; i++)
			t[i] = new Thread(this);
		long startTime = System.currentTimeMillis();

		for (int i=0; i<threads; i++)
			t[i].start();
		for (int i=0; i<threads; i++)
			t[i].join();
		
		long endTime = System.currentTimeMillis();
		double time = (endTime - startTime) / 1000.0;
		long total = iterationsPerThread*threads;
		System.out.println("Total time: " + time + "s;\n" +
				"Performed " + total + " authorizations\n" +
				"Operations per second: " + total/time);
	}
	
	protected void createStandardSetup() throws Exception
	{
		int q = 10;
		clients = new Client[q];
		resDescs = new ResourceDescriptor[q];
		actions = new String[q];
		decisions = new Decision[q];
		
		for (int i=0; i<q; i++)
		{
			actions[i] = "testAction" + i;
			if ((i%5) == 0)
			{
				clients[i] = MockAuthZContext.createRequest("admin", 
					"CN=Testing Tester,C=XX");
				resDescs[i] = new ResourceDescriptor(
					"http://serviceName" + i, "default_resource" + i, 
					"CN=Testing Owner,C=XX");
				decisions[i] = Decision.PERMIT;
			} else
			{
				clients[i] = MockAuthZContext.createRequest("user", 
					"CN=Testing Tester,C=X"+i);
				resDescs[i] = new ResourceDescriptor(
						"http://serviceName" + i, "default_resource" + i, 
					"CN=Testing Owner,C=X"+i);
				decisions[i] = Decision.DENY;
			}
		}

	}
}









