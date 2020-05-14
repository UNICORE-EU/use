package de.fzj.unicore.wsrflite.admin.service;

import java.util.Collection;

import de.fzj.unicore.wsrflite.Resource;
import de.fzj.unicore.wsrflite.admin.AdminAction;
import de.fzj.unicore.wsrflite.xmlbeans.AdminActionDocument;
import de.fzj.unicore.wsrflite.xmlbeans.renderers.ValueRenderer;

/**
 * publish names/descriptions of {@link AdminAction}s
 */
public class AdminActionsRenderer extends ValueRenderer {
	
	public AdminActionsRenderer(Resource parent){
		super(parent, AdminActionDocument.type.getDocumentElementName());
	}
	
	@Override
	protected AdminActionDocument[] getValue() {
		Collection<AdminAction> actions=parent.getKernel().getAdminActions().values();
		AdminActionDocument[] xmls=new AdminActionDocument[actions.size()];
		int i=0;
		for(AdminAction action: actions){
			xmls[i]=AdminActionDocument.Factory.newInstance();
			xmls[i].addNewAdminAction().setName(action.getName());
			xmls[i].getAdminAction().setDescription(action.getDescription());
			i++;
		}
		return xmls;
	}

}
