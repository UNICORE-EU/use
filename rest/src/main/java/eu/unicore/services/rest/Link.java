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

	@Override
	public int hashCode() {
		return this.href.hashCode();
	}

	@Override
	public boolean equals(Object other) {
		if(other==null || !(other instanceof Link))return false;
		return this.href.equals(((Link)other).href);
	}
}
