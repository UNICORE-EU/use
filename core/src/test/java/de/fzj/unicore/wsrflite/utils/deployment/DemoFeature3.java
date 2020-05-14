package de.fzj.unicore.wsrflite.utils.deployment;

/**
 * disabled via config
 */
public class DemoFeature3 extends FeatureImpl {

	public static boolean initWasRun = false; 
	public static String NAME = "demo3";
	
	public DemoFeature3() {
		this.name = "demo3";
		getInitTasks().add(new Runnable(){
			public void run(){
				System.out.println("This is feature <demo3>.");
				initWasRun = true;
			}
		});
	}

}
