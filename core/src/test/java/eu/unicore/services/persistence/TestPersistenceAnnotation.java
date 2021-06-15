package eu.unicore.services.persistence;

import java.util.List;

import eu.unicore.util.ConcurrentAccess;
import junit.framework.TestCase;

public class TestPersistenceAnnotation extends TestCase{

	protected void setUp(){
	}
	
	public void test1(){
		PersistenceSettings ps=PersistenceSettings.get(MockWSR1.class);
		assertTrue(ps.isConcurrentMethod("foo"));
		assertFalse(ps.isConcurrentMethod("bar"));
		assertTrue(ps.isLoadOnce());
		
	}

	public void testInherited(){
		PersistenceSettings ps=PersistenceSettings.get(MockWSR2.class);
		assertFalse(ps.isLoadOnce());
		assertTrue(ps.isConcurrentMethod("foo"));
		assertTrue(ps.isConcurrentMethod("bar"));
	}
	
	public void testSpecifyConcurrentMethods(){
		PersistenceSettings ps=PersistenceSettings.get(MockWSR1.class);
		assertTrue(ps.isLoadOnce());
		List<String>methods=ps.getConcurrentMethodNames();
		assertTrue(methods.contains("foo"));
		assertFalse(methods.contains("bar"));
		assertFalse(methods.contains("baz"));
		System.out.println(ps);
	}
	
	@Persistent(loadSemantics=LoadSemantics.LOAD_ONCE)
	public class MockWSR1{
		
		@ConcurrentAccess(allow=true)
		public void foo(){}
		@ConcurrentAccess
		public void bar(){}
		
		public void baz(){}
		
	}
	
	@Persistent(loadSemantics=LoadSemantics.NORMAL)
	public class MockWSR2 extends MockWSR1{
		
		@ConcurrentAccess(allow=true)
		public void bar(){}
	}
	
}

