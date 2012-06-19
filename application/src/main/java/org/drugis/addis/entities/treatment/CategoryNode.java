package org.drugis.addis.entities.treatment;

public class CategoryNode implements DecisionTreeNode {
	private final String d_name;

	public CategoryNode(String name) {
		d_name = name;
	}
	
	public String getName() {
		return d_name;
	}
	
	@Override
	public DecisionTreeNode decide(Object object) {
		return null;
	}

	@Override
	public boolean isLeaf() {
		return true;
	}
}