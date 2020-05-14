package de.fzj.unicore.wsrflite.utils.deployment;

/**
 * disabled in code
 */
public class DemoFeature2 extends FeatureImpl {

	public static boolean initWasRun = false; 
	public static String NAME = "demo2";
	
	public DemoFeature2() {
		this.name = "demo2";
		getInitTasks().add(new Runnable(){
			public void run(){
				System.out.println("This is feature <demo2>.");
				initWasRun = true;
			}
		});
	}

	public boolean isEnabled(){
		return false;
	}
}
