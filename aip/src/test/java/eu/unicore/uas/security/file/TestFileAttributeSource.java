/*
 * Copyright (c) 2010 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 06-09-2010
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */
package eu.unicore.uas.security.file;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import eu.unicore.services.Kernel;
import eu.unicore.services.security.TestConfigUtil;

import eu.unicore.security.SecurityTokens;
import eu.unicore.security.SubjectAttributesHolder;

import junit.framework.TestCase;

/**
 * @author golbi
 */
public class TestFileAttributeSource extends TestCase
{
	public static final String NAME = "TST1";

	
	private FileAttributeSource init(String file, String matching)
	{
		FileAttributeSource src = new FileAttributeSource();
		src.setFile(file);
		src.setMatching(matching);
		try
		{
			Kernel k=new Kernel(TestConfigUtil.getInsecureProperties());
			src.configure(NAME);
			src.start(k);
		} catch (Exception e)
		{
			e.printStackTrace();
			fail("Can't init AttributeSource: " + e);
		}
		
		assertTrue(NAME.equals(src.getName()));
		return src;
	}
	
	public void testStrict()
	{
		FileAttributeSource src = init("src/test/resources/file/testUudb-strict.xml", "strict");
		
		SecurityTokens tokens = new SecurityTokens();
		tokens.setUserName("CN=Stanisław Lem, C=PL");
		tokens.setConsignorTrusted(true);
		
		try
		{
			SubjectAttributesHolder holder = src.getAttributes(tokens, null);
			Map<String, String[]> valid = holder.getValidIncarnationAttributes();
			Map<String, String[]> def = holder.getDefaultIncarnationAttributes();
			assertTrue(valid.size() == 2);
			assertTrue(valid.get("role") != null && valid.get("role").length == 1
					&& valid.get("role")[0].equals("user"));
			assertTrue(valid.get("xlogin") != null && valid.get("xlogin").length == 2
					&& valid.get("xlogin")[0].equals("somebody")
					&& valid.get("xlogin")[1].equals("nobody"));
			assertTrue(valid.get("empty") == null);
			
			assertTrue(holder.getXacmlAttributes().size() == 2);
			
			assertTrue(def.size() == 2);
			assertTrue(def.get("role") != null && def.get("role").length == 1
					&& def.get("role")[0].equals("user"));
			assertTrue(def.get("xlogin") != null && def.get("xlogin").length == 1
					&& def.get("xlogin")[0].equals("somebody"));
			assertTrue(def.get("empty") == null);
			
			tokens.setUserName("CN=Dead Man, C=US");
			holder = src.getAttributes(tokens, null);
			valid = holder.getValidIncarnationAttributes();
			def = holder.getDefaultIncarnationAttributes();
			assertTrue(valid.size() == 1 && def.size() == 1);
			assertTrue(valid.get("role") != null && valid.get("role").length == 1
					&& valid.get("role")[0].equals("user1"));
			assertTrue(def.get("role") != null && def.get("role").length == 1
					&& def.get("role")[0].equals("user1"));
			
			tokens.setUserName("C=US, CN=Dead Man");
			holder = src.getAttributes(tokens, null);
			valid = holder.getValidIncarnationAttributes();
			def = holder.getDefaultIncarnationAttributes();
			assertTrue(valid.size() == 0);
			assertTrue(def.size() == 0);
		} catch (IOException e)
		{
			e.printStackTrace();
			fail("Can't get attributes: " + e);
		}
	}

	public void testRegExp()
	{
		FileAttributeSource src = init("src/test/resources/file/testUudb-regexp.xml", "regexp");
		
		SecurityTokens tokens = new SecurityTokens();
		tokens.setConsignorTrusted(true);
		SubjectAttributesHolder map;
		
		try
		{
			tokens.setUserName("CN=Stanisław Lem, O=ICM, C=PL");
			map = src.getAttributes(tokens, null);
			assertTrue(map.getValidIncarnationAttributes().size() == 2);

			tokens.setUserName("CN=Stanisław Lem, O=ACK, C=PL");
			map = src.getAttributes(tokens, null);
			assertTrue(map.getValidIncarnationAttributes().size() == 1);

			tokens.setUserName("CN=Stanisław Lem, O=I, C=PL");
			map = src.getAttributes(tokens, null);
			assertTrue(map.getValidIncarnationAttributes().size() == 2);
						
			tokens.setUserName("CN=Dead Man, C=US");
			map = src.getAttributes(tokens, null);
			assertTrue(map.getValidIncarnationAttributes().size() == 1);
		} catch (IOException e)
		{
			e.printStackTrace();
			fail("Can't get attributes: " + e);
		}
	}


	
	public void testRefresh() throws InterruptedException
	{
		String srcF = "src/test/resources/file/testUudb-strict.xml";
		String dst = "target/test-classes/testUudb-copy.xml";
		try
		{
			File f = new File(dst);
			f.delete();
			copyFile(srcF, dst);

			FileAttributeSource src = init(dst, "regexp");

			SecurityTokens tokens = new SecurityTokens();
			tokens.setConsignorTrusted(true);
			SubjectAttributesHolder holder;
			Map<String, String[]> def;

			tokens.setUserName("CN=Dead Man, C=US");
			holder = src.getAttributes(tokens, null);
			def = holder.getDefaultIncarnationAttributes();
			
			assertTrue(def.size() == 1);
			assertTrue(def.get("role") != null && def.get("role").length == 1
					&& def.get("role")[0].equals("user1"));
			Thread.sleep(1000);
			copyFile("src/test/resources/file/testUudb-regexp.xml", 
				dst);
			
			holder = src.getAttributes(tokens, null);
			def = holder.getDefaultIncarnationAttributes();
			assertTrue(def.size() == 1);
			assertTrue(def.get("role") != null && def.get("role").length == 1
					&& def.get("role")[0].equals("banned"));

		} catch (IOException e)
		{
			e.printStackTrace();
			fail("Can't get attributes: " + e);
		}
	}




	private static void copyFile(String from, String to) throws IOException
	{
		BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(to));
		InputStream fis = new BufferedInputStream(new FileInputStream(from));
		int b;
		while ((b = fis.read()) != -1)
			os.write(b);
		os.close();
		fis.close();
	}



}
