package eu.unicore.services.utils.deployment;

public class DemoFeature extends FeatureImpl {

	public static boolean initWasRun = false; 
	public static String NAME = "demo";
	
	public DemoFeature() {
		this.name = NAME;
		getStartupTasks().add(()->{
			System.out.println("This is feature <demo2>.");
			initWasRun = true;
		});
	}

}
