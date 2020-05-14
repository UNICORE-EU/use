package de.fzj.unicore.wsrflite.testservice;

import de.fzj.unicore.wsrflite.utils.deployment.DeploymentDescriptorImpl;

public class MockServiceDescriptor extends DeploymentDescriptorImpl {

	public MockServiceDescriptor(){
		this.name = "test";
		this.type = MockService.TYPE;
		this.implementationClass = MockHome.class;
		this.interfaceClass = IMock.class;
	}
	
}
