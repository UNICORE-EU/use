package eu.unicore.services.rest.testservice;

import eu.unicore.services.impl.BaseModel;

public class CounterModel extends BaseModel {

	private static final long serialVersionUID = 1L;

	private int counter=0;

	public int getCounter() {
		return counter;
	}

	public void setCounter(int counter) {
		this.counter = counter;
	}
	
	
}
