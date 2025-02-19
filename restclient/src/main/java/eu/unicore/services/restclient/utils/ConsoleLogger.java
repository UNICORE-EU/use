package eu.unicore.services.restclient.utils;

import org.apache.logging.log4j.message.FormattedMessage;

public class ConsoleLogger implements UserLogger {

	protected boolean verbose = true;
	protected boolean debug = false;

	@Override
	public void info(String msg, Object... params) {
		System.out.println(new FormattedMessage(msg, params).getFormattedMessage());
	}

	@Override
	public void verbose(String msg, Object... params) {
		if(verbose)info(msg,params);
	}

	@Override
	public void debug(String msg, Object... params) {
		if(debug)info(msg,params);
	}

}
