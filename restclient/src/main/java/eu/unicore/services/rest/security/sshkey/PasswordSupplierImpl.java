package eu.unicore.services.rest.security.sshkey;

import eu.emi.security.authn.x509.helpers.PasswordSupplier;

public class PasswordSupplierImpl implements PasswordSupplier {
	
	private final char[] password;
	
	public PasswordSupplierImpl(char[] password){
		this.password = password;
	}
	
	@Override
	public char[] getPassword() {
		return password;
	}

}
