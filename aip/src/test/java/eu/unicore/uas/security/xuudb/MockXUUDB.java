package eu.unicore.uas.security.xuudb;

import java.io.IOException;
import java.security.cert.X509Certificate;

import de.fzJuelich.unicore.xuudb.CheckCertChainResponseDocument;
import de.fzJuelich.unicore.xuudb.CheckCertificateChainDocument;
import de.fzJuelich.unicore.xuudb.CheckCertificateDocument;
import de.fzJuelich.unicore.xuudb.CheckCertificateResponseDocument;
import de.fzJuelich.unicore.xuudb.CheckDNDocument;
import de.fzJuelich.unicore.xuudb.CheckDNResponseDocument;
import de.fzJuelich.unicore.xuudb.LoginDataType;
import de.fzj.unicore.xuudb.X509Utils;
import de.fzj.unicore.xuudb.interfaces.IPublic;
import eu.emi.security.authn.x509.impl.X500NameUtils;

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
