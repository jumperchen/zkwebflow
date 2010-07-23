/* AbstractFlowComponent.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		May 2, 2009 8:06:08 PM, Created by henrichen
}}IS_NOTE

Copyright (C) 2009 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under GPL Version 2.0 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
*/

package org.zkoss.zwf.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.UiException;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Express;
import org.zkoss.zk.ui.metainfo.EventHandler;
import org.zkoss.zk.ui.metainfo.ZScript;
import org.zkoss.zk.ui.sys.ExecutionCtrl;
import org.zkoss.zwf.Flow;
import org.zkoss.zwf.FlowComponent;
import org.zkoss.zwf.State;
import org.zkoss.zwf.Transition;
import org.zkoss.zwf.event.FlowEvent;
import org.zkoss.zwf.event.FlowEventListener;
import org.zkoss.zwf.metainfo.NodeInfo;

/**
 * Base Flow Object.
 * @author henrichen
 *
 */
abstract public class AbstractFlowComponent implements FlowComponent {
	protected Map _children = new LinkedHashMap();
	protected Map _listenersMap;
	protected AbstractFlowComponent _parent;
	protected NodeInfo _info;

	protected AbstractFlowComponent(NodeInfo info) {
		_info = info;
	}
	
	public EventHandler getEventHandler(String evtname) {
		return _info.getEventHandler(evtname);
	}
	
	public String getId() {
		return (String) _info.getAttribute("id");
	}
	
	public NodeInfo getInfo() {
		return _info;
	}
	
	public void appendChild(AbstractFlowComponent obj) {
		obj.setParent(this); //will add into _children 
	}
	
	public void setParent(AbstractFlowComponent parent) {
		_parent = parent;
		parent.appendChildDirectly(this);
	}
	
	public AbstractFlowComponent getParent() {
		return _parent;	
	}
	protected void appendChildDirectly(AbstractFlowComponent kid) {
		_children.put(kid.getId(), kid);
	}
	
	public Map getChildren() {
		return _children;
	}
	
	public void addFlowEventListener(String name, FlowEventListener lsn) {
		if (_listenersMap == null) {
			_listenersMap = new HashMap();
		}
		if (lsn instanceof Express) {
			name = "EXPR_"+name;
		}
		List lsns = (List) _listenersMap.get(name);
		if (lsns == null) {
			lsns = new ArrayList(4);
			_listenersMap.put(name, lsns);
		}
		lsns.add(lsn);
	}
	
	public void removeFlowEventListener(String name, FlowEventListener lsn) {
		if (_listenersMap != null) {
			if (lsn instanceof Express) {
				name = "EXPR_"+name;
			}
			List lsns = (List) _listenersMap.get(name);
			if (lsns != null) {
				lsns.remove(lsn);
			}
		}
	}
	
	/**
	 * Send FlowEvent to this flow component.
	 * @param event the {@link FlowEvent} sent
	 */
	public void fireFlowEvent(FlowEvent event) {
		try {
			fireFlowEvent0(event);
		} catch(Exception ex) {
			throw UiException.Aide.wrap(ex);
		}
	}
	
	private void fireFlowEvent0(FlowEvent event) throws Exception {
		final String name = event.getName();
		if (_listenersMap != null) {
			//Express event listener
			List exprlsns = (List) _listenersMap.get("EXPR_"+name);
			if (exprlsns != null) {
				for(final Iterator it=exprlsns.iterator(); it.hasNext();) {
					final FlowEventListener lsn = (FlowEventListener) it.next();
					lsn.onEvent(event);
				}
			}
		}
		
		//ZScript onXxx in flow definition file
		EventHandler handler = getEventHandler(name);
		if (handler != null) {
			final Component target = event.getTarget();
			interpretZScript(handler.getZScript(), target == null ? (Component) event.getData() : target);
		}
		
		if (_listenersMap != null) {
			//normal event listener
			List lsns = (List) _listenersMap.get(name);
			if (lsns != null) {
				for(final Iterator it=lsns.iterator(); it.hasNext();) {
					final FlowEventListener lsn = (FlowEventListener) it.next();
					lsn.onEvent(event);
				}
			}
		}
	}
	
	protected void interpretZScript(ZScript zscript, Component comp) {
		if (zscript != null) {
			Page page = ((ExecutionCtrl)Executions.getCurrent()).getCurrentPage();
			page.interpret(
				zscript.getLanguage(), zscript.getContent(page, comp), comp.getNamespace());
		}
	}
}
