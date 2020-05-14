package eu.unicore.services.rest;

import static org.junit.Assert.*;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.junit.Test;

public class TestPagingHelper {

	@Test
	public void testPagingHelper() throws Exception {
		PagingHelper ph = new PagingHelper("http://base", "http://base", "sub");
		List<String>uids = new ArrayList<>();
		for(int i=0;i<47; i++){
			uids.add(String.valueOf(i));
		}
		JSONObject o1 = ph.renderJson(0, 11, uids);
		String next = getLinkNext(o1);
		String prev = getLinkPrev(o1);
		assertNull(prev);
		assertEquals(11,getOffset(next));
		assertEquals(11,getNum(next));
		
		o1 = ph.renderJson(2, 11, uids);
		next = getLinkNext(o1);
		prev = getLinkPrev(o1);
		assertNotNull(prev);
		assertEquals(13,getOffset(next));
		assertEquals(11,getNum(next));
		assertEquals(0,getOffset(prev));
		assertEquals(11,getNum(prev));
		
		o1 = ph.renderJson(40, 11, uids);
		System.out.println(o1);
		next = getLinkNext(o1);
		prev = getLinkPrev(o1);
		assertNull(next);
		assertNotNull(prev);
		assertEquals(29,getOffset(prev));
		assertEquals(11,getNum(prev));
		
	}
	
	private String getLinkNext(JSONObject o) {
		try{
			return o.getJSONObject("_links").getJSONObject("next").getString("href");
		}catch(Exception e){
			return null;
		}
	}
	
	private String getLinkPrev(JSONObject o) {
		try{
			return o.getJSONObject("_links").getJSONObject("previous").getString("href");
		}catch(Exception e){
			return null;
		}
	}
	
	private int getOffset(String href){
		try{
			return Integer.valueOf(new URL(href).getQuery().split("&")[0].split("=")[1]);
		}
		catch(Exception e){
			return -1;
		}
	}
	
	private int getNum(String href){
		try{
			return Integer.valueOf(new URL(href).getQuery().split("&")[1].split("=")[1]);
		}
		catch(Exception e){
			return -1;
		}
	}
}
