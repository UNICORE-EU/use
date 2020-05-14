package eu.unicore.services.rest;

public class Link {

	private final String href, description, relation;
	
	public Link( String relation, String href, String description){
		this.relation = relation;
		this.href = href;
		this.description = description;
	}

	public Link( String relation, String href){
		this(relation,href,null);
	}
	
	public String getHref() {
		return href;
	}

	public String getDescription() {
		return description;
	}

	public String getRelation() {
		return relation;
	}
	
}
