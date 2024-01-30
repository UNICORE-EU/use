package eu.unicore.services.aip.xuudb;


import de.fzJuelich.unicore.xuudb.GetAttributesRequestDocument;
import de.fzJuelich.unicore.xuudb.GetAttributesResponseDocument;
import de.fzJuelich.unicore.xuudb.GetAttributesResponseType;
import de.fzJuelich.unicore.xuudb.SimulateGetAttributesRequestDocument;
import de.fzJuelich.unicore.xuudb.SimulateGetAttributesResponseDocument;
import eu.unicore.xuudb.interfaces.IDynamicAttributesPublic;

public class MockDAP implements IDynamicAttributesPublic{

	int callCount=0;
	String gid;
	String xlogin;
	String[] supplementaryGids;

	@Override
	public GetAttributesResponseDocument getAttributes(
			GetAttributesRequestDocument xml) {
		callCount++;
		GetAttributesResponseDocument doc=GetAttributesResponseDocument.Factory.newInstance();
		
		GetAttributesResponseType resp = doc.addNewGetAttributesResponse();
		resp.setGid(gid);
		resp.setXlogin(xlogin);
		resp.setSupplementaryGidsArray(supplementaryGids);
		
		
		
		return doc;
	}

	@Override
	public SimulateGetAttributesResponseDocument simulateGetAttributes(
			SimulateGetAttributesRequestDocument xml) {
		
		return null;
	}

}
