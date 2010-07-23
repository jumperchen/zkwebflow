/* FlowStack.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		May 12, 2009 4:45:01 PM, Created by henrichen
}}IS_NOTE

Copyright (C) 2009 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under GPL Version 2.0 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
*/

package org.zkoss.zwf.metainfo;

import java.util.Stack;

import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.sys.ExecutionCtrl;
import org.zkoss.zwf.impl.FlowImpl;

/**
 * Utility Stack class for manipulate Flow/Subflow.
 * @author henrichen
 *
 */
public class FlowStack {
	private static final String FLOW_STACK = "zkoss.zwf.FLOW_STACK";
	
	public static FlowImpl peekFlow() {
		final Page page = ((ExecutionCtrl)Executions.getCurrent()).getCurrentPage(); 
		final Stack stack = (Stack) page.getAttribute(FLOW_STACK);
		return stack != null && !stack.isEmpty() ? (FlowImpl) stack.peek() : null; 
	}
	
	public static void pushFlow(FlowImpl flow) {
		final Page page = ((ExecutionCtrl)Executions.getCurrent()).getCurrentPage(); 
		Stack stack = (Stack) page.getAttribute(FLOW_STACK);
		if (stack == null) {
			stack = new Stack();
			page.setAttribute(FLOW_STACK, stack);
		}
		stack.push(flow);
	}
	
	public static FlowImpl popFlow() {
		final Page page = ((ExecutionCtrl)Executions.getCurrent()).getCurrentPage(); 
		final Stack stack = (Stack) page.getAttribute(FLOW_STACK);
		final FlowImpl flow = (FlowImpl) stack.pop();
		if (stack.isEmpty()) {
			clear();
		}
		return flow;
	}
	
	public static void clear() {
		final Page page = ((ExecutionCtrl)Executions.getCurrent()).getCurrentPage(); 
		page.setAttribute(FLOW_STACK, null); //clear
	}
}
