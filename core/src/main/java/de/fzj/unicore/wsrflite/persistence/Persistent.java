package de.fzj.unicore.wsrflite.persistence;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * annotate ws-resource home implementation class with persistence settings
 * 
 * @author schuller
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Persistent {

	/**
	 * load semantics: "load once" or normal
	 * 
	 * "Load once" means: the instance is kept in memory, but changes are written to disk
	 * as well (in case you are using disk persistence)
	 * 
	 * "Normal" means: the instance is not kept in memory, but reloaded from disk 
	 * on each request 
	 */
	public LoadSemantics loadSemantics() default LoadSemantics.NORMAL;

}
