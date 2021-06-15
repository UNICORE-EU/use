package eu.unicore.services.ws;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.commons.codec.binary.Base64;
import org.apache.xmlbeans.XmlObject;
import org.oasisOpen.docs.wsrf.rl2.CurrentTimeDocument;
import org.oasisOpen.docs.wsrf.sg2.ServiceGroupRPDocument;

import de.fzj.unicore.wsrflite.InitParameters;
import de.fzj.unicore.wsrflite.xmlbeans.WSUtilities;
import eu.unicore.services.ws.impl.WSResourceImpl;
import eu.unicore.services.ws.renderers.FieldRenderer;
import eu.unicore.services.ws.renderers.ModifiableBase;
import eu.unicore.services.ws.renderers.ValueRenderer;

public class MockWSResourceImpl extends WSResourceImpl {



	public MockWSResourceImpl(){
		super();
		addRenderer(new FooRenderer(this));
		addRenderer(new FieldRenderer(this, CurrentTimeDocument.type.getDocumentElementName(), "tDoc"));
		addRenderer(new FieldRenderer(this, new QName("a","b"), "stringSet"));
		addRenderer(new FieldRenderer(this, new QName("array","b"), "integerArray"));
		addRenderer(new FieldRenderer(this, new QName("intarray","b"), "intArray"));
		addRenderer(new ModifyRenderer(this, new QName("tags","tag")));
	}

	
	@Override
	public MockResourceModel getModel() {
		return (MockResourceModel)super.getModel();
	}


	public List<String>getChildIDs(){
		return getModel().getChildIDs();
	}

	@Override
	public QName getResourcePropertyDocumentQName() {
		return ServiceGroupRPDocument.type.getDocumentElementName();
	}

	public static int SIZE=64;

	@Override
	public void initialise(InitParameters initobjs)
			throws Exception {
		MockResourceModel m = getModel();
		if(m==null){
			m=new MockResourceModel();
			setModel(m);
		}
		super.initialise(initobjs);

		//setup instance state
		ByteArrayOutputStream os=new ByteArrayOutputStream();
		os.write("<foo:data xmlns:foo=\"foo.org\">".getBytes());
		byte[] b=new byte[SIZE];
		new java.util.Random().nextBytes(b);
		byte[] base64=Base64.encodeBase64(b,true);
		os.write(base64);
		os.write("</foo:data>".getBytes());
		m.setFoo(XmlObject.Factory.parse(os.toString()));

		CurrentTimeDocument tDoc=CurrentTimeDocument.Factory.newInstance();
		tDoc.addNewCurrentTime().setCalendarValue(Calendar.getInstance());
		m.settDoc(tDoc);
		
		Set<String> stringSet=new HashSet<String>();
		stringSet.add("foo");
		stringSet.add("bar");
		stringSet.add("ham");
		m.setStringSet(stringSet);
		
		m.setIntegerArray(new Integer[]{1,2,3});

		m.setIntArray(new int[]{11,12,13});
		
		m.setTags(new HashSet<>());
		
	}

	public static class FooRenderer extends ValueRenderer{

		FooRenderer(MockWSResourceImpl parent){
			super(parent, new QName("foo","bar"));
		}

		@Override
		public XmlObject[] getValue() throws Exception {
			return new XmlObject[]{((MockWSResourceImpl)parent).getModel().getFoo()};
		}

	}

	public static class ModifyRenderer extends ModifiableBase<XmlObject>{
		MockWSResourceImpl ws;
		public ModifyRenderer(MockWSResourceImpl parent, QName name){
			super(name, parent, false, 0, 100);
			this.ws=parent;
		}

		@Override
		public XmlObject[] render() {
			Set<String>tags=ws.getModel().getTags();
			XmlObject[]result=new XmlObject[tags.size()];
			int i=0;
			for(String s: tags){
				result[i]=WSUtilities.createXmlDoc(qName, s, null);
				i++;
			}
			return result;
		}

		@Override
		public int getCurrentSize() {
			return ws.getModel().getTags().size();
		}

		@Override
		protected void doClear() {
			ws.getModel().getTags().clear();
		}

		@Override
		protected void doAdd(XmlObject o) {
			String content=WSUtilities.extractElementTextAsString(o);
			ws.getModel().getTags().add(content);
		}

		@Override
		public void updateDigest(MessageDigest md) throws Exception {
			for(XmlObject o: render()){
				md.update(String.valueOf(o).getBytes());
			}
		}
		
	}

}
