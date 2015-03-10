package edu.arizona.biosemantics.oto.common.model;

import java.util.List;

public class StructureHierarchy {

	private List<TermContext> termContexts;
	private String authenticationToken;
	
	public StructureHierarchy() { }
	
	public StructureHierarchy(List<TermContext> termContexts,
			String authenticationToken) {
		super();
		this.termContexts = termContexts;
		this.authenticationToken = authenticationToken;
	}

	public List<TermContext> getTermContexts() {
		return termContexts;
	}

	public void setTermContexts(List<TermContext> termContexts) {
		this.termContexts = termContexts;
	}

	public String getAuthenticationToken() {
		return authenticationToken;
	}

	public void setAuthenticationToken(String authenticationToken) {
		this.authenticationToken = authenticationToken;
	}
}