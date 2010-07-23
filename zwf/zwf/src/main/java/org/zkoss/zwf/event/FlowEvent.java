/* FlowEvent.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		May 2, 2009 8:19:22 PM, Created by henrichen
}}IS_NOTE

Copyright (C) 2009 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under GPL Version 2.0 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
*/

package org.zkoss.zwf.event;

import org.zkoss.zk.scripting.Namespaces;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.sys.ExecutionCtrl;
import org.zkoss.zwf.FlowComponent;
import org.zkoss.zwf.impl.AbstractFlowComponent;
import org.zkoss.zwf.impl.ZKProxy;

/**
 * Defines an event that encapsulates changes to a flow component. 
 * @author henrichen
 *
 */
public class FlowEvent extends Event {
	public static final String ON_ENTRY = "onEntry"; //fire to Flow, State
	public static final String ON_EXIT = "onExit"; //fire to Flow, State
	public static final String ON_TRANSIT = "onTransit"; //fire to Transition
	public static final String ON_VIEW_STATE_CHANGE = "onViewStateChange"; //fire to topFlow (when transit to a ViewState)
	public static final String ON_SUBFLOW = "onSubflow"; //fire to topFlow (when transit to a Subflow)
	
	private FlowComponent _flowcomp;
	
	public FlowEvent(String name, FlowComponent obj, Object data) {
		super(name, ZKProxy.getProxy().getSelf((ExecutionCtrl)Executions.getCurrent()), data);
		_flowcomp = obj;
	}
	
	public FlowComponent getFlowTarget() {
		return _flowcomp;
	}

	//Object//
	public String toString() {
		return "[FlowEvent name="+getName()+", flowTarget=" + _flowcomp +", target="+getTarget()+", data="+getData()+']';
	}
}
