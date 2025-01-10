package eu.unicore.services.rest.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

public class TestRESTRendererBase {

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
}
