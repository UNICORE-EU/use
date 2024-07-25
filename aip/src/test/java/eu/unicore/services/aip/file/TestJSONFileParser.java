package eu.unicore.services.aip.file;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

public class TestJSONFileParser {

	private static final JSONObject OK = new JSONObject();
	static {
		JSONObject e1 = new JSONObject();
		e1.put("role", "user");
		e1.put("xlogin", new JSONArray("['somebody', 'nobody']"));
		OK.put("CN=Stanisław Lem, C=PL", e1);
		JSONObject e2 = new JSONObject();
		e2.put("role", "user");
		OK.put("CN=Ernst Grünfeld, C=AT", e2);
	}

	@Test
	public void testOK() throws Exception
	{
		Map<String, List<Attribute>> map = new JSONFileParser().parse(OK);
		assertEquals(2, map.size());
		System.out.println(map);
	}
}
