/*
 * Copyright (c) 2010 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 08-11-2010
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */
package eu.unicore.uas.pdp;

import java.io.FileInputStream;
import java.security.cert.X509Certificate;

import eu.emi.security.authn.x509.impl.CertificateUtils;
import eu.emi.security.authn.x509.impl.CertificateUtils.Encoding;
import eu.unicore.security.Client;
import eu.unicore.security.Role;
import eu.unicore.security.SecurityTokens;



public class MockAuthZContext
{
	public static Client createRequest(String role, String name) throws Exception
	{
		Client c = new Client();
		c.setRole(new Role(role, ""));
		SecurityTokens t = new SecurityTokens();
		X509Certificate[] consignor = CertificateUtils.loadCertificateChain(
				new FileInputStream("src/test/resources/local/consignor.der"), Encoding.DER);
		t.setConsignor(consignor);
		t.setUserName(name);
		t.setConsignorTrusted(true);
		c.setAuthenticatedClient(t);
		return c;
	}
}
