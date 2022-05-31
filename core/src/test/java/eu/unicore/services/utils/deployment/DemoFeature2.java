package eu.unicore.services.utils.deployment;

/**
 * disabled in code
 */
public class DemoFeature2 extends FeatureImpl {

	public static boolean initWasRun = false; 
	public static String NAME = "demo2";
	
	public DemoFeature2() {
		this.name = "demo2";
	}
	
	@Override
	public void initialise() throws Exception {
		System.out.println("This is feature <demo2>.");
		initWasRun = true;
	}

	public boolean isEnabled(){
		return false;
	}
}
