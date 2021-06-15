package eu.unicore.services.admin;

import java.util.Map;

import eu.unicore.services.Kernel;

public class MockAdminAction implements AdminAction {

	@Override
	public String getName() {
		return "mock";
	}
	
	@Override
	public String getDescription() {
		return "Echos all incoming parameters";
	}

	@Override
	public AdminActionResult invoke(Map<String, String> params, Kernel kernel) {
		AdminActionResult aar=new AdminActionResult(true, "ok");
		for(Map.Entry<String, String>e: params.entrySet()){
			aar.addResult(e.getKey(), "echo-"+e.getValue());
		}
		return aar;
	}

}
