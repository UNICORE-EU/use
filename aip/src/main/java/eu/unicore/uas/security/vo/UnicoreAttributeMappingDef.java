/*
 * Copyright (c) 2010 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 07-10-2010
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */
package eu.unicore.uas.security.vo;

/**
 * This structure defines UNICORE incarnation attribute mapping (from saml name).
 * UNICORE side must fill the UNICORE name and properties of the attributes.
 * SAML callout configuration should provide for attributes a SAML name and if needed a default
 * SAML attribute name.
 * @author golbi
 *
 */
public class UnicoreAttributeMappingDef
{
	private String unicoreName;
	private String samlName;
	private String defSamlName;
	private boolean multiVal;
	private boolean nonZeroVal;
	private boolean disabledInPull = false;
	private boolean disabledInPush = false;
	
	public UnicoreAttributeMappingDef(String unicoreName, 
			boolean multiVal, boolean nonZeroVal)
	{
		if (unicoreName == null)
			throw new IllegalArgumentException("unicoreName must not be null");
		this.unicoreName = unicoreName;
		this.multiVal = multiVal;
		this.nonZeroVal = nonZeroVal;
	}

	
	public String getUnicoreName()
	{
		return unicoreName;
	}
	public void setUnicoreName(String unicoreName)
	{
		this.unicoreName = unicoreName;
	}
	public String getSamlName()
	{
		return samlName;
	}
	public void setSamlName(String samlName)
	{
		this.samlName = samlName;
	}
	public String getDefSamlName()
	{
		return defSamlName;
	}
	public void setDefSamlName(String defSamlName)
	{
		this.defSamlName = defSamlName;
	}
	public void setMultiVal(boolean multiVal)
	{
		this.multiVal = multiVal;
	}
	public boolean isMultiVal()
	{
		return multiVal;
	}
	public void setNonZeroVal(boolean nonZeroVal)
	{
		this.nonZeroVal = nonZeroVal;
	}
	public boolean isNonZeroVal()
	{
		return nonZeroVal;
	}
	public boolean isDisabledInPull()
	{
		return disabledInPull;
	}
	public void setDisabledInPull(boolean disabledInPull)
	{
		this.disabledInPull = disabledInPull;
	}
	public boolean isDisabledInPush()
	{
		return disabledInPush;
	}
	public void setDisabledInPush(boolean disabledInPush)
	{
		this.disabledInPush = disabledInPush;
	}
}