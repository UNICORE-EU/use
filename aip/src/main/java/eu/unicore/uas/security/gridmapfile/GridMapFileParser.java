package eu.unicore.uas.security.gridmapfile;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.logging.log4j.Logger;

import eu.emi.security.authn.x509.impl.OpensslNameUtils;
import eu.unicore.services.utils.Pair;
import eu.unicore.util.Log;


/**
 * Utility class to parse Globus-style grid map file
 * 
 * @author bdemuth
 */
public class GridMapFileParser
{
	
	private static final Logger logger = Log.getLogger(Log.SECURITY, GridMapFileAuthoriser.class);
	
	private final File gridMapFile;

	private static final String COMMENT_CHARS = "#";
	
	private int numDuplicates;
	private int numEmpty;
	private int numError;

	public GridMapFileParser(File gridMapFile)
	{
		this.gridMapFile = gridMapFile;
	}

	public Map<String,List<String>> parse()
	{
		
		Map<String,List<String>> result = new HashMap<>();
	
		try(BufferedReader br = new BufferedReader(new InputStreamReader(new DataInputStream(new FileInputStream(gridMapFile)))))
				{
			String line;
			
			while ((line = br.readLine()) != null)   {
				try {
					line = line.trim();
					if ( (line.length() == 0) ||
							( COMMENT_CHARS.indexOf(line.charAt(0)) != -1) ) {
							numEmpty++;
						continue;
					}

					Pair<String, String[]> mapping = parseLine(line);
					if(mapping != null)
					{
						String key = mapping.getM1();

						List<String> logins = result.get(key);
						if(logins == null)
						{
							logins = new ArrayList<String>();
							result.put(key,logins);
						}
						String[] ids = mapping.getM2();
						for(String id : ids)
						{
							if(logins.contains(id))
							{
								numDuplicates++;
							}
							else
							{
								logins.add(id);
							}
						}
					}
					else 
					{
						numError ++;
					}
					
				} catch (Exception e) {
					logger.warn("Problem while parsing line "+line+" from grid-mapfile "+gridMapFile.getAbsolutePath()+": "+e.getMessage(),e);
					numError ++;
				}
			}

		} catch (Exception e){
			logger.warn("Problem while parsing grid-mapfile "+gridMapFile.getAbsolutePath()+": "+e.getMessage(),e);
		}
		return result;
	}

	public static Pair<String, String[]> parseLine(String line) throws IOException
	{

		QuotedStringTokenizer tokenizer;
		StringTokenizer idTokenizer;

		tokenizer = new QuotedStringTokenizer(line);

		String dn = null;

		if (tokenizer.hasMoreTokens()) {
			dn = tokenizer.nextToken();
		} else {
			throw new IOException("Line does not contain user DN!");

		}

		String xlogins = null;

		if (tokenizer.hasMoreTokens()) {
			xlogins = tokenizer.nextToken();
		} else {
			throw new IOException("Line does not contain xlogin!");

		}

		idTokenizer = new StringTokenizer(xlogins, ",");
		String [] ids = new String [ idTokenizer.countTokens() ];
		int i = 0;
		while(idTokenizer.hasMoreTokens()) {
			ids[i++] = idTokenizer.nextToken();
		}

		String normalizedDN = OpensslNameUtils.normalize(dn);
		if(normalizedDN == null) throw new IOException("Invalid distinguished name: "+dn);
		return new Pair<String,String[]>(normalizedDN,ids);
	}

	public int getNumDuplicates() {
		return numDuplicates;
	}

	public int getNumEmpty() {
		return numEmpty;
	}

	public int getNumError() {
		return numError;
	}


	

}
