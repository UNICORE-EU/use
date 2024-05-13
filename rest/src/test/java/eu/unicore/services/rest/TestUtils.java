package eu.unicore.services.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import com.google.common.collect.Lists;

import eu.unicore.services.rest.RESTUtils.HtmlBuilder;

public class TestUtils {
	
	@Test
	public void testHtmlBuilder(){
		HtmlBuilder b = new HtmlBuilder();
		fill(b);
		String s = b.build();
		System.out.println(s);
		assertTrue(s.contains("<h1>test123</h1>"));
		assertTrue(s.contains("bar\n"));
		assertTrue(s.contains("<ul>"));
		assertTrue(s.contains("</ul>"));
		assertTrue(s.contains("<li>foo</li>"));
		assertTrue(s.contains("<b>spam</b>"));
		assertTrue(s.contains("body"));
	}
	
	@Test
	public void testHtmlBuilderFragment(){
		HtmlBuilder b = new HtmlBuilder(true);
		fill(b);
		String s = b.build();
		assertFalse(s.contains("body"));
	}
	
	private void fill(HtmlBuilder b){
		b.h(1, "test123");
		b.text("bar").cr();
		b.ul();
		b.li().text("foo").end();
		b.end();
		b.bftext("spam");
		b.br();
	}
	
	@Test
	public void testJSONObject() throws Exception {
		String x = "{foo : 'bar', arr: ['a1'], obj: {a:b,}}";
		JSONObject o = new JSONObject(x);
		Object o1 = o.get("foo");
		assertTrue(o1 instanceof String);
		Object o2 = o.get("arr");
		assertTrue(o2 instanceof JSONArray);
		Object o3 = o.get("obj");
		assertTrue(o3 instanceof JSONObject);
	}
	
	@Test
	public void testTemplateExpansion() throws Exception {
		String template = "x=%foo, y=%bar";
		JSONObject context = new JSONObject("{foo: 123, bar: 456}");
		System.out.println(context.toString(2));
		String expanded = RESTUtils.expandTemplate(template, context);
		assertEquals("x=123, y=456", expanded);
	}

	@Test
	public void testEvaluateToString() throws Exception {
		Map<String, List<String>> attr = new HashMap<>();
		attr.put("a", Lists.asList("1", "2", new String[] {}));
		Map<String, Object> vars = new HashMap<>();
		vars.put("attr", attr);
		String expr = "attr['a'][0]";
		String res = RESTUtils.evaluateToString(expr, vars);
		System.out.println(res);
		assertEquals("1", res);
	}

	@Test
	public void testEvaluateToArray() throws Exception {
		Map<String, List<String>> attr = new HashMap<>();
		attr.put("a", Lists.asList("1", "2", new String[] {}));
		Map<String, Object> vars = new HashMap<>();
		vars.put("attr", attr);
		String expr = "attr['a']";
		String[] res = RESTUtils.evaluateToArray(expr, vars);
		System.out.println(Lists.newArrayList(res));
		assertEquals("1", res[0]);
		assertEquals("2", res[1]);

		expr = "123";
		res = RESTUtils.evaluateToArray(expr, vars);
		System.out.println(Lists.newArrayList(res));
		assertEquals("123", res[0]);
	}
}
