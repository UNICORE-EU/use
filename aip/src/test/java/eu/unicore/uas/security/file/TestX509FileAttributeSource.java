/*
 * Copyright (c) 2010 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 06-09-2010
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */
package eu.unicore.uas.security.file;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Map;

import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.security.TestConfigUtil;

import junit.framework.TestCase;
import eu.emi.security.authn.x509.impl.CertificateUtils;
import eu.emi.security.authn.x509.impl.CertificateUtils.Encoding;
import eu.unicore.security.SecurityTokens;
import eu.unicore.security.SubjectAttributesHolder;

/**
 * @author golbi
 */
public class TestX509FileAttributeSource extends TestCase
{
	public static final String NAME = "TST1";

	
	private X509FileAttributeSource init(String file)
	{
		X509FileAttributeSource src = new X509FileAttributeSource();
		src.setFile(file);
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
		assertTrue(src.getStatusDescription().contains("OK"));
		return src;
	}
	
	public void test1()throws Exception
	{
		X509FileAttributeSource src = init("src/test/resources/file/testUudb-full-x509.xml");
		
		SecurityTokens tokens = new SecurityTokens();
		X509Certificate[] c = CertificateUtils.loadCertificateChain(
				new FileInputStream("src/test/resources/file/demoadmin.pem"), Encoding.PEM);
		tokens.setUser(c);
		tokens.setConsignor(c);
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
			
		} catch (IOException e)
		{
			e.printStackTrace();
			fail("Can't get attributes: " + e);
		}
	}
}
