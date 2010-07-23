/* Transition.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		May 19, 2009 7:04:32 PM, Created by henrichen
}}IS_NOTE

Copyright (C) 2009 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under GPL Version 2.0 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
*/

package org.zkoss.zwf;

/**
 * A Transition from state to state in a flow.
 * @author henrichen
 *
 */
public interface Transition extends FlowComponent {
	/**
	 * Returns the next view to go for this transition.
	 * @return the next view to go for this transition.
	 */
	public String getTo();
	
	/**
	 * Returns the hint (yes | no | clear) for bookmarking current State.
	 * <ol>
	 *  <li>yes: will bookmark current state (default).</li>
	 *  <li>no: will NOT bookmark current state.</li>
	 *  <li>clear: clear all bookmarks of this flow.</li>
	 * </ol>
	 * 
	 * @return the hint (yes | no | clear) for bookmarking current State
	 */
	public String getBookmark();
	
	/**
	 * Returns the associated {@link State} of this transition.
	 * @return the associated {@link State} of this transition.
	 */
	public State getState();
}
