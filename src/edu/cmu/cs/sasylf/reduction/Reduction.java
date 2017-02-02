package edu.cmu.cs.sasylf.reduction;

public enum Reduction {
	LESS("<"),
	EQUAL("="),
	NONE("?");

	public String getOperator() { return name; }

	private String name;
	private Reduction(String n) {
		name = n;
	}  
}
