/*
 * Copyright (c) 2012 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package de.fzj.unicore.wsrflite.security;

import static de.fzj.unicore.wsrflite.security.ContainerSecurityProperties.PREFIX;
import static de.fzj.unicore.wsrflite.security.ContainerSecurityProperties.PROP_CHECKACCESS;
import static de.fzj.unicore.wsrflite.security.ContainerSecurityProperties.PROP_GATEWAY_AUTHN;
import static de.fzj.unicore.wsrflite.security.ContainerSecurityProperties.PROP_REQUIRE_SIGNATURES;
import static de.fzj.unicore.wsrflite.security.ContainerSecurityProperties.PROP_SSL_ENABLED;

import java.io.File;
import java.util.Properties;

import org.apache.commons.io.FileUtils;

import de.fzj.unicore.wsrflite.USEClientProperties;
import eu.unicore.util.httpclient.ClientProperties;

public class TestConfigUtil {
	public static Properties getInsecureProperties() {
		Properties ret = new Properties();
		ret.setProperty(PREFIX+PROP_SSL_ENABLED, "false");
		ret.setProperty(PREFIX+PROP_CHECKACCESS, "false");
		ret.setProperty(PREFIX+PROP_GATEWAY_AUTHN, "false");
		ret.setProperty(PREFIX+PROP_REQUIRE_SIGNATURES, "false");
		ret.setProperty("container.client.serverHostnameChecking", "NONE");
		ret.setProperty(USEClientProperties.PREFIX+ClientProperties.PROP_MESSAGE_SIGNING_ENABLED, "false");
		String dir = "target/kerneldata";
		FileUtils.deleteQuietly(new File(dir));
		ret.setProperty("persistence.directory", dir);
		return ret;
	}
}
