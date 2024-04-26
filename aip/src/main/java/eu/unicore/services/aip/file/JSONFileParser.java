package eu.unicore.services.aip.file;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Utility class to parse JSON file with attributes.
 *
 * Format is a JSON file with entries of the form
 * <pre>
 *  {
 *   "dn1" : {
 *     "attribute1" : "value",
 *     "attribute2" : ["val1", ... "valN" ]
 *   },
 *   "dn2" : { ... }
 *  }
 * </pre>
 * where each attribute can have a single value or a list of values
 * 
 * @author schuller
 */
public class JSONFileParser implements IFileParser
{

	public Map<String, List<Attribute>> parse(InputStream is) throws IOException
	{
		return parse(new JSONObject(IOUtils.toString(is, "UTF-8")));
	}

	public Map<String, List<Attribute>> parse(JSONObject source) throws JSONException
	{
		Map<String, List<Attribute>> ret = new LinkedHashMap<>();
		for(String key: source.keySet()) {
			JSONObject spec = source.getJSONObject(key);
			List<Attribute> attributes = new ArrayList<>();
			for(String attributeName: spec.keySet()) {
				List<String> values = new ArrayList<>();
				Attribute toAdd = new Attribute(attributeName, values);
				//values can be a list or a single value
				JSONArray arr = spec.optJSONArray(attributeName);
				if(arr!=null) {
					for(int i=0; i<arr.length(); i++) {
						values.add(arr.getString(i));
					}
				}
				else {
					values.add(spec.getString(attributeName));
				}
				attributes.add(toAdd);
			}			
			ret.put(key, attributes);
		}

		return ret;
	}

}
