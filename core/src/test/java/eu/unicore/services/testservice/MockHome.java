package eu.unicore.services.testservice;

import eu.unicore.services.Resource;
import eu.unicore.services.impl.DefaultHome;

public class MockHome extends DefaultHome {

	@Override
	protected Resource doCreateInstance() throws Exception {
		return null;
	}

	public static boolean startupTaskWasRun=false;
	
	@Override
	public void run(){
		startupTaskWasRun=true;
	}
	
}
