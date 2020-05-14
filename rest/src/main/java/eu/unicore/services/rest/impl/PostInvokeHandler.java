package eu.unicore.services.rest.impl;

import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;

import de.fzj.unicore.wsrflite.Home;
import de.fzj.unicore.wsrflite.Resource;
import de.fzj.unicore.wsrflite.security.util.AuthZAttributeStore;
import eu.unicore.security.wsutil.SecuritySession;
import eu.unicore.security.wsutil.SecuritySessionUtils;

public class PostInvokeHandler extends AbstractPhaseInterceptor<Message> {

	private static final ThreadLocal<SecuritySession>threadSession=new ThreadLocal<SecuritySession>();

	public PostInvokeHandler() {
		super(org.apache.cxf.phase.Phase.PRE_STREAM);
	}

	@Override
	public void handleFault(Message message) {
		handleSession(message);
		cleanup(message, false);
	}


	public void handleMessage(Message message) {
		handleSession(message);
		cleanup(message, true);
	}

	// check if we have a proper response
	// and set session ID info if possible
	private static void handleSession(Message message){
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
		}catch(Exception ex){
			
		}
	}

	public static void cleanup(Message message, boolean storeChanges){
		AuthZAttributeStore.clear();

		Resource res = (Resource)message.getExchange().get(USERestInvoker.LOCKEDKEY);
		if(res != null){
			Home home = (Home)message.getExchange().get(USERestInvoker.HOME);
			if(storeChanges && !res.isDestroyed()) {
				try{
					home.persist(res);
				}catch(Exception ex){
					throw new RuntimeException(ex);
				}
			}
			else{
				home.getStore().unlock(res);
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
