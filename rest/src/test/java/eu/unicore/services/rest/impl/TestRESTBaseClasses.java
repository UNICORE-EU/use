package eu.unicore.services.rest.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import eu.unicore.services.rest.Link;

public class TestRESTBaseClasses {

	@Test
	public void testPropertiesMap() throws Exception {
		var r = new RESTRendererBase() {
			@Override
			protected boolean wantProperty(String k) {
				return k.startsWith("1");
			}
		};
		Map<String,Object> m = r.getProperties();
		m.put("1", "foo");
		m.put("11", (Supplier<Object>)()->"bar");
		m.put("2", "spam");
		m.put("22", (Supplier<Object>)()->"ham");
		assertEquals("foo", m.get("1"));
		assertEquals("bar", m.get("11"));
		assertFalse(m.containsKey("2"));
	}


	@Test
	public void testLink() throws Exception {
		var l1 = new Link("foo","https://1.org", "");
		var l2 = new Link("foo2","https://1.org", "2");
		assertEquals(l1, l2);
		assertEquals(l1.hashCode(), l2.hashCode());
	}

}
