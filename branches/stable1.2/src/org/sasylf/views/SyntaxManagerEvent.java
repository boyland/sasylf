package org.sasylf.views;

import java.util.EventObject;

public class SyntaxManagerEvent extends EventObject {

	private static final long serialVersionUID = 3697053173951102953L;

	   private final ISyntaxItem[] added;
	   private final ISyntaxItem[] removed;



	   public SyntaxManagerEvent(
	      SyntaxManager source,
	      ISyntaxItem[] itemsAdded, ISyntaxItem[] itemsRemoved
	   ) {
	      super(source);
	      added = itemsAdded;
	      removed = itemsRemoved;
	   }

	   public ISyntaxItem[] getItemsAdded() {
	      return added;
	   }

	   public ISyntaxItem[] getItemsRemoved() {
	      return removed;
	   }



}
