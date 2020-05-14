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
 

package de.fzj.unicore.wsrflite.impl;

import java.io.File;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;

import de.fzj.unicore.persist.PersistenceProperties;
import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.persistence.Persistence;
import de.fzj.unicore.wsrflite.persistence.Store;
import de.fzj.unicore.wsrflite.security.TestConfigUtil;

public class TestExpireWSResources_Persistent extends TestExpireWSResources{

	private static String dir="target/data-test-expiry";
	
	@Before
	public void setUp()throws Exception{
		FileUtils.deleteQuietly(new File(dir));
		super.setUp();
	}
	
	@After
	public void tearDown(){
		FileUtils.deleteQuietly(new File(dir));
	}
	
	@Override
	protected Store createStore()throws Exception{
		Properties p = TestConfigUtil.getInsecureProperties();
		p.setProperty(PersistenceProperties.PREFIX + PersistenceProperties.DB_DIRECTORY, dir);
		Kernel k=new Kernel(p);
		Store s=new Persistence();
		s.init(k,"test123");
		return s;
	}
	
}
