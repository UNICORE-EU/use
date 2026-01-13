package eu.unicore.services.messaging.impl;

import eu.unicore.services.messaging.Message;

public class StringMessage extends Message {

	private static final long serialVersionUID = 1L;

	private final String body;

	public StringMessage(String body) {
		super();
		this.body = body;
	}

	@Override
	public String toString() {
		return body;
	}

}