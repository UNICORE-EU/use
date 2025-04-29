package eu.unicore.services.aip.xuudb;

import eu.unicore.xuudb.interfaces.IDynamicAttributesPublic;
import eu.unicore.xuudb.xbeans.GetAttributesRequestDocument;
import eu.unicore.xuudb.xbeans.GetAttributesResponseDocument;
import eu.unicore.xuudb.xbeans.GetAttributesResponseType;
import eu.unicore.xuudb.xbeans.SimulateGetAttributesRequestDocument;
import eu.unicore.xuudb.xbeans.SimulateGetAttributesResponseDocument;

public class MockDAP implements IDynamicAttributesPublic {

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
