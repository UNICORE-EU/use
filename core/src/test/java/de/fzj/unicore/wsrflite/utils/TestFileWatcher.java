package de.fzj.unicore.wsrflite.utils;

import java.io.File;
import java.io.FileNotFoundException;

import junit.framework.TestCase;

public class TestFileWatcher extends TestCase {
	
	Boolean actionRun=Boolean.FALSE;
	
	public void testRun()throws Exception{
		File f=File.createTempFile("wsrflitetest", "bar");
		f.deleteOnExit();
		
		Runnable action=new Runnable() {
			public void run() {
				actionRun=Boolean.TRUE;
			}
		};
		
		FileWatcher fw=new FileWatcher(f, action);
		Thread.sleep(1000);
		f.setLastModified(System.currentTimeMillis());
		Thread.sleep(1000);
		fw.run();
		assertEquals(Boolean.TRUE,actionRun);
		actionRun=Boolean.FALSE;
		
		File f2=File.createTempFile("wsrflitetest", "foo");
		f2.deleteOnExit();
		fw.addTarget(f2);
		Thread.sleep(1000);
		f.setLastModified(System.currentTimeMillis());
		fw.run();
		assertEquals(Boolean.TRUE,actionRun);
		
	}

	public void testNoSuchFile()throws Exception{
		File f=new File(String.valueOf(System.currentTimeMillis()));
		
		Runnable action=new Runnable() {
			public void run() {
			}
		};
		
		try{
			new FileWatcher(f, action);
			fail("Expected file not found exception.");
		}catch(FileNotFoundException fne){
			/* OK */
		}
		
	}
	

	
}
