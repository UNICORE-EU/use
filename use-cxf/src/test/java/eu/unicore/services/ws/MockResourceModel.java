package eu.unicore.services.ws;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.xmlbeans.XmlObject;
import org.oasisOpen.docs.wsrf.rl2.CurrentTimeDocument;

import eu.unicore.services.impl.BaseModel;

public class MockResourceModel extends BaseModel {

	private static final long serialVersionUID = 1L;

	private final List<String>childIDs=new ArrayList<String>();

	XmlObject foo;

	Set<String> tags;

	CurrentTimeDocument tDoc;

	Set<String> stringSet;

	Integer[] integerArray;

	int[] intArray;

	public XmlObject getFoo() {
		return foo;
	}

	public void setFoo(XmlObject foo) {
		this.foo = foo;
	}

	public Set<String> getTags() {
		return tags;
	}

	public void setTags(Set<String> tags) {
		this.tags = tags;
	}

	public CurrentTimeDocument gettDoc() {
		return tDoc;
	}

	public void settDoc(CurrentTimeDocument tDoc) {
		this.tDoc = tDoc;
	}

	public Set<String> getStringSet() {
		return stringSet;
	}

	public void setStringSet(Set<String> stringSet) {
		this.stringSet = stringSet;
	}

	public Integer[] getIntegerArray() {
		return integerArray;
	}

	public void setIntegerArray(Integer[] integerArray) {
		this.integerArray = integerArray;
	}

	public int[] getIntArray() {
		return intArray;
	}

	public void setIntArray(int[] intArray) {
		this.intArray = intArray;
	}

	public List<String> getChildIDs() {
		return childIDs;
	}
	
	
}
