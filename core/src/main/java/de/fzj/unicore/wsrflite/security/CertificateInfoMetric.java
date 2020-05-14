package de.fzj.unicore.wsrflite.security;

import java.security.cert.X509Certificate;

import com.codahale.metrics.Gauge;

import eu.unicore.util.Log;

/**
 * provide some info about server's certificate via a metric 
 * (accessible via admin service)
 *  
 * @author schuller
 */
public class CertificateInfoMetric implements Gauge<String> {

	private final SecurityManager secManager;
	
	public CertificateInfoMetric(SecurityManager secManager){
		this.secManager=secManager;
	}
	
	@Override
	public String getValue() {
		StringBuilder sb=new StringBuilder();
		try{
			X509Certificate cert=secManager.getServerCert();
			if(cert!=null){
				sb.append("ServerIdentity: "+secManager.getServerIdentity());
				sb.append(";");
				sb.append("Expires: "+cert.getNotAfter());
				sb.append(";");
				sb.append("IssuedBy: "+cert.getIssuerX500Principal().getName());
			}
			else{
				sb.append("n/a");
			}
		}catch(Exception e){
			sb.append("Error: "+Log.createFaultMessage("", e)+">");
		}
		
		return sb.toString();
	}

}
