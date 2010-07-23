/* FlowImplicitObjectResolver.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		May 11, 2009 12:45:54 PM, Created by henrichen
}}IS_NOTE

Copyright (C) 2009 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under GPL Version 2.0 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
*/

package org.zkoss.zwf.metainfo;

import java.util.Map;

import org.zkoss.xel.VariableResolver;
import org.zkoss.xel.XelException;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Execution;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zwf.Flow;
import org.zkoss.zwf.FlowComposer;
import org.zkoss.zwf.State;
import org.zkoss.zwf.impl.FlowImpl;
import org.zkoss.zwf.impl.StateImpl;
import org.zkoss.zwf.impl.ZKProxy;

/**
 * VariableResolver to get the Flow's implicit Object and access Flow related scope.
 * @author henrichen
 *
 */
public class FlowVariableResolver implements VariableResolver {
	private static final String FLOW_VARIABLE_RESOLVING = "zkoss.zwf.FLOW_VARIABLE_RESOLVING";

	public Object resolveVariable(String name) throws XelException {
		final Execution exec = Executions.getCurrent();
		if (exec.getAttribute(FLOW_VARIABLE_RESOLVING) != null) { //recursive back
			return null;
		}
		try {
			ZKProxy.getProxy().setAttribute(exec, FLOW_VARIABLE_RESOLVING, Boolean.TRUE);
			if("flow".equals(name)) {
				return getFlow();
			} else if ("flowScope".equals(name)) {
				return getFlowScope();
			} else if ("parentFlow".equals(name)) {
				return getParentFlow();
			} else if ("parentFlowScope".equals(name)) {
				return getParentFlowScope();
			} else if ("topFlow".equals(name)) {
				return getTopFlow();
			} else if ("topFlowScope".equals(name)) {
				return getTopFlowScope();
			} else if ("state".equals(name)) {
				return getState();
			} else if ("stateScope".equals(name)) {
				return getStateScope();
			} else if ("flashScope".equals(name)) {
				return getFlashScope();
			} else if ("composer".equals(name)) {
				return getComposer();
			} else if ("view".equals(name)) {
				return getView();
			} else {
				//search in the scope
				//flashScope
				final Map flashScope = getFlashScope();
				if (flashScope != null) {
					Object val = flashScope.get(name); 
					if (val != null) return val;
				}
				
				//stateScope
				final Map viewScope = getStateScope();
				if (viewScope != null) {
					Object val = viewScope.get(name); 
					if (val != null) return val;
				}
				
				//flowScope
				final Map flowScope = getFlowScope();
				if (flowScope != null) {
					Object val = flowScope.get(name);
					if (val != null) return val;
				}
				
				//ancestor parentFlowScope until topFlowScope
				FlowImpl flow = getFlow();
				if (flow != null) {
					FlowImpl parentFlow = null;
					while((parentFlow = flow.getParentFlow()) != null) {
						Object val = parentFlow.getFlowScope().get(name);
						if (val != null) return val;
						
						flow = parentFlow;
					}
				}
				//sessionScope
/*				if (!"sessionScope".equals(name)) { //avoid recursive endless loop
					final Component view = getView();
					if (view != null) {
						final Map sessionScope = (Map) view.getVariable("sessionScope", false);
						if (sessionScope != null) {
							Object val = sessionScope.get(name);
							if (val != null) return val;
						}
					}
				}
*/				
				//try to get flow component
				Object obj = getFlowComponent(name);
				if (obj != null) return obj; 
				
				//cannot find the named variable 
				return null;
			}
		} finally {
			ZKProxy.getProxy().removeAttribute(exec, FLOW_VARIABLE_RESOLVING);
		}
	}
	
	private Map getFlowScope() {
		final FlowImpl flow = getFlow();
		return flow != null ? (Map) flow.getFlowScope() : null;
	}
	
	private Map getFlashScope() {
		final FlowImpl flow = getFlow();
		return flow != null ? (Map) flow.getFlashScope() : null;
	}
	
	private Flow getParentFlow() {
		final FlowImpl flow = getFlow();
		return flow != null ? (Flow) flow.getParentFlow() : null;
	}
	
	private Map getParentFlowScope() {
		final Flow pflow = getParentFlow(); 
		return pflow != null ? pflow.getFlowScope() : null;
	}
	
	private Flow getTopFlow() {
		final FlowImpl flow = getFlow();
		return flow != null ? (FlowImpl) flow.getTopFlow() : null;
	}
	
	private Map getTopFlowScope() {
		final Flow tflow = getTopFlow();
		return tflow != null ? tflow.getFlowScope() : null;
	}
	
	private State getState() {
		final FlowImpl flow = getFlow();
		return flow != null ? flow.getCurrentState() : null;
	}
	
	private Map getStateScope() {
		final StateImpl state = (StateImpl) getState(); 
		return state != null ? state.getStateScope() : null;
	}
	
	private Object getFlowComponent(String name) {
		final FlowImpl flow = getFlow();
		return flow != null ? flow.getFlowComponent(name) : null;
	}
	
	private FlowImpl getFlow() {
		return FlowStack.peekFlow();
	}
	
	private FlowComposer getComposer() {
		final FlowImpl flow = getFlow();
		return flow != null ? flow.getFlowComposer() : null;
	}
	
	private Component getView() {
		final Flow flow = getFlow();
		return flow != null ? flow.getView() : null;
	}
}
