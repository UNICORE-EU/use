package eu.unicore.services.rest.security.sshkey;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.DSAPublicKeySpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.bouncycastle.asn1.nist.NISTNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.jce.spec.ECNamedCurveSpec;

import eu.emi.security.authn.x509.helpers.PasswordSupplier;
import eu.emi.security.authn.x509.impl.CertificateUtils;

/**
 * helpers to read SSH private/public keys
 * 
 * @author schuller
 */
public class SSHUtils {

	public static PrivateKey readPrivateKey(File priv, PasswordSupplier password) throws IOException {
		return CertificateUtils.loadPEMPrivateKey(new FileInputStream(priv), password);
	}
	
	public static PublicKey readPublicKey(File file) throws IOException, GeneralSecurityException {
		String pubkey = FileUtils.readFileToString(file);
		return readPubkey(pubkey);
	}
	
	public static PublicKey readPubkey(String pubkey) throws IOException, GeneralSecurityException {
		String base64, format = null;
		StringTokenizer st = new StringTokenizer(pubkey);
		try {
			format = st.nextToken();
			base64 = st.nextToken();
			try{
				st.nextToken();
			}catch(NoSuchElementException e){/*ignored since comment is not important*/}
		} catch (NoSuchElementException e) {
			throw new IllegalArgumentException("Cannot read public key, expect SSH format");
		}
		if(format.contains("rsa")){
			return readRSA(base64);
		}
		if(format.contains("ecdsa")){
			return readECDSA(base64);
		}
		else if (format.contains("dsa") || format.contains("dss")){
			return readDSA(base64);
		}
		else{
			throw new IOException("Format "+format+" not known");
		}
	}
	
	private static PublicKey readRSA(String base64) throws IOException, GeneralSecurityException {
		byte[] decoded = Base64.decodeBase64(base64.getBytes());
		DataInputStream data = new DataInputStream(new ByteArrayInputStream(decoded));
		String type = new String(readString(data));
		if(!type.contains("rsa")){
			throw new IllegalArgumentException("Expected RSA public key, got: "+type);
		}
		BigInteger expo = new BigInteger(readString(data));
		BigInteger modulus = new BigInteger(readString(data));
		RSAPublicKeySpec keySpec = new RSAPublicKeySpec(modulus, expo);
		return KeyFactory.getInstance("RSA").generatePublic(keySpec);
	}
	
	private static PublicKey readDSA(String base64) throws IOException, GeneralSecurityException {
		byte[] decoded = Base64.decodeBase64(base64.getBytes());
		DataInputStream data = new DataInputStream(new ByteArrayInputStream(decoded));
		String type = new String(readString(data));
		if(!type.contains("dsa") && !type.contains("dss")){
			throw new IllegalArgumentException("Expected DSA public key, got: "+type);
		}
		BigInteger p = new BigInteger(readString(data));
		BigInteger q = new BigInteger(readString(data));
		BigInteger g = new BigInteger(readString(data));
		BigInteger y = new BigInteger(readString(data));
		DSAPublicKeySpec keySpec = new DSAPublicKeySpec (y,p,q,g);
		
		return KeyFactory.getInstance("DSA").generatePublic(keySpec);
	}
	
	private static PublicKey readECDSA(String base64) throws IOException, GeneralSecurityException {
		byte[] decoded = Base64.decodeBase64(base64.getBytes());
		DataInputStream data = new DataInputStream(new ByteArrayInputStream(decoded));
		readString(data); // key type

		String curve = new String(readString(data));

		int keylen = data.readInt();
		data.readByte(); // the number "4", for some reason
		
        final byte[] x = new byte[(keylen-1)/2];
        final byte[] y = new byte[(keylen-1)/2];
        
		data.read(x);
		BigInteger bigX = new BigInteger(1, x);
		data.read(y);
		BigInteger bigY = new BigInteger(1, y);

        String name = "P-"+curve.substring(5);
        X9ECParameters ecParams = NISTNamedCurves.getByName(name);
        ECNamedCurveSpec ecCurveSpec = new ECNamedCurveSpec(name, ecParams.getCurve(), ecParams.getG(), ecParams.getN());
        ECPoint p = new ECPoint(bigX, bigY);
        ECPublicKeySpec publicKeySpec = new ECPublicKeySpec(p, ecCurveSpec);

        KeyFactory keyFactory = KeyFactory.getInstance("ECDSA");
        return keyFactory.generatePublic(publicKeySpec);
	}
	
	private static byte[] readString(DataInputStream data) throws IOException {  
	    int len = data.readInt();  
	    byte[] str = new byte[len];  
	    for(int i=0; i<len;i++){
	    	str[i]=data.readByte();
	    }  
	    return str;  
	}   
	
}
