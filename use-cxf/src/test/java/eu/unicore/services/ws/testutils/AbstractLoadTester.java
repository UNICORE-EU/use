/*********************************************************************************
 * Copyright (c) 2006 Forschungszentrum Juelich GmbH 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * (1) Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the disclaimer at the end. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * 
 * (2) Neither the name of Forschungszentrum Juelich GmbH nor the names of its 
 * contributors may be used to endorse or promote products derived from this 
 * software without specific prior written permission.
 * 
 * DISCLAIMER
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************/
 
package eu.unicore.services.ws.testutils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;

import org.junit.Before;
import org.oasisOpen.docs.wsrf.rp2.GetResourcePropertyDocumentDocument1;

/**
 * does simple load testing, storing client-side response times and
 * turnaround times
 * 
 * you can subclass this and override the getTask() method
 * 
 * @author schuller
 */
public abstract class AbstractLoadTester extends JettyTestCase {
	
	public int max_num_client_threads=2;
	
	public int requests_per_client=30;

	public int calls_per_task=5;
	
	protected static Writer statsWriter;
	
	public static boolean haveInit=false;
	
	private static GetResourcePropertyDocumentDocument1 getRpdReq;
	
	protected static int running=0;
	private static ArrayList<Long> responseTimes;
	
	@Before
	public void addServices() throws Exception {
		if(haveInit)return;
		
		responseTimes=new ArrayList<Long>();

		statsWriter=makeOutputWriter();
		
		if(haveInit)return;
		
		//setup static xml docs
		getRpdReq=GetResourcePropertyDocumentDocument1.Factory.newInstance();
		getRpdReq.addNewGetResourcePropertyDocument();
		
		printStats(new Date()+": gathering statistics started.");
		haveInit=true;
	}
	
	protected static Writer makeOutputWriter()throws IOException{
		String statsFile="./loadtest";
		while(new File(statsFile).exists()) statsFile+="_1";
		return new FileWriter(statsFile);
	}

	/**
	 * a single task to be run. This will be run many times by many client threads
	 * @return
	 */
	protected abstract Runnable getTask();
	

	/**
	 * This will be run by many client threads
	 * @return
	 */
	protected Runnable runOneClient(){
		
		return new Runnable(){
			
			public void run(){
				try {
					String tName=Thread.currentThread().getName();
					int i=0;
					System.out.println(tName+" starting");
					while(i<requests_per_client){
						runTimed("["+tName+"] Run single task ",getTask());
						i++;
					}
					
					printStats("["+Thread.currentThread().getName()+"] ended.");
					running--;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
	}
	

	protected void runTimed(String msg, Runnable r)throws Exception{
		long s=System.currentTimeMillis();
		r.run();
		long e=System.currentTimeMillis();
		long t=(e-s)/calls_per_task;
		printStats(msg+" ["+t+" ms], running threads="+running);
		responseTimes.add(Long.valueOf(t));
	}
	
	protected static synchronized void printStats(String s)throws Exception {
		statsWriter.append(s+"\n");
		statsWriter.flush();
	}
	
	//run a test...
	public void test() throws Exception{
		System.out.println("Running load test.");
		System.out.println("Client Threads="+max_num_client_threads);
		System.out.println("Tasks per client="+requests_per_client);
		System.out.println("Requests per Task="+calls_per_task);
		
		long start=System.currentTimeMillis();
		
		//startup threads
		for(int i=0;i<max_num_client_threads;i++){
			Thread t=new Thread(runOneClient());
			t.setName("LoadTesterThread-"+i);
			t.start();
			running++;
		}
		//wait
		while(running>0) {
			System.out.println("running="+running);
			Thread.sleep(1500);
		}
		System.out.println("running="+running);
		Long a=Long.valueOf(0);
		Long total=Long.valueOf(0);
		for(Long l:responseTimes){
			total+=l;
		}
		a=total/responseTimes.size();
		long t=System.currentTimeMillis()-start;
		long tx=calls_per_task*max_num_client_threads*requests_per_client;
		printStats("Summary:");
		printStats("Ran "+tx+
				" requests ("+max_num_client_threads+" client threads, "+
				calls_per_task*requests_per_client+" requests each.)");
		printStats("Average response time: "+a+ "ms.");
		printStats("Total time: "+t+ "ms.");
		printStats("Transactions per sec: "+tx*1000/t);
		printStats(new Date()+": Collecting stats ended.");
	}
	
	
	
}
