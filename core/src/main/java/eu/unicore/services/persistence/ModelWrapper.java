package eu.unicore.services.persistence;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Type;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import eu.unicore.persist.util.GSONConverter;
import eu.unicore.services.Model;
import eu.unicore.util.Log;

/**
 * helper to store Model info without losing the model class information
 *
 * @author schuller
 */
public class ModelWrapper implements Serializable {

	private static final long serialVersionUID = 1L;

	private String className;

	private Model model;

	public ModelWrapper(Model model){
		this.model = model;
		this.className  = model!=null ? model.getClass().getName() : null;
	}

	public String getClassName() {
		return className;
	}

	public Model getModel() {
		return model;
	}

	public static class Adapter implements GSONConverter{

		@Override
		public Type getType() {
			return ModelWrapper.class;
		}

		@Override
		public Object[] getAdapters() {
			return new Object[]{adapter};
		}

		@Override
		public boolean isHierarchy(){
			return true;
		}
	}

	public static class XBeanAdapter implements GSONConverter{

		@Override
		public Type getType() {
			return XmlObject.class;
		}

		@Override
		public Object[] getAdapters() {
			return new Object[]{xBeanAdapter};
		}

		@Override
		public boolean isHierarchy(){
			return true;
		}
	}

	private static final ModelAdapter adapter=new ModelAdapter();

	public static class ModelAdapter implements JsonDeserializer<ModelWrapper>{

		@Override
		public ModelWrapper deserialize(JsonElement json, Type typeOfT,
				JsonDeserializationContext context)
						throws JsonParseException {
			String className = json.getAsJsonObject().get("className").getAsString();
			if(className.startsWith("de.fzj.unicore.uas.")) {
			    // ugly but simple - U9 to U10 changed package names in UNICORE/X
			    className = className.replace("de.fzj.unicore.uas.", "eu.unicore.uas.");
            }
			try{
				Class<?>modelClass = Class.forName(className);
				Model model =  context.deserialize(json.getAsJsonObject().get("model"),modelClass);
				return new ModelWrapper(model);
			}catch(ClassNotFoundException cne) {
				throw new JsonParseException("Unknown model class "+className, cne);
			}
		}
	}

	private static final XmlBeansAdapter xBeanAdapter=new XmlBeansAdapter();

	public static class XmlBeansAdapter implements JsonSerializer<XmlObject>, JsonDeserializer<XmlObject>{

		@Override
		public JsonElement serialize(XmlObject src, Type typeOfSrc,
				JsonSerializationContext context) {
			ByteArrayOutputStream bos=new ByteArrayOutputStream();
			try{
				src.save(bos);
				return new JsonPrimitive(bos.toString());
			}
			catch(IOException io){
				// can't happen
				return new JsonPrimitive("failed: "+Log.getDetailMessage(io));
			}
		}

		@Override
		public XmlObject deserialize(JsonElement json, Type typeOfT,
				JsonDeserializationContext context)
						throws JsonParseException {
			try{
				return XmlObject.Factory.parse(json.getAsString());
			}catch(XmlException xe){
				throw new JsonParseException(xe);
			}
		}
	}

}
