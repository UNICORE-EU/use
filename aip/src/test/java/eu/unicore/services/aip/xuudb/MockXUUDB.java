package eu.unicore.services.aip.xuudb;

import java.io.IOException;
import java.security.cert.X509Certificate;

import eu.emi.security.authn.x509.impl.X500NameUtils;
import eu.unicore.xuudb.X509Utils;
import eu.unicore.xuudb.interfaces.IPublic;
import eu.unicore.xuudb.xbeans.CheckCertChainResponseDocument;
import eu.unicore.xuudb.xbeans.CheckCertificateChainDocument;
import eu.unicore.xuudb.xbeans.CheckCertificateDocument;
import eu.unicore.xuudb.xbeans.CheckCertificateResponseDocument;
import eu.unicore.xuudb.xbeans.CheckDNDocument;
import eu.unicore.xuudb.xbeans.CheckDNResponseDocument;
import eu.unicore.xuudb.xbeans.LoginDataType;

public class MockXUUDB implements IPublic{

	int callCount=0;
	String lastDN;
	String expectedDN;
	
	String role;
	String xlogin;
	String projects;
	
	public CheckCertificateResponseDocument checkCertificate(
			CheckCertificateDocument arg0) {
		callCount++;
		String pem = arg0.getCheckCertificate().getCertInPEM();
		X509Certificate cert;
		try {
			cert = X509Utils.getX509FromPEMString(pem);
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
		CheckCertificateResponseDocument respDoc = CheckCertificateResponseDocument.Factory.newInstance();
		performCheck(cert.getSubjectX500Principal().getName(), respDoc.addNewCheckCertificateResponse());
		return respDoc;
	}

	public CheckCertChainResponseDocument checkCertificateChain(
			CheckCertificateChainDocument arg0) {
		callCount++;
		return null;
	}

	public CheckDNResponseDocument checkDN(CheckDNDocument req) {
		CheckDNResponseDocument res=CheckDNResponseDocument.Factory.newInstance();
		performCheck(req.getCheckDN().getDistinguishedName(), res.addNewCheckDNResponse());
		return res;
	}
	
	private void performCheck(String dn, LoginDataType res) {
		callCount++;
		lastDN = dn;
		if(expectedDN!=null && !X500NameUtils.equal(expectedDN,  lastDN)){
			throw new IllegalStateException("test failure: \n expected <"+expectedDN+"> \n got <"+lastDN+">");
		}
		if(xlogin!=null)res.setXlogin(xlogin);
		if(role!=null)res.setRole(role);
		if(projects!=null)res.setProjects(projects);
	}
	
	void reset(){
		callCount=0;
		lastDN=null;
		expectedDN=null;
		role=null;
		xlogin=null;
		projects=null;
	}
}
