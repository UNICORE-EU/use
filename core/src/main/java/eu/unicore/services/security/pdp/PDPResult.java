package eu.unicore.services.security.pdp;

/**
 * Wraps a decision which was produced by PDP. UNCLEAR status means DENY,
 * but additionally some extra logging may be performed.  
 * @author golbi
 */
public class PDPResult
{
	public enum Decision {PERMIT, DENY, UNCLEAR};
	
	private final Decision decision;
	private final String message;
	
	public PDPResult(Decision decision, String message)
	{
		super();
		this.decision = decision;
		this.message = message;
	}
	
	public Decision getDecision()
	{
		return decision;
	}
	
	public String getMessage()
	{
		return message;
	}
}
