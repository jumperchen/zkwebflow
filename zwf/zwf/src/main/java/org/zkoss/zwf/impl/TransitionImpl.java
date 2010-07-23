/* Transition.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		May 2, 2009 10:23:49 PM, Created by henrichen
}}IS_NOTE

Copyright (C) 2009 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under GPL Version 2.0 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
*/

package org.zkoss.zwf.impl;

import org.zkoss.zwf.State;
import org.zkoss.zwf.Transition;
import org.zkoss.zwf.metainfo.NodeInfo;
import org.zkoss.zwf.metainfo.TransitionInfo;

/**
 * Runtime instance of {@link TransitionInfo}.
 * @author henrichen
 *
 */
public class TransitionImpl extends AbstractFlowComponent implements Transition {
	public TransitionImpl(NodeInfo info) {
		super(info);
	}
	
	public String getTo() {
		return (String) _info.getAttribute("to");
	}
	
	public String getBookmark() {
		return (String) _info.getAttribute("bookmark");
	}
	
	public State getState() {
		return (State) getParent();
	}
}
