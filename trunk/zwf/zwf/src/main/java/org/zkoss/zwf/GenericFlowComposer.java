/* GenericFlowComposer.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		May 19, 2009 5:49:42 PM, Created by henrichen
}}IS_NOTE

Copyright (C) 2009 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under GPL Version 2.0 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
*/

package org.zkoss.zwf;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.zkoss.lang.Classes;
import org.zkoss.zk.scripting.Namespaces;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Components;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Execution;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.IdSpace;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.UiException;
import org.zkoss.zk.ui.WebApp;
import org.zkoss.zk.ui.event.Express;
import org.zkoss.zk.ui.sys.ComponentCtrl;
import org.zkoss.zk.ui.sys.DesktopCtrl;
import org.zkoss.zk.ui.sys.ExecutionCtrl;
import org.zkoss.zk.ui.sys.PageCtrl;
import org.zkoss.zk.ui.sys.SessionCtrl;
import org.zkoss.zk.ui.sys.WebAppCtrl;
import org.zkoss.zwf.event.FlowEvent;
import org.zkoss.zwf.event.FlowEventListener;
import org.zkoss.zwf.event.GenericFlowEventListener;
import org.zkoss.zwf.impl.ComponentProxy;
import org.zkoss.zwf.impl.FlowImpl;
import org.zkoss.zwf.impl.StateImpl;
import org.zkoss.zwf.impl.ZKProxy;

/**
 * Generic flow composer which do variable auto-wiring and event registration. 
 * @author henrichen
 *
 */
abstract public class GenericFlowComposer implements FlowComposer {
	//-- Implicit ZK Components --//
	/** Implicit Object; the component itself in current execution. 
	 */ 
	protected Component self;
	/** Implicit Object; the space owner of the current execution.
	 */
	protected IdSpace spaceOwner;
	/** Implicit Object; the page of the current execution.
	 */
	protected Page page;
	/** Implicit Object; the desktop of the current execution.
	 */
	protected Desktop desktop;
	/** Implicit Object; the current session.
	 */
	protected Session session;
	/** Implicit Object; the web application.
	 */
	protected WebApp application;
	/** Implicit Object; a map of attributes defined in the component of the current execution.
	 */
	protected Map componentScope;
	/** Implicit Object; a map of attributes defined in the ID space contains the component of the current execution.
	 */
	protected Map spaceScope;
	/** Implicit Object; a map of attributes defined in the page.
	 */
	protected Map pageScope;
	/** Implicit Object; a map of attributes defined in the desktop.
	 */
	protected Map desktopScope;
	/** Implicit Object; a map of attributes defined in the session.
	 */
	protected Map sessionScope;
	/** Implicit Object; a map of attributes defined in the web application.
	 */
	protected Map applicationScope;
	/** Implicit Object; a map of attributes defined in the request.
	 */
	protected Map requestScope;
	/** Implicit Object; the current execution.
	 */
	protected Execution execution;
	/** Implicit Object; the arg argument passed to the createComponents method. It is never null.
	 */
	protected Map arg;
	/** Implicit Object; the param argument passed from the http request.
	 */
	protected Map param;
	
	//-- Implicit Flow Components --//
	/** Implicit Object; The current flow. */
	protected Flow flow;
	/** Implicit Object; The parent flow of the current flow.  */
	protected Flow parentFlow;
	/** Implicit Object; The top flow of the current flow. */
	protected Flow topFlow;
	/** Implicit Object; The current state. */
	protected State state;
	/** Implicit Object; The working view of the current flow. */
	protected Component view;
	
	/** Implicit Object; The flowScope of the current flow. */
	protected Map flowScope;
	/** Implicit Object; The flashScope of the current flow. */
	protected Map flashScope;
	/** Implicit Object; The flowScope of the parent flow of the current flow. */
	protected Map parentFlowScope;
	/** Implicit Object; The flowScope of the top flow of the current flow. */
	protected Map topFlowScope;
	/** Implicit Object; The stateScope of the current state. */
	protected Map stateScope;
	
	/** The separator. */
	protected final char _separator;
	
	private Set _listeners = new HashSet();
	private Method[] _mtds;
	protected Component _view;

	protected GenericFlowComposer() {
		_separator = '$';
	}
	
	/** Constructor with a custom separator.
	 * The separator is used to separate the flow component ID and event name.
	 * By default, it is '$'. For Grooy and other environment that '$'
	 * is not applicable, you can specify '_'.
	 * @since 3.6.0
	 */
	protected GenericFlowComposer(char separator) {
		_separator = separator;
	}

	private Component getRef() {
		Component ref = (Component) ZKProxy.getProxy().getSelf((ExecutionCtrl)Executions.getCurrent());
		if (ref == null || ref.getPage() == null)
			ref = _view;
		return ref;
	}
	
	protected Object getVariable(String name) {
		final Component ref = getRef();
		Object obj = ref.getVariable(name, false);
		if (obj == null) {
			Flow cflow = (Flow) ref.getVariable("flow", false); //get current flow
			if (cflow != null) {
				obj = ((FlowImpl)cflow).getFlowComponent(name); //Flow, State, Transition in current Flow. 
			}
		}
		return obj;
	}
	
	public void doAfterCompose(Flow flow) throws Exception {
		this._view = flow.getView();
		_view.setVariable(flow.getId()+_separator+"composer", this, true);
		wireVariablesAndAddEventListeners();
		
		//register rewiring listener when view state changes 
		flow.addFlowEventListener(FlowEvent.ON_VIEW_STATE_CHANGE, new RewireListener());
		flow.addFlowEventListener(FlowEvent.ON_SUBFLOW, new RewireListener());
	}
	
	private void wireVariablesAndAddEventListeners() {
		new WireDynamicProxy(this).wireVariables(); //wire dynamic proxy first
		final Component ref = getRef();
		Components.wireVariables(ref, this, _separator);
		addFlowEventListeners();
	}
	
	private class RewireListener implements FlowEventListener, Express {
		public void onEvent(FlowEvent event) throws Exception {
			//wire variables to reference fields when change view state
			wireVariablesAndAddEventListeners();
		}
	}
	
	private Method[] getMethods() {
		if (_mtds == null) {
			final Class cls = this.getClass();
			final Method[] mtds = cls.getMethods();
			final List onmtds = new ArrayList(mtds.length);
			final String sep = _separator == '$' ? ("\\"+_separator) : (""+_separator);
			for (int j = 0; j < mtds.length; ++j) {
				final Method md = mtds[j];
				String mdname = md.getName();
				if (_listeners.contains(mdname)) { //already registered, skip
					continue;
				}
				if (mdname.length() >= 5 && mdname.startsWith("on") 
				&& Character.isUpperCase(mdname.charAt(2))) {
					String[] split = mdname.split(sep, 0);
					if (split.length >= 2) { //legal flow onXxx methods
						onmtds.add(md);
					}
				}
			}
			_mtds = (Method[]) onmtds.toArray(new Method[onmtds.size()]);
		}
		return _mtds;
	}
	
	//addEventListeners
	private void addFlowEventListeners() {
		final Method[] mtds = getMethods();
		//special case
		if (mtds.length <= _listeners.size()) { //no more method to be registered
			return;
		}
		final String sep = _separator == '$' ? ("\\"+_separator) : (""+_separator);
		for (int j = 0; j < mtds.length; ++j) {
			final Method md = mtds[j];
			String mdname = md.getName();
			if (_listeners.contains(mdname)) { //already registered, skip
				continue;
			}
			String[] split = mdname.split(sep, 0);
			if (split.length == 2) { //onXxx$flow, onXxx$state[$flow]
				String evtnm = split[0];
				Object comp = getVariable(split[1]);
				
				if (comp instanceof State || comp instanceof Flow) {
					((FlowComponent)comp).addFlowEventListener(evtnm, new GenericFlowEventListener(mdname, this));
					_listeners.add(mdname);
				}
			} else if (split.length == 3){ //onXxx$state$flow, onXxx$transition$state[$flow]
				String evtnm = split[0];
				String trId = split[1]; //state or transition
				String stateId = split[2]; //flow or state
				
				//flow take precedence
				Flow xflow = flow;
				while (xflow != null && !stateId.equals(xflow.getId())) {
					xflow = xflow.getParentFlow();
				}
				if (xflow != null) {
					final Map states = ((Flow)xflow).getChildren();
					if (states != null) {
						final State state = (State) states.get(stateId);
						if (state != null) {
							state.addFlowEventListener(evtnm, new GenericFlowEventListener(mdname, this));
							_listeners.add(mdname);
						}
					}
				}
				
				if (!_listeners.contains(mdname)) {
					xflow = flow;
					do {
						final Map states = ((Flow)xflow).getChildren();
						if (states != null) {
							final State state = (State) states.get(stateId);
							if (state != null) {
								final Object comp = ((StateImpl)state).getChildren().get(trId);
								if (comp instanceof Transition) {
									((FlowComponent)comp).addFlowEventListener(evtnm, new GenericFlowEventListener(mdname, this));
									_listeners.add(mdname);
									break;
								}
							}
						}
					} while ((xflow = xflow.getParentFlow()) != null);
				}
			} else { //split.length == 4 //onXxx$transition$state$flow
				String evtnm = split[0];
				String trId = split[1]; //transition
				String stateId = split[2]; //state
				String flowId = split[3]; //flow
				
				Flow xflow = flow;
				while (xflow != null && !flowId.equals(xflow.getId())) {
					xflow = xflow.getParentFlow();
				}
				if (xflow != null) {
					final Map states = ((Flow)xflow).getChildren();
					if (states != null) {
						final State state = (State) states.get(stateId);
						if (state != null) {
							final Object comp = ((StateImpl)state).getChildren().get(trId);
							if (comp instanceof Transition) {
								((FlowComponent)comp).addFlowEventListener(evtnm, new GenericFlowEventListener(mdname, this));
								_listeners.add(mdname);
								break;
							}
						}
					}
				}
			}
		}
	}
		
	

	/**
	 * Wiring dynamic UI component proxy in a flow.
	 * @author henrichen
	 */
	private class WireDynamicProxy {
		private final Object _controller;
		private final Set _injected;
		private final Map _fldMaps;
		
		public WireDynamicProxy(Object controller) {
			_controller = controller;
			_injected = new HashSet();
			_fldMaps = new LinkedHashMap(64);
			
			Class cls = _controller.getClass();
			do {
				Field[] flds = cls.getDeclaredFields();
				for (int j = 0; j < flds.length; ++j) {
					final Field fd = flds[j];
					final String fdname = fd.getName();
					if (!_fldMaps.containsKey(fdname))
						_fldMaps.put(fdname, fd);
				}
				cls = cls.getSuperclass();
			} while (cls != null && !Object.class.equals(cls));
		}
		public void wireVariables() {
			//check methods
			final Class cls = _controller.getClass();
			Method[] mtds = cls.getMethods();
			for (int j = 0; j < mtds.length; ++j) {
				final Method md = mtds[j];
				final String mdname = md.getName();
				if (mdname.length() > 3 && mdname.startsWith("set") 
				&& Character.isUpperCase(mdname.charAt(3))) {
					final String fdname = Classes.toAttributeName(mdname);
					if (!_injected.contains(fdname)) { //if not injected yet
						final Class[] parmcls = md.getParameterTypes();
						if (parmcls.length == 1) {
							final Class fdcls = parmcls[0];
							if (Component.class.isAssignableFrom(fdcls) && fdcls.isInterface()) { //a kind of Component interface
								final Object arg = ComponentProxy.newInstance(parmcls, _view, fdname);
								injectByMethod(md, arg, fdname);
							}
						}
					}
				}
			}

			//check fields
			for (final Iterator it=_fldMaps.entrySet().iterator();it.hasNext();) {
				final Entry entry = (Entry) it.next();
				final String fdname = (String) entry.getKey();
				if (!_injected.contains(fdname)) { //if not injected by setXxx yet
					final Field fd = (Field) entry.getValue();
					final Class fdcls = fd.getType();
					
					if (fdcls.isInterface()) {
						if (!injectImplicit(fd, fdname)) {
							if (Component.class.isAssignableFrom(fdcls)) { //a kind of Component interface
								final Object arg = ComponentProxy.newInstance(new Class[] {fdcls}, _view, fdname);
								injectField(arg, fd);
							}
						}
					}
				}
			}
		}
		
		private boolean injectImplicit(Field fd, String fdname) {
			Object arg = null;
			if ("self".equals(fdname)
					|| "view".equals(fdname)) {
				arg = ComponentProxy.newInstance(new Class[] {Component.class, ComponentCtrl.class, IdSpace.class}, _view, fdname);
			} else if ("spaceOwner".equals(fdname)) {
				arg = ComponentProxy.newInstance(new Class[] {IdSpace.class, Component.class, ComponentCtrl.class, Page.class, PageCtrl.class}, _view, fdname);
			} else if ("page".equals(fdname)) {
				arg = ComponentProxy.newInstance(new Class[] {Page.class, PageCtrl.class, IdSpace.class}, _view, fdname);
			} else if ("desktop".equals(fdname)) {
				arg = ComponentProxy.newInstance(new Class[] {Desktop.class, DesktopCtrl.class}, _view, fdname);
			} else if ("session".equals(fdname)) {
				arg = ComponentProxy.newInstance(new Class[] {Session.class, SessionCtrl.class}, _view, fdname);
			} else if ("application".equals(fdname)) {
				arg = ComponentProxy.newInstance(new Class[] {WebApp.class, WebAppCtrl.class}, _view, fdname);
			} else if ("componentScope".equals(fdname)
					|| "spaceScope".equals(fdname)
					|| "pageScope".equals(fdname)
					|| "desktopScope".equals(fdname)
					|| "sessionScope".equals(fdname)
					|| "applicationScope".equals(fdname)
					|| "requestScope".equals(fdname)
					|| "arg".equals(fdname)
					|| "param".equals(fdname)
					|| "flashScope".equals(fdname)
					|| "flowScope".equals(fdname)
					|| "parentFlowScope".equals(fdname)
					|| "topFlowScope".equals(fdname)
					|| "stateScope".equals(fdname)
					) {
				arg = ComponentProxy.newInstance(new Class[] {Map.class}, _view, fdname);
			} else if ("execution".equals(fdname)) {
				arg = ComponentProxy.newInstance(new Class[] {Execution.class, ExecutionCtrl.class}, _view, fdname);
			} else if ("flow".equals(fdname)
					|| "parentFlow".equals(fdname)
					|| "topFlow".equals(fdname)) {
				arg = ComponentProxy.newInstance(new Class[] {Flow.class, ExecutionCtrl.class}, _view, fdname);
			} else if ("state".equals(fdname)) {
				arg = ComponentProxy.newInstance(new Class[] {State.class, ExecutionCtrl.class}, _view, fdname);
			} 
			
			if (arg != null) {
				injectField(arg, fd);
				return true;
			}
			return false;
		}
		
		private boolean injectByMethod(Method md, Object arg, String fdname) {
			final Field fd = (Field) _fldMaps.get(fdname);
			if (fd != null) {
				final boolean old = fd.isAccessible();
				try {
					//check field value
					fd.setAccessible(true);
					final Object value = fd.get(_controller);
					if (value == null) {
						md.invoke(_controller, new Object[] {arg});
						if (fd.get(_controller) != null) { //field is set
							_injected.add(fdname); //mark as injected
							return true;
						}
					}
				} catch (Exception ex) {
					throw UiException.Aide.wrap(ex);
				} finally {
					fd.setAccessible(old);
				}
			} else {
				try {
					md.invoke(_controller, new Object[] {arg});
					_injected.add(fdname); //no field, just mark as injected
					return true;
				} catch (Exception ex) {
					throw UiException.Aide.wrap(ex);
				}
			}
			return false;
		}
		
		private boolean injectField(Object arg, Field fd) {
			final boolean old = fd.isAccessible();
			try {
				fd.setAccessible(true);
				final Object value = fd.get(_controller);
				if (value == null) {
					fd.set(_controller, arg);
					_injected.add(fd.getName());
					return true;
				}
			} catch (Exception e) {
				throw UiException.Aide.wrap(e);
			} finally {
				fd.setAccessible(old);
			}
			return false;
		}
	}
}
