/* NodeInfo.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		Apr 30, 2009 3:12:19 PM, Created by henrichen
}}IS_NOTE

Copyright (C) 2009 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under GPL Version 2.0 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
*/

package org.zkoss.zwf.metainfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.metainfo.AttributesInfo;
import org.zkoss.zk.ui.metainfo.ComponentInfo;
import org.zkoss.zk.ui.metainfo.EventHandler;
import org.zkoss.zk.ui.metainfo.EventHandlerMap;
import org.zkoss.zk.ui.metainfo.Property;
import org.zkoss.zk.ui.metainfo.VariablesInfo;
import org.zkoss.zk.ui.metainfo.ZScript;
import org.zkoss.zk.ui.util.ConditionImpl;
import org.zkoss.zk.xel.ExValue;
import org.zkoss.zk.xel.impl.EvaluatorRef;
import org.zkoss.zwf.impl.AbstractFlowComponent;

/**
 * Represents a node of the Flow definition tree.
 * It is an abstract class. The root must be an instance of {@link FlowDefinition}
 * and the other nodes must be instances of 
 * {@link ZScript}, {@link StateInfo}, or {@link TranstionInfo}.
 *
 * <p>Note:it is not thread-safe.
 * 
 * @author henrichen
 *
 */
abstract public class NodeInfo {
	/** A list of {@link NodeInfo} and {@link ZScript}. */
	/*pacakge*/ List _children = new ArrayList();
	/*package*/ NodeInfo _parent;
	/** attribute map */
	protected Map _attrs;
	
	/** A Map of event handler to handle events. */
	protected EventHandlerMap _evthds;
	/** A map of custom attributs (String name, ExValue value). */
	protected Map _custAttrs;
	
	protected final FlowDefinition _flowdef;

	public NodeInfo(FlowDefinition flowdef) {
		_flowdef = flowdef;
	}

	/**
	 * Returns the associated flow definition.
	 * @return
	 */
	public FlowDefinition getFlowDefinition() {
		return _flowdef;
	}
	
	/**
	 * Returns the EvaluatorRef of the associated flow definition.
	 * @return the EvaluatorRef of the associated flow definition.
	 */
	public EvaluatorRef getEvaluatorRef() {
		return getFlowDefinition().getEvaluatorRef();
	}

	/** Adds an event handler.
	 *
	 * @param name the event name.
	 * @param zscript the script.
	 */
	public void addEventHandler(String name, ZScript zscript) {
		if (name == null || zscript == null)
			throw new IllegalArgumentException("name and zscript cannot be null");
		//if (!Events.isValid(name))
		//	throw new IllegalArgumentException("Invalid event name: "+name);
			//AbstractParser has checked it, so no need to check again

		final EventHandler evthd = new EventHandler(getFlowDefinition().getEvaluatorRef(), zscript, null);
		if (_evthds == null)
			_evthds = new EventHandlerMap();
		_evthds.add(name, evthd);
	}
	/** Returns a readonly collection of event names (String),
	 * or an empty collection if no event name is registered.
	 *
	 * <p>To add an event handler, use {@link #addEventHandler} instead.
	 */
	public Set getEventHandlerNames() {
		return _evthds != null ? _evthds.getEventNames(): Collections.EMPTY_SET;
	}
	
	public EventHandler getEventHandler(String evtnm) {
		return _evthds != null ? _evthds.get(null, evtnm) : null;
	}
	
	/** Adds a property initializer.
	 * It will initialize a component when created with this info.
	 * @param name the member name. The component must have a valid setter
	 * for it.
	 * @param value the value. It might contain expressions (${}).
	 */
	public void addAttribute(String name, String value) {
		if (_attrs == null) {
			_attrs = new HashMap();
		}
		_attrs.put(name, value);
	}
	
	public Object getAttribute(String name) {
		if (_attrs != null) {
			return _attrs.get(name);
		}
		return null;
	}
	
	/** Sets the parent.
	 */
	public void setParent(NodeInfo parent) {
		//we don't check if parent is changed (since we have to move it
		//to the end)
		if (_parent != null)
			_parent.removeChildDirectly(this);

		_parent = parent;

		if (_parent != null)
			_parent.appendChildDirectly(this);
	}
	/** Used for implementation only. */
	/*package*/ void setParentDirectly(NodeInfo parent) {
		_parent = parent;
	}

	/** Adds a zscript child.
	 */
	public void appendChild(ZScript zscript) {
		appendChildDirectly(zscript);
	}
	/** Adds a {@link ComponentInfo} child.
	 */
	public void appendChild(NodeInfo compInfo) {
		compInfo.setParent(this); //it will call back appendChildDirectly
	}

	/** Removes a zscript child.
	 * @return whether the child is removed successfully.
	 */
	public boolean removeChild(ZScript zscript) {
		return removeChildDirectly(zscript);
	}
	/** Removes a variables child.
	 * @return whether the child is removed successfully.
	 */
	public boolean removeChild(VariablesInfo variables) {
		return removeChildDirectly(variables); 
	}
	/** Removes a custom-attributes child.
	 * @return whether the child is removed successfully.
	 */
	public boolean removeChild(AttributesInfo custAttrs) {
		return removeChildDirectly(custAttrs); 
	}
	/** Removes a {@link ComponentInfo} child.
	 *
	 * <p>Call {@link ComponentInfo#setParent} instead.
	 * @return whether the child is removed successfully.
	 * @since 2.4.0
	 */
	public boolean removeChild(NodeInfo compInfo) {
		if (compInfo != null && removeChildDirectly(compInfo)) {
			compInfo.setParentDirectly(null);
			return true;
		}
		return false;
	}
	
	/** Adds a child.
	 * <p>Note: it does NOT maintain {@link ComponentInfo#getParent}.
	 */
	/*pacakge*/ void appendChildDirectly(Object child) {
		if (child == null)
			throw new IllegalArgumentException("child required");
		_children.add(child);
	}
	/** Removes a child.
	 * <p>Note: it does NOT maintain {@link ComponentInfo#getParent}.
	 */
	/*package*/ boolean removeChildDirectly(Object child) {
		return _children.remove(child);
	}

	/** Returns a list of children.
	 * Children include instances of {@link NodeInfo}, {@link ZScript}
	 * or {@link AttributesInfo}.
	 *
	 * <p>Note: the returned list is live but it is not a good idea
	 * to modify it directly. It is better to invoke
	 * {@link #appendChild(NodeInfo)} and {@link #removeChild(NodeInfo)}
	 * instead.
	 */
	public List getChildren() {
		return _children;
	}
}
