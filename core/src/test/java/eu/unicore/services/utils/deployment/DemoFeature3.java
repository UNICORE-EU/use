package eu.unicore.services.utils.deployment;

public class DemoFeature3 extends FeatureImpl {

	public static boolean initWasRun = false; 
	public static String NAME = "demo3";
	
	public DemoFeature3() {
		super(NAME);
		getStartupTasks().add( () -> {
				System.out.println("This is feature <demo3>.");
				initWasRun = true;
			});
	}
}
