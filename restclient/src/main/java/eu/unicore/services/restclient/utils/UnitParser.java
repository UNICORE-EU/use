package eu.unicore.services.restclient.utils;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * helper for working with common units and multipliers
 * (not threadsafe!)
 *
 * @author schuller
 */
public class UnitParser {

	private final static Pattern valueWithUnitsPattern=Pattern.compile("\\s*(\\d*[\\.\\,]?\\d*)\\s*(\\w*)\\s*");

	private final String[][]units;
	private final int[]conversionFactors;
	private final NumberFormat outputFormat;
	private final int decimalDigits;

	/**
	 * @param units - unit names (with synonyms)
	 * @param conversionFactors - conversion factors
	 * @param decimalDigits - the number of decimal digits to use
	 */
	public UnitParser(String[][]units,int[]conversionFactors, int decimalDigits){
		this.units = units;
		this.conversionFactors = conversionFactors;
		this.outputFormat = NumberFormat.getNumberInstance();
		outputFormat.setMaximumFractionDigits(decimalDigits);
		outputFormat.setMinimumFractionDigits(decimalDigits);
		this.decimalDigits = decimalDigits;
	}

	public UnitParser(String[][]units,int[]conversionFactors){
		this(units,conversionFactors,0);
	}

	/**
	 * convert a value with units to the value in default units
	 * @param valueWithUnits
	 * @return String representation (without units!)
	 */
	public String getStringValue(String valueWithUnits){
		return outputFormat.format(getDoubleValue(valueWithUnits));
	}

	/**
	 * get the value (in default units)
	 * @param valueWithUnits
	 * @return double
	 */
	public double getDoubleValue(String valueWithUnits){
		String u = getUnits(valueWithUnits);
		int conversion = 1;
		if(u!=null){
			conversion = getConversionFactor(u);
		}
		return Double.parseDouble(getNumber(valueWithUnits))*conversion;
	}

	/**
	 * return the "optimal" string representation of the given value
	 * 
	 * @param d - the value to represent
	 * @param forceUnit - to always print the unit, even if the conversion factor is "1"
	 */
	public String getHumanReadable(double d, boolean forceUnit){
		long factor = 1;
		String unit = "";
		double converted = d;
		for(int c=0;c<conversionFactors.length;c++){
			factor *= conversionFactors[c];
			if(c<conversionFactors.length && converted<conversionFactors[c]){
				break;
			}
			unit = units[c+1][0];
			converted = d/factor;
		}
		if("".equals(unit)){
			outputFormat.setMinimumFractionDigits(0);
			if(forceUnit){
				unit = units[0][0];
			}
		}
		try{
			return outputFormat.format(converted)+unit;
		}
		finally {
			outputFormat.setMinimumFractionDigits(decimalDigits);
		}
	}

	public String getHumanReadable(double d){
		return getHumanReadable(d,false);
	}

	public void setMinimumDigits(int digits){
		outputFormat.setMinimumFractionDigits(digits);
	}

	int getConversionFactor(String unit){
		int i=0;
		boolean found=false;
		outer: for(String[] u: units){
			for(String u1: u){
				if(u1.startsWith(unit)){
					found=true;
					break outer;
				}
			}
			i++;
		}
		if(found){
			int result=1;
			for(int j=0;j<i;j++)result*=conversionFactors[j];
			return result;
		}
		else{
			throw new IllegalArgumentException("No conversion for unit '"+unit+"'");
		}
	}

	/**
	 * extract the numerical part of the argument
	 * @param valueWithUnits
	 */
	String getNumber(String valueWithUnits){
		Matcher m=valueWithUnitsPattern.matcher(valueWithUnits);
		if(m.matches()){
			return m.group(1);
		}
		else throw new NumberFormatException("not a parsable value: "+valueWithUnits);
	}

	/**
	 * extract the unit part of the argument
	 * @param valueWithUnits
	 */
	String getUnits(String valueWithUnits){
		Matcher m=valueWithUnitsPattern.matcher(valueWithUnits);
		if(m.matches()){
			return m.group(2);
		}
		else return null;
	}

	static String[][] capacityUnits={
		{"","bytes"},
		{"K","kilobytes","kb"}, 
		{"M","megabytes","mb"}, 
		{"G","gigabytes","gb"}, 
		{"T","terabytes","tb"}
	};

	static int[] capacityFactors=new int[]{1024,1024,1024,1024};

	static String[][] timeUnits={
		{"sec","seconds",},
		{"min","minutes",}, 
		{"h","hours",}, 
		{"d","days",}, 
	};

	static int[] timeFactors=new int[]{60,60,24};

	/**
	 * get a new parser instance suitable for parsing capacity units (K, M, G etc)
	 * @param decimalDigits
	 */
	public static UnitParser getCapacitiesParser(int decimalDigits){
		return new UnitParser(capacityUnits,capacityFactors,decimalDigits);
	}

	/**
	 * get a new parser instance suitable for parsing time units (seconds, minutes, hours, days)
	 * @param decimalDigits
	 */
	public static UnitParser getTimeParser(int decimalDigits){
		return new UnitParser(timeUnits,timeFactors,decimalDigits);
	}

	/**
	 * parses a time from the given string<br/>
	 * 
	 * understands a number of date/time formats such as ISO8601, HH:mm, etc
	 * 
	 * @param spec - the date specification
	 */
	public static Date extractDateTime(String spec){
		Calendar result=null;
		try{
			Calendar d1 = Calendar.getInstance();
			d1.setTime(getHHMMDate().parse(spec));
			Calendar c=Calendar.getInstance();
			c.set(Calendar.HOUR_OF_DAY, d1.get(Calendar.HOUR_OF_DAY));
			c.set(Calendar.MINUTE, d1.get(Calendar.MINUTE));
			c.set(Calendar.SECOND, 0);
			if(c.compareTo(Calendar.getInstance())<0){
				//interpret time as "on the next day"
				c.add(Calendar.DATE, 1);
			}
			result = c;
		}catch(ParseException pe){}
		if(result==null){
			try{
				Calendar c=Calendar.getInstance();
				c.setTime(getSimpleDateFormat().parse(spec));
				c.set(Calendar.SECOND, 0);
				result = c;
			}catch(ParseException pe){}
		}
		if(result==null){
			try{
				Calendar c = Calendar.getInstance();
				c.setTime(getISO8601().parse(spec));
				result = c;
			}catch(ParseException pe){}
		}
		if(result!=null)return result.getTime();
		else throw new IllegalArgumentException("Specified date string '"+spec+"'could not be parsed!");
	}

	public static String convertDateToISO8601(String dateSpec){
		return getISO8601().format(extractDateTime(dateSpec));
	}

	/**
	 * get an DateFormat instance for the "HH:mm" format<br/>
	 */
	public static DateFormat getHHMMDate(){
		return new SimpleDateFormat("HH:mm");
	}

	/**
	 * gets a DateFormat instance for the ISO8601 "yyyy-MM-dd'T'HH:mm:ssZ" format<br/>
	 */
	public static DateFormat getISO8601(){
		return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
	}

	/**
	 * get a DateFormat instance for the "yyyy-MM-dd HH:mm" format<br/>
	 */
	public static DateFormat getSimpleDateFormat(){
		return new SimpleDateFormat("yyyy-MM-dd HH:mm");
	}

}
