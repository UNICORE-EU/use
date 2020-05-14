package de.fzj.unicore.wsrflite.utils.deployment;

public class DemoFeature extends FeatureImpl {

	public static boolean initWasRun = false; 
	public static String NAME = "demo";
	
	public DemoFeature() {
		this.name = NAME;
		getInitTasks().add(new Runnable(){
			public void run(){
				System.out.println("This is feature <demo2>.");
				initWasRun = true;
			}
		});
	}


}
