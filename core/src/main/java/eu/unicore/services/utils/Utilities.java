package eu.unicore.services.utils;

import java.lang.reflect.Method;
import java.math.BigInteger;
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

import eu.unicore.services.ContainerProperties;

/**
 * tools and utilities
 * 
 * @author schuller
 * @author demuth
 */
public class Utilities {

	private Utilities() {
	}

	/**
	 * returns a new unique identifier that is
	 * reasonably short and human-friendly
	 */
	public static String newUniqueID(){
		UUID u = UUID.randomUUID();
		BigInteger l = asUnsigned(u.getMostSignificantBits(), u.getLeastSignificantBits());
		return Base62.encode(l);
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
	
	
	
	private static final BigInteger HALF = BigInteger.ONE.shiftLeft(64); // 2^64

	private static BigInteger asUnsigned(long hi, long low) {
		BigInteger l = BigInteger.valueOf(low+(2^64*hi));
		return l.signum() < 0 ? l.add(HALF) : l;
	}
			
	/**
	 * Base62 encoder from
	 * https://github.com/opencoinage/opencoinage/blob/master/src/java/org/opencoinage/util/Base62.java
	 * @see http://en.wikipedia.org/wiki/Base_62
	 */
	public static class Base62 {
		public static final BigInteger BASE = BigInteger.valueOf(62);
		public static final String DIGITS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
		public static final String REGEXP = "^[0-9A-Za-z]+$";

		/**
		 * Encodes a number using Base62 encoding.
		 *
		 * @param  number a positive integer
		 * @return a Base62 string
		 * @throws IllegalArgumentException if <code>number</code> is a negative integer
		 */
		public static String encode(BigInteger number) {
			if (number.compareTo(BigInteger.ZERO) == -1) { // number < 0
				throw new IllegalArgumentException("number must not be negative");
			}
			StringBuilder result = new StringBuilder();
			while (number.compareTo(BigInteger.ZERO) == 1) { // number > 0
				BigInteger[] divmod = number.divideAndRemainder(BASE);
				number = divmod[0];
				int digit = divmod[1].intValue();
				result.insert(0, DIGITS.charAt(digit));
			}
			return (result.length() == 0) ? DIGITS.substring(0, 1) : result.toString();
		}

		public static String encode(long number) {
			return encode(BigInteger.valueOf(number));
		}

		/**
		 * Decodes a string using Base62 encoding.
		 *
		 * @param  string a Base62 string
		 * @return a positive integer
		 * @throws IllegalArgumentException if <code>string</code> is empty
		 */
		public static BigInteger decode(final String string) {
			if (string.length() == 0) {
				throw new IllegalArgumentException("string must not be empty");
			}
			BigInteger result = BigInteger.ZERO;
			int digits = string.length();
			for (int index = 0; index < digits; index++) {
				int digit = DIGITS.indexOf(string.charAt(digits - index - 1));
				result = result.add(BigInteger.valueOf(digit).multiply(BASE.pow(index)));
			}
			return result;
		}
	}
}
