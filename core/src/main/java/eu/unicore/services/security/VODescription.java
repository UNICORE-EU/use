package eu.unicore.services.security;

import java.io.Serializable;

/**
 * Holder of a VO information: vo mane and service address which defines sort of a VO namespace. In case
 * of fully SAML enabled VO services, the VO service address can be used to obtain SAML metadata and
 * subsequently addresses of all web services.
 * @author K. Benedyczak
 */
@Deprecated
public class VODescription implements Serializable
{
	private static final long serialVersionUID = 1L;
	private String voName;
	private String voServiceURI;

	public VODescription(String voName, String voServiceURI) {
		this.voName = voName;
		this.voServiceURI = voServiceURI;
	}
	
	public String getVoName() {
		return voName;
	}
	public void setVoName(String voName) {
		this.voName = voName;
	}
	public String getVoServiceURI() {
		return voServiceURI;
	}
	public void setVoServiceURI(String voServiceURI) {
		this.voServiceURI = voServiceURI;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((voName == null) ? 0 : voName.hashCode());
		result = prime * result + ((voServiceURI == null) ? 0 : voServiceURI.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		VODescription other = (VODescription) obj;
		if (voName == null)
		{
			if (other.voName != null)
				return false;
		} else if (!voName.equals(other.voName))
			return false;
		if (voServiceURI == null)
		{
			if (other.voServiceURI != null)
				return false;
		} else if (!voServiceURI.equals(other.voServiceURI))
			return false;
		return true;
	}

	@Override
	public String toString()
	{
		return voName + " at " + voServiceURI;
	}
}
