/* State.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		May 2, 2009 9:37:47 PM, Created by henrichen
}}IS_NOTE

Copyright (C) 2009 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under GPL Version 2.0 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
*/

package org.zkoss.zwf.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.UiException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.metainfo.Annotation;
import org.zkoss.zk.ui.metainfo.ZScript;
import org.zkoss.zk.ui.sys.ComponentCtrl;
import org.zkoss.zwf.State;
import org.zkoss.zwf.event.FlowEvent;
import org.zkoss.zwf.metainfo.EndStateInfo;
import org.zkoss.zwf.metainfo.NodeInfo;
import org.zkoss.zwf.metainfo.StateInfo;
import org.zkoss.zwf.metainfo.SubflowStateInfo;
import org.zkoss.zwf.metainfo.TransitionInfo;
import org.zkoss.zwf.metainfo.ViewStateInfo;


/**
 * Base runtime instance class for {@link StateInfo}. 
 * @author henrichen
 *
 */
abstract public class StateImpl extends AbstractFlowComponent implements State {
	private final static String STATE_SCOPE = "zkoss.zwf.STATE_SCOPE";
	
	protected StateImpl(NodeInfo info) {
		super(info);
		init((StateInfo)info);
	}
	
	private void init(StateInfo info) {
		for(final Iterator it = info.getChildren().iterator(); it.hasNext();) {
			final Object kid = it.next();
			if (kid instanceof ZScript) {
				//TODO execute ZScript
			} else if (kid instanceof TransitionInfo) {
				final TransitionInfo si = (TransitionInfo) kid;
				new TransitionImpl((TransitionInfo)si).setParent(this);
			} else {
				throw new UiException("Unknown children of FlowDefinition: "+kid);
			}
		}
	}
	
	/*package*/ TransitionImpl getTransition(String id) {
		return (TransitionImpl) _children.get(id);
	}
	
	/**
	 * Internal use only.
	 * @return the associated Flow of this State.
	 */
	public FlowImpl getFlow() {
		return (FlowImpl) getParent();
	}
	
	//Enter a state
	public  void enter() {
		final FlowImpl flow = getFlow();
		flow.getFlowScope().put(STATE_SCOPE, new HashMap());
		
		getFlow().setCurrentStateId(getId());
		fireFlowEvent(new FlowEvent("onEntry", this, getFlow().getView()));
	}
	
	//Exit a state
	public void exit() {
		fireFlowEvent(new FlowEvent("onExit", this, getFlow().getView()));
		//destroy viewScope
		final FlowImpl flow = getFlow();
		flow.getFlowScope().remove(STATE_SCOPE);
	}
	
	//return the scope of this state
	public Map getStateScope() {
		final FlowImpl flow = getFlow();
		return flow != null ? (Map) flow.getFlowScope().get(STATE_SCOPE) : null;
	}
}
