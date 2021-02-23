/*********************************************************************************
 * Copyright (c) 2006 Forschungszentrum Juelich GmbH 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * (1) Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the disclaimer at the end. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * 
 * (2) Neither the name of Forschungszentrum Juelich GmbH nor the names of its 
 * contributors may be used to endorse or promote products derived from this 
 * software without specific prior written permission.
 * 
 * DISCLAIMER
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************/


package de.fzj.unicore.wsrflite.utils;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.Logger;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlCursor.TokenType;
import org.apache.xmlbeans.XmlObject;

import de.fzj.unicore.wsrflite.ContainerProperties;

/**
 * tools and utilities
 * 
 * @author schuller
 * @author demuth
 */
public class Utilities {

	protected Utilities() {
	}

	/**
	 * returns a new globally unique identifier
	 */
	public static String newUniqueID(){
		return UUID.randomUUID().toString();
	}

	public static String extractServiceName(String url){
		try{
			URI u = new URI(url);
			String[] path = u.getPath().split("/");
			return path[path.length-1];
		}catch(Exception e){return null;}
	}

	/**
	 * validate that the given String value (interpreted as an Integer) 
	 * is in the supplied range
	 * 
	 * @param value - String to be verified
	 * @param minValue - minimum
	 * @param maxValue - maximum
	 * @return true if the value is within the range, false otherwise
	 */
	public static boolean validateIntegerRange(String value, int minValue, int maxValue){
		try{
			if(value==null)return false;
			Integer i=Integer.parseInt(value);
			if(i<minValue || i > maxValue){
				return false;
			}
		}catch(Exception e){
			return false;
		}
		return true;
	}


	/**
	 * return the physical server address
	 * @return a URL of the form scheme://host:port where 'scheme' is http or https
	 */
	public static String getPhysicalServerAddress(ContainerProperties cfg, boolean isSSLEnabled){
		String host=cfg.getValue(ContainerProperties.SERVER_HOST);
		String port=cfg.getValue(ContainerProperties.SERVER_PORT);
		String proto="http";
		if (isSSLEnabled) 
			proto="https";
		return proto+"://"+host+":"+port;
	}


	/**
	 * return the Gateway address, i.e.e the base part of this server's URL
	 * 
	 * @throws Exception
	 */
	public static String getGatewayAddress(ContainerProperties cfg)throws Exception{
		URL u=new URL(cfg.getValue(ContainerProperties.EXTERNAL_URL));
		return u.toString().split(u.getPath())[0];
	}
	
	/**
	 * extract the text content of an XML element 
	 * 
	 * @param source the xml element
	 * @return the text content, or "" if element has no content
	 */
	public static String extractElementTextAsString(XmlObject source){
		XmlCursor c=null;
		try{
			c=source.newCursor();
			while(c.hasNextToken()){
				if(c.toNextToken().equals(TokenType.TEXT)){
					return c.getChars();	
				}
			}
			return "";
		}finally{
			try{
				c.dispose();
			}catch(Exception e){}
		}
	}

	/**
	 * Set properties on the given object using the parameter map.
	 * The method attempts to find matching setters for the parameter names 
	 * 
	 * @param obj
	 * @param params
	 * @param logger - can be null, if non-null, errors and warnings will be logged
	 */
	public static void mapParams(Object obj, Map<String,String>params, Logger logger){
		Class<?> clazz=obj.getClass();
		for(Map.Entry<String,String> en: params.entrySet()){
			String s=en.getKey();
			String paramName=s.substring(s.lastIndexOf(".")+1);
			Method m=findSetter(clazz, paramName);
			if(m==null){
				if(logger!=null)logger.warn("Can't map parameter <"+s+">");
				continue;
			}
			try{
				setParam(obj,m,en.getValue());
			}
			catch(Exception ex){
				if(logger!=null)logger.warn("Can't set value <"+en.getValue()+"> for parameter <"+s+">");
			}
		}
	}
	
	/**
	 * finds a setter for the given parameter
	 * @param clazz
	 * @param paramName
	 */
	public static Method findSetter(Class<?> clazz, String paramName){
		for(Method m: clazz.getMethods()){
			if(m.getName().equalsIgnoreCase("set"+paramName) &&
				m.getParameterTypes().length > 0)return m;
		}
		return null;
	}
	
	private static void setParam(Object obj, Method m, String valueString)throws Exception{
		Object arg=valueString;
		if(m.getParameterTypes()[0].isAssignableFrom(int.class)){
			arg=Integer.parseInt(valueString);
		}
		else if(m.getParameterTypes()[0].isAssignableFrom(Integer.class)){
			arg=Integer.parseInt(valueString);
		}
		else if(m.getParameterTypes()[0].isAssignableFrom(long.class)){
			arg=Long.parseLong(valueString);
		}
		else if(m.getParameterTypes()[0].isAssignableFrom(Long.class)){
			arg=Long.parseLong(valueString);
		}
		else if(m.getParameterTypes()[0].isAssignableFrom(boolean.class)){
			arg=Boolean.valueOf(valueString);
		}
		else if(m.getParameterTypes()[0].isAssignableFrom(Boolean.class)){
			arg=Boolean.valueOf(valueString);
		}
		m.invoke(obj, new Object[]{arg});
	}
	
	
	/**
	 * gets a DateFormat instance for the ISO8601 "yyyy-MM-dd'T'HH:mm:ssZ" format
	 */
	public static DateFormat getISO8601(){
		return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
	}

	/**
	 * get a DateFormat instance for the "yyyy-MM-dd HH:mm" format
	 */
	public static DateFormat getSimpleDateFormat(){
		return new SimpleDateFormat("yyyy-MM-dd HH:mm");
	}

}
