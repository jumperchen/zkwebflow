/* Flow.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		May 19, 2009 6:47:30 PM, Created by henrichen
}}IS_NOTE

Copyright (C) 2009 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under GPL Version 2.0 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
*/

package org.zkoss.zwf;

import java.util.Map;

import org.zkoss.zk.ui.Component;

/**
 * A Flow in ZK Web Flow.
 * @author henrichen
 *
 */
public interface Flow extends FlowComponent {
	/**
	 * Returns the top flow of this flow; might be itself.
	 * @return the top flow of this flow; might be itself.
	 */
	public Flow getTopFlow();
	
	/**
	 * Returns the associated scope of this flow.
	 * @return the associated scope of this flow.
	 */
	public Map getFlowScope();

	/**
	 * Returns the parent flow of this flow; might be null if this is a top flow.
	 * @return the parent flow of this flow; might be null if this is a top flow.
	 */
	public Flow getParentFlow();
	
	/**
	 * Returns the flash scope of this flow. Note that flash scope will clear 
	 * itself whenever enter a new state.
	 * @return the flash scope of this flow.
	 */
	public Map getFlashScope();
	
	/**
	 * Returns whether this flow is a sub-flow (i.e. not a top flow).
	 * @return whether this flow is a sub-flow (i.e. not a top flow).
	 */
	public boolean isSubflow();
	
	/**
	 * Returns the associated <subflow-state> of this flow;
	 * might be null if this flow is not a sub-flow (i.e. is a top flow).
	 * @return the associated <subflow-state> of this flow 
	 */
	public SubflowState getSubflowState();
	
	/**
	 * Returns the root component of the associated working view.
	 * @return  the root component of the associated working view.
	 */
	public Component getView();
	
	/**
	 * Returns the {@link FlowComponent} of the specified id in this flow and 
	 * then search in parent flow of this flow if cannot find in this flow, and 
	 * so on, until reach the top flow.
	 * @return the {@link FlowComponent} of the specified id. 
	 */
	public FlowComponent getFlowComponent(String name);
	
	/**
	 * Returns the current state of this flow.
	 * @return the current state of this flow.
	 */
	public State getCurrentState();
	
	/**
	 * Returns the {@link Map} of the {@link State}s belongs to this {@link Flow}.
	 * @return
	 */
	public Map getChildren();
}
