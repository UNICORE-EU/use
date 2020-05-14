package eu.unicore.services.rest;

import static junit.framework.Assert.assertEquals;

import org.junit.Test;

public class TestUtils {

	@Test
	public void testConvertWSRFURLs(){
		String[] wsrf = {
				"https://foo:8080/TEST/services/StorageManagement?res=default_storage",
				"https://foo:8080/TEST/services/TargetSystemFactoryService?res=default_target_system_factory",
				"https://foo:8080/TEST/services/StorageFactory?res=default_storage_factory",
				};
		String[] rest = {
				"https://foo:8080/TEST/rest/core/storages/default_storage",
				"https://foo:8080/TEST/rest/core/factories/default_target_system_factory",
				"https://foo:8080/TEST/rest/core/storagefactories/default_storage_factory",
				};
		
		for(int i=0; i<wsrf.length; i++){
			assertEquals("WSRF-to-REST URL conversion", rest[i], Registries.convertToREST(wsrf[i]));
		}
	}
	
	
}
