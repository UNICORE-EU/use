package eu.unicore.services.pdp.local;

import java.util.List;

import org.herasaf.xacml.core.policy.Evaluatable;

/**
 * Used to listen about policy changes.
 * @author golbi
 *
 */
public interface PolicyListener
{
	public void updateConfiguration(List<Evaluatable> policies, String algorithm);
}
