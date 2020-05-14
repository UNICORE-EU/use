package de.fzj.unicore.wsrflite.testservice;

import de.fzj.unicore.wsrflite.Resource;
import de.fzj.unicore.wsrflite.impl.DefaultHome;

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
