package eu.unicore.services.testservice;

import eu.unicore.services.utils.deployment.DeploymentDescriptorImpl;

public class MockServiceDescriptor extends DeploymentDescriptorImpl {

	public MockServiceDescriptor(){
		this.name = "test";
		this.type = MockService.TYPE;
		this.implementationClass = MockHome.class;
		this.interfaceClass = IMock.class;
	}
	
}
