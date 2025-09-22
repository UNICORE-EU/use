package eu.unicore.services.rest.impl;

import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;

import eu.unicore.security.wsutil.SecuritySession;
import eu.unicore.security.wsutil.SecuritySessionUtils;
import eu.unicore.services.Home;
import eu.unicore.services.Resource;
import eu.unicore.services.security.util.AuthZAttributeStore;
import jakarta.servlet.http.HttpServletResponse;

public final class PostInvokeHandler extends AbstractPhaseInterceptor<Message> {

	private static final ThreadLocal<SecuritySession>threadSession = new ThreadLocal<>();

	public PostInvokeHandler() {
		super(org.apache.cxf.phase.Phase.PRE_STREAM);
	}

	@Override
	public void handleFault(Message message) {
		handleSession(message);
		cleanup(message, false);
	}

	@Override
	public void handleMessage(Message message) {
		handleSession(message);
		cleanup(message, true);
	}

	// check if we have a proper response
	// and set session ID info if possible
	private void handleSession(Message message){
		try{
			HttpServletResponse response = (HttpServletResponse)message.get("HTTP.RESPONSE");
			if(response != null){
				SecuritySession session = threadSession.get();
				if(session!=null){
					response.setHeader(SecuritySessionUtils.SESSION_ID_HEADER, session.getSessionID());
					response.setHeader(SecuritySessionUtils.SESSION_LIFETIME_HEADER, String.valueOf(session.getLifetime()));
					clearSession();
				}
			}
		}catch(Exception ex){}
	}

	private void cleanup(Message message, boolean storeChanges){
		AuthZAttributeStore.clear();
		Resource res = (Resource)message.getExchange().get(USERestInvoker.LOCKEDKEY);
		if(res != null){
			Home home = (Home)message.getExchange().get(USERestInvoker.HOME);
			if(storeChanges) {
				try{
					home.done(res);
				}catch(Exception ex){
					throw new RuntimeException(ex);
				}
			}
			else{
				try{
					home.getStore().unlock(res);
				}catch(Exception ex){
					throw new RuntimeException(ex);
				}
			}
			message.getExchange().remove(USERestInvoker.LOCKEDKEY);
		}
	}

	public static void setSession(SecuritySession session){
		threadSession.set(session);
	}

	public static void clearSession(){
		threadSession.remove();
	}

	public static SecuritySession getSession(){
		return threadSession.get();
	}
}
