package eu.unicore.services.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import eu.unicore.services.ExternalSystemConnector.Status;
import eu.unicore.util.Pair;

public class TestExtConnectorHelper {

	@Test
	public void testDirect() {
		Callable<Pair<Boolean,String>> checker = () -> {
			try{
				Thread.sleep(1000);
			}catch(InterruptedException ie) {}
			return new Pair<>(true, "Fine");
		};
		ExternalConnectorHelper ech = new ExternalConnectorHelper();
		ech.setExternalSystemName("test");
		ech.setCheckSupplier(checker);
		assertEquals("test", ech.getExternalSystemName());
		// without an executor, we get the status updates immediately...
		assertEquals("Fine", ech.getConnectionStatusMessage());
		assertEquals(Status.OK, ech.getConnectionStatus());
	}

	@Test
	public void testAsync() throws Exception {
		Callable<Pair<Boolean,String>> checker = () -> {
			try{
				Thread.sleep(1000);
			}catch(InterruptedException ie) {}
			return new Pair<>(true, "Fine");
		};
		ExternalConnectorHelper ech = new ExternalConnectorHelper();
		ech.setExternalSystemName("test");
		ech.setCheckService(Executors.newFixedThreadPool(1));
		ech.setCheckSupplier(checker);
		assertEquals("test", ech.getExternalSystemName());
		assertEquals("N/A", ech.getConnectionStatusMessage());
		assertEquals(Status.UNKNOWN, ech.getConnectionStatus());
		Thread.sleep(2000);
		// ... a few moments later ...
		assertEquals("Fine", ech.getConnectionStatusMessage());
		assertEquals(Status.OK, ech.getConnectionStatus());
	}

	@Test
	public void testAsyncAndWait() throws Exception {
		Callable<Pair<Boolean,String>> checker = () -> {
			try{
				Thread.sleep(1000);
			}catch(InterruptedException ie) {}
			return new Pair<>(true, "Fine");
		};
		ExternalConnectorHelper ech = new ExternalConnectorHelper();
		ech.setExternalSystemName("test");
		ech.setCheckService(Executors.newFixedThreadPool(1));
		ech.setCheckSupplier(checker);
		assertEquals("test", ech.getExternalSystemName());
		ech.awaitConnectionStatusRefresh(2, TimeUnit.SECONDS);
		assertEquals("Fine", ech.getConnectionStatusMessage());
		assertEquals(Status.OK, ech.getConnectionStatus());
	}

}
