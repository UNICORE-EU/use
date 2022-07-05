package eu.unicore.services.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

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

}
