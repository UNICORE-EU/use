package eu.unicore.services.utils;

import java.util.HashMap;
import java.util.function.Function;

public class LoadingMap<K,V> extends HashMap<K,V>{

	private static final long serialVersionUID = 1L;

	private final Function<K, V> loader;

	public LoadingMap(Function<K, V> loader) {
		this.loader = loader;
	}

	@Override
	public synchronized V get(Object key) {
		V value = super.get(key);
		if(value==null) {
			@SuppressWarnings("unchecked")
			K _key = (K)key;
			value = loader.apply(_key);
			if(value!=null)super.put(_key,  value);
		}
		return value;
	}

}