/* FlowImpl.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		May 2, 2009 8:15:11 PM, Created by henrichen
}}IS_NOTE

Copyright (C) 2009 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under GPL Version 2.0 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
*/
package org.zkoss.zwf.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.zkoss.lang.Classes;
import org.zkoss.util.logging.Log;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.UiException;
import org.zkoss.zk.ui.metainfo.ZScript;
import org.zkoss.zwf.Flow;
import org.zkoss.zwf.FlowComponent;
import org.zkoss.zwf.FlowComposer;
import org.zkoss.zwf.State;
import org.zkoss.zwf.SubflowState;
import org.zkoss.zwf.event.FlowEvent;
import org.zkoss.zwf.metainfo.ActionStateInfo;
import org.zkoss.zwf.metainfo.EndStateInfo;
import org.zkoss.zwf.metainfo.FlowDefinition;
import org.zkoss.zwf.metainfo.FlowStack;
import org.zkoss.zwf.metainfo.NodeInfo;
import org.zkoss.zwf.metainfo.StateInfo;
import org.zkoss.zwf.metainfo.SubflowStateInfo;
import org.zkoss.zwf.metainfo.ViewStateInfo;

/**
 * Instance of @{link FlowDefinition}.
 * @author henrichen
 */
public class FlowImpl extends AbstractFlowComponent implements Flow {
	private static final Log log = Log.lookup(FlowImpl.class);
	public static final String FLOW_CONTEXT = "zkoss.zwf.FLOW_CONTEXT";
	public static final String FLOW_STATE0 = "$STATE0$"; //special state, zero state
	private static final String FLOW_KEY = "zkoss.zwf.FLOW_KEY";
	private static final String FLASH_SCOPE = "zkoss.zwf.FLASH_SCOPE";
	private static final String CURRENT_STATE_PATH = "zkoss.zwf.CURRENT_STATE_PATH";
	private static final String SNAPSHOTS = "zkoss.zwf.SNAPSHOTS";
	private static final String CURRENT_STATE_ID = "zkoss.zwf.CURRENT_STATE_ID";
	private static final String STATE_KEY = "zkoss.zwf.STATE_KEY";
	private static final String FLOW_OUTPUT = "zkoss.zwf.FLOW_OUTPUT";
	private static final String FLOW_USED = "zkoss.zwf.FLOW_USED";
	
	private FlowDefinition _flowdef;
	private boolean _useBookmark1; //whether load flow bookmark1
	private Component _view;
	private SubflowStateImpl _subflowState;
//	private Object _output;
	private int _flowKey;
//	private int _stateKey;
	private FlowImpl _parentFlow;
	private FlowImpl _topFlow;
	private Map _flowScope;
	private transient String _path; //flow path
	private FlowComposer _composer;
//	private boolean _nobookmark; //default to false, meaning will handle bookmarking

	public FlowImpl(NodeInfo flowdef, SubflowStateImpl subflowState, Component view) {
		super(flowdef);
		_flowdef = (FlowDefinition) flowdef;
		_subflowState = subflowState;
		_view = view;
		
		if (subflowState == null) {
			final Map ctx = getFlowContext();
			Integer flowKey = prepareFlowKey(ctx);
			_flowKey = flowKey.intValue();
			//TODO do we need to put this flow inside FlowContext? see #clear(), too.
			ctx.put(flowKey, this);
		} else {
			//TODO subflowState might add new "page" and new "view" attributes
			//if a subflow, use the top flow's view
			final FlowImpl parentFlow = subflowState.getFlow(); 
			setView(parentFlow.getView());
			_flowKey = parentFlow.getFlowKey();
			
			//register TransitListener of this new flow
/*			final Page page = ((ExecutionCtrl)Executions.getCurrent()).getCurrentPage();
			final Collection roots = page.getRoots();
			for(final Iterator it = roots.iterator(); it.hasNext();) {
				registerTransitListener((Component)it.next());
			}
*/		}
		init(flowdef);
	}
	
	private Map getFlowContext() {
		final Session sess = Sessions.getCurrent();
		Map ctx = null;
		//though almost impossible, avoid flow context access racing in a session(two desktops)
		synchronized (sess) {
			ctx = (Map) sess.getAttribute(FLOW_CONTEXT);
			if (ctx == null) {
				ctx = new HashMap();
				ZKProxy.getProxy().setAttribute(sess, FLOW_CONTEXT, ctx);
			}
		}
		return ctx;
	}
	
	private Integer prepareFlowKey(Map ctx) {
		Integer flowKey = null; 
		synchronized (ctx) {
			flowKey = (Integer) ctx.get(FLOW_KEY);
			if (flowKey == null) {
				flowKey = new Integer(1);
			} else {
				flowKey = new Integer(flowKey.intValue()+1);
			}
			ctx.put(FLOW_KEY, flowKey);
		}
		return flowKey;
	}
	
	/** Internal Use */
	//whether a brand new created flow
	private boolean isFresh() {
		final Map flowScope = getTopFlow().getFlowScope(); 
		return flowScope == null || !flowScope.containsKey(FLOW_USED);
	}
	
	//set a flow as used
	private void setFresh(boolean b) {
		if (!b)
			getTopFlow().getFlowScope().put(FLOW_USED, Boolean.TRUE);
		else
			getTopFlow().getFlowScope().remove(FLOW_USED);
	}
	
	/** Internal Use */
	public String getPath() {
		if (_path == null) {
			FlowImpl flow = this;
			FlowImpl parent = null;
			final StringBuffer sb = new StringBuffer(256)
				.append(flow.isSubflow() ? flow.getSubflowState().getId() : flow.getId());
			while ((parent = flow.getParentFlow()) != null) {
				sb.insert(0, (parent.isSubflow() ? parent.getSubflowState().getId() : parent.getId()) + "/");
				flow = parent;
			}
			_path = sb.toString();
		}
		return _path;
	}
	
	public Map getFlowScope() {
		if (isSubflow()) {
			final Map topFlowScope = getTopFlow().getFlowScope();
			return (Map) topFlowScope.get(getPath());
		} else {
			return _flowScope;
		}
	}
	
	public Map getFlashScope() {
		Map flowScope = getFlowScope();
		return (Map) flowScope.get(FLASH_SCOPE);
	}
	
	/** Returns whether this is a subflow (a.k.a NOT a top flow).
	 * 
	 * @return whether this is a subflow.
	 */
	public boolean isSubflow() {
		return _subflowState != null;
	}
	
	/** Returns the associated subflowState of this flow; could be null if 
	 * this is a top flow.
	 * @return the associated subflowState of this flow
	 */
	public SubflowState getSubflowState() {
		return _subflowState;
	}
	
	/** Returns the parent flow of this flow. */
	public FlowImpl getParentFlow() {
		return _subflowState != null ? _subflowState.getFlow() : null;
	}
	
	/** Returns the top flow in a flow-subflow-subflow hierarchy. */
	public FlowImpl getTopFlow() {
		FlowImpl flow = this;
		FlowImpl parentFlow = null;
		while((parentFlow = flow.getParentFlow()) != null) {
			flow = parentFlow;
		}
		return flow;
	}
	
	/** Sets the output value of a flow/subflow */
	public void setOutput(Object output) {
		getFlowScope().put(FLOW_OUTPUT, output);
	}
	
	/** Returns the output value of a flow/subflow */
	public Object getOutput() {
		return getFlowScope().get(FLOW_OUTPUT);
	}

	/** Sets the associated ZK view anchor component which this flow works on */
	private void setView(Component view) {
		_view = view;
	}
	
	/** Returns the associated ZK View anchor component which this flow works on */
	public Component getView() {
		return _view;
	}
	
	/** Returns the applied @{link FlowComposer} */
	public FlowComposer getFlowComposer() {
		return _composer;
	}
	
	/** Returns the flow component as specified in this flow.*/
	public FlowComponent getFlowComponent(String name) {
		//search flow
		if (name.equals(getId())) { //this flow
			return this;
		}
		//search state of this flow
		if (_children.containsKey(name)) {
			return (FlowComponent) _children.get(name); 
		}
		//search transition for each state
		for (final Iterator it=_children.values().iterator(); it.hasNext();) {
			final StateImpl state = (StateImpl) it.next();
			final Map statekids = state.getChildren();
			if (statekids.containsKey(name)) {
				return (FlowComponent) statekids.get(name);
			}
		}
		//search parentFlow
		final Flow parentFlow = getParentFlow();
		return parentFlow != null ? parentFlow.getFlowComponent(name) : null;
	}
	
	private void init(NodeInfo info) {
		for(final Iterator it = info.getChildren().iterator(); it.hasNext();) {
			final Object kid = it.next();
			if (kid instanceof ZScript) {
				interpretZScript((ZScript) kid, getView());
			} else if (kid instanceof StateInfo){
				final NodeInfo si = (NodeInfo) kid;
				if (si instanceof ViewStateInfo) {
					new ViewStateImpl(si).setParent(this);
				} else if (si instanceof SubflowStateInfo) {
					new SubflowStateImpl(si).setParent(this);
				} else if (si instanceof EndStateInfo) {
					new EndStateImpl((EndStateInfo)si).setParent(this);
				} else if (si instanceof ActionStateInfo) {
					new ActionStateImpl((ActionStateInfo)si).setParent(this);
				} else {
					throw new UiException("Unknown children of FlowDefinition: "+kid);
				}
			} else {
				throw new UiException("Unknown children of FlowDefinition: "+kid);
			}
		}
	}

	public StateImpl lookupState(String stateid) {
		return (StateImpl) _children.get(stateid);	
	}
	
	private void processApply() {
		Object apply = (String) _info.getAttribute("apply");
		if (apply instanceof String) { //class name
			try {
				_composer = (FlowComposer) Classes.newInstanceByThread((String)apply);
				_composer.doAfterCompose(this);
			} catch (Exception ex) {
				throw UiException.Aide.wrap(ex);
			}
		}
		//TODO if apply accept ${} for FlowComposer Class and Object
	}
	
	//enter the flow
	public void enter() {
		//TODO when enter a Flow
		FlowStack.pushFlow(this);
		
		//flowScope allocated
		if (isSubflow()) {
			getTopFlow().getFlowScope().put(getPath(), new HashMap());
		} else {
			_flowScope = new HashMap();
		}
		
		//flashScope allocated
		Map flowScope = getFlowScope();
		flowScope.put(FLASH_SCOPE, new HashMap());
	
		//processing apply composer (Flow and flowScope ready).
		processApply();
		
		//fire onSubflow event to top flow if this is a subflow
		if (isSubflow()) {
			getTopFlow().fireFlowEvent(new FlowEvent(FlowEvent.ON_SUBFLOW, getTopFlow(), this));
		}

		//set as a used flow
		setFresh(false); 

		//calling Flow.onEntry
		fireFlowEvent(new FlowEvent(FlowEvent.ON_ENTRY, this, getView()));
		
		//goto 1st state
		final StateImpl state = getFirstState();
		if (state != null) {
			gotoState(state.getId(), false);
		} else { //no state flow, exit directly
			exit();
		}
	}

	private String parseTransitionAction(Object output) {
		if (output instanceof Boolean) {
			return ((Boolean)output).booleanValue() ? "yes" : "no";
		} else if (output instanceof String){
			return (String) output;
		} else if (output instanceof Enum) {
			return ((Enum)output).name();
		} else {
			return "success";
		}
	}
	
	//exit the flow
	public void exit() {
		//TODO when exit a Flow
		if (isSubflow()) { //back to parent flow
			//fire flow.onExit
			fireFlowEvent(new FlowEvent(FlowEvent.ON_EXIT, this, getView()));
			
			//remember the output of this flow (before destroy the flow) 
			final String output = parseTransitionAction(getOutput());
			
			//destroy flowScope
			getTopFlow().getFlowScope().remove(getPath());
			
			//pop away the current flow
			FlowStack.popFlow();
			
			//transition of the subflowState
			final FlowImpl parentFlow = _subflowState.getFlow();
			parentFlow.transit(_subflowState, output, false);
		} else {
			//fire flow.onExit
			fireFlowEvent(new FlowEvent(FlowEvent.ON_EXIT, this, getView()));

			//exit the top flow
			cleanup();
			
			//destroy flowScope
			_flowScope = null;
			
			//clear the FlowStack
			FlowStack.clear();
			
			//restart a brand new flow
			Executions.getCurrent().sendRedirect(null);
		}
	}
	
	protected void gotoState(String stateid, boolean reuseSnapshotKey) {
		//check whether next state exists
		final StateImpl nextState = lookupState(stateid);
		if (nextState == null) {
			throw new UiException("Unknown state: "+stateid);
		}
		
		//exit from current state
		final StateImpl currentState = (StateImpl) getCurrentState();
		if (currentState != null) {
			currentState.exit();
		}
		//enter the next state
		nextState.enter();
		
		//special end state, exit immediately
		if (nextState instanceof EndStateImpl) {
			nextState.exit();
		} else if (nextState instanceof ViewStateImpl) {
			//next state key
			if (!reuseSnapshotKey) {
				nextStateKey();
			}
			
			//add bookmark
			Executions.getCurrent().getDesktop().setBookmark(getSnapshotKey());
			getTopFlow().getFlowScope().put(CURRENT_STATE_PATH, ((ViewStateImpl)nextState).getStatePath());
			
			//snapshot after rendering
			snapshot(true);
			
			//fire onViewStateChange
			getTopFlow().fireFlowEvent(new FlowEvent(FlowEvent.ON_VIEW_STATE_CHANGE, getTopFlow(), nextState));
		} else if (nextState instanceof ActionStateImpl) {
			final String action = parseTransitionAction(((ActionStateImpl)nextState).getTestResult());
			transit(nextState, action, reuseSnapshotKey); //will recursive back
		}
	}
	
	private StateImpl getFirstState() {
		return _children.isEmpty() ? null : (StateImpl) _children.values().iterator().next();
	}
	
	public String getCurrentStateId() {
		String stateId = (String) getFlowScope().get(CURRENT_STATE_ID);
		if (stateId == null) {
			stateId = FLOW_STATE0;
			setCurrentStateId(stateId);
		}
		return stateId;
	}
	
	public void setCurrentStateId(String stateId) {
		getFlowScope().put(CURRENT_STATE_ID, stateId);
	}
	
	public State getCurrentState() {
		return (State) _children.get(getCurrentStateId());
	}
	
	/**
	 * remove Flow and its snapshots from the FLOW_CONTEXT.
	 */
	private void cleanup() {
		if (_subflowState == null) {
			Map ctx = getFlowContext();
			//clear the session cached Flow
			ctx.remove(new Integer(getFlowKey())); 
			clearSnapshots(); //clear snapshots of this flow
		}
	}
	
	/**
	 * Internal use only.
	 */
	/*package*/ InputStream getInputStream(String name) {
		return ((FlowDefinition)_info).getInputStream(name);
	}
	/**
	 * Internal use only.
	 */
	private int getFlowKey() {
		return _flowKey;
	}
	/**
	 * Internal use only.
	 */
	private int getStateKey() {
		Map ctx = (Map) getFlowContext();
		synchronized (ctx) {
			final String stateKey = STATE_KEY+"_"+getTopFlow().getFlowKey();
			Integer skey = (Integer) ctx.get(stateKey);
			if (skey == null) {
				skey = new Integer(0);
				ctx.put(stateKey, skey);
			}
			return skey.intValue();
		}
	}
	private void nextStateKey() {
		Map ctx = (Map) getFlowContext();
		synchronized (ctx) {
			final String stateKey = STATE_KEY+"_"+getTopFlow().getFlowKey();
			Integer skey = (Integer) ctx.get(stateKey);
			if (skey == null) {
				skey = new Integer(0);
				ctx.put(stateKey, skey);
			}
			ctx.put(stateKey, new Integer(skey.intValue() + 1));
		}
	}
	
	/**
	 * Internal use only. Snapshot the currentState.
	 * @param entry true if snapshot after the entry of the state; 
	 * false means snapshot before transition to next state.
	 */
	protected void snapshot(boolean entry) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream out = null;
		try {
			out = new ObjectOutputStream(baos);
			out.writeObject(getTopFlow().getFlowScope());
			out.close();
			out = null;
			getTopFlow().addSnapshot(getSnapshotKey(), baos.toByteArray());
		} catch (IOException ex) {
			throw UiException.Aide.wrap(ex);
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException ex) {
					//ignore
					ex.printStackTrace();
				}
			}
		}
	}
	private Map getSnapshots() {
		Map ctx = (Map) getFlowContext();
		synchronized (ctx) {
			final String snapshotKey = SNAPSHOTS+"_"+getTopFlow().getFlowKey();
			Map snapshots = (Map) ctx.get(snapshotKey);
			if (snapshots == null) {
				snapshots = new HashMap();
				ctx.put(snapshotKey, snapshots);
			}
			return snapshots;
		}
	}
	
	private void clearSnapshots() {
		Map ctx = (Map) getFlowContext();
		synchronized (ctx) {
			final String snapshotKey = SNAPSHOTS+"_"+getTopFlow().getFlowKey();
			ctx.remove(snapshotKey);
		}
	}
	
	private boolean containsSnapshot(String key) {
		Map ctx = (Map) getFlowContext();
		synchronized (ctx) {
			final String snapshotKey = SNAPSHOTS+"_"+getTopFlow().getFlowKey();
			final Map snapshots = (Map) ctx.get(snapshotKey);
			return snapshots != null && snapshots.containsKey(key);
		}
	}
	
	private void clearSnapshot(String key) {
		getSnapshots().remove(key);
	}
	
	private void addSnapshot(String key, byte[] snapshot) {
		getSnapshots().put(key, snapshot);
	}
	
	/*package*/ boolean restoreState(String statePath) {
		FlowStack.pushFlow(this);

		//restore state
		final String[] split = statePath.split("/", 2);
		final String stateid = split[0];
		final StateImpl state = (StateImpl) _children.get(stateid);
		if (state instanceof SubflowStateImpl) {
			return ((SubflowStateImpl)state).restore(split[1]);
		} else if (state instanceof ViewStateImpl) {
			((ViewStateImpl)state).restore();
			//TODO fire onRestore event?
			return true;
		} 
		return false;
	}
	
	//Returns parsed flowKey([0]) and stateKey([1]). Assume in the form of zXsY format.
	private int[] parseSnapshotKey(String snapshotKey) {
		//check snapshotKey format
		if (!snapshotKey.startsWith("z")) {
			if (log.debugable())
				log.debug("Illegal flow bookmark format: "+snapshotKey);
			return null;
		}
		//parse the snapshotKey
		String[] split = snapshotKey.split("s", 0);
		if (split.length <= 1) {
			if (log.debugable())
				log.debug("Illegal flow bookmark format: "+snapshotKey);
			return null;
		}
		int fkey = -1;
		int skey = -1;
		try {
			final String flowKey = split[0].substring(1); //remove "z"
			fkey = Integer.parseInt(flowKey);
			skey = Integer.parseInt(split[1]);
			return new int[] {fkey, skey};
		} catch (Exception ex) {
			if (log.debugable())
				log.debug("Illegal flow bookmark format: "+snapshotKey);
			return null;
		}
	}
	
	/** Restore the state of this Flow per the given snapshot key. 
	 * @param snapshotKey the zXsY snapshot key
	 */
	public void restore(String snapshotKey) {
		//per the bookmark snapshotKey, restore state
		if (log.debugable()) log.debug("restore:"+snapshotKey);
		
		if (FLOW_STATE0.equals(snapshotKey)) { //state0, new flow execution, enter the flow
			//enter flow
			enter();
			return;
		}
		
		//parse the snapShotKey
		final int[] keys = parseSnapshotKey(snapshotKey);
		
		//illegal snapshotKey
		if (keys == null) {
			if (isFresh()) {
				//fresh new Flow, run as a new flow execution
				restore(FLOW_STATE0);
			} else {
				//correct the bookmark to current snapshotKey
				setBookmark();
			}
			return;
		}
		
		int fkey = keys[0];
//		int skey = keys[1];
		
		//fresh new flow with a snapshotKey bookmark
		if (isFresh()) { 
			//try restore with the snapshotKey
			int orgFlowKey = _flowKey; //might have to restore later
			try {
				_flowKey = fkey;
				//restore the Flow's states 
				if (!restoreStates(snapshotKey)) {
					_flowKey = orgFlowKey;
					restore(FLOW_STATE0); //fail restore, run as a new flow execution
				}
			} catch(Throwable ex) {
				//restore the flow key
				_flowKey = orgFlowKey;
				throw UiException.Aide.wrap(ex);
			}
		} else if (_flowKey != fkey) { //snapshotKey NOT match this flow
			if (containsSnapshot(getSnapshotKey())) {
				//correct the bookmark to current snapshotKey
				setBookmark();
			} else {
				restore(FLOW_STATE0); //run as a new flow execution
			}
		} else { //snapshotKey match this flow
			//restore the Flow's states 
			if (!containsSnapshot(snapshotKey) && containsSnapshot(getSnapshotKey())) {
				//correct the bookmark to current snapshotKey
				setBookmark();
			} else if (!restoreStates(snapshotKey)) {
				restore(FLOW_STATE0); //fail restore, run as a new flow execution
			}
		}
	}
	private void setBookmark() {
		Executions.getCurrent().getDesktop().setBookmark(getSnapshotKey());
	}
	private boolean restoreStates(String snapshotKey) {
		//restore FlowScope of top flow first
		boolean success = false;
		final Map flowScope = restoreTopFlowScope(snapshotKey);
		if (flowScope != null) {
			_flowScope = flowScope;
			String statePath = (String) _flowScope.get(CURRENT_STATE_PATH);
			String[] split = statePath.split("/", 2);
			if (getId().equals(split[0])) { //yes, matched top flow
				FlowStack.clear(); //reset
				success = restoreState(split[1]);
			}
		}
		return success;
	}
	
	private Map restoreTopFlowScope(String snapshotKey) {
		byte[] snapshot = (byte[]) getSnapshots().get(snapshotKey);
		if (snapshot != null) {
			ByteArrayInputStream bais = new ByteArrayInputStream(snapshot);
			ObjectInputStream in = null;
			try {
				in = new ObjectInputStream(bais);
				Map flowScope = (Map) in.readObject();
				in.close();
				in = null;
				return flowScope;
			} catch (IOException ex) {
				throw UiException.Aide.wrap(ex);
			} catch (ClassNotFoundException ex) {
				throw UiException.Aide.wrap(ex);
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException ex) {
						//ignore
						ex.printStackTrace();
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * Internal use only.
	 */
	/*package*/ String getSnapshotKey() {
		return getSnapshotKey(getStateKey());
	}
	/*package*/ String getSnapshotKey(int stateKey) {
		return "z"+getFlowKey()+"s"+stateKey;
	}
	/**
	 * Internal use only.
	 */
	public boolean isUseBookmark1() {
		return _useBookmark1;
	}
	
	/**
	 * Internal use only.
	 */
	public void setUseBookmark1(boolean b) {
		_useBookmark1 = b;
	}

	/*package*/ void transit(StateImpl state, String action, boolean reuseSnapshotKey) {
		final TransitionImpl trs = (TransitionImpl) state.getTransition(action); //find action event listener
		if (trs != null) {
			if (state instanceof ViewStateImpl) {
				//TODO fire onSnapshot event?
				//snapshot this view state before transition to next state
				final String bookmark = trs.getBookmark();
				if ("no".equals(bookmark)) { //no, don't bookmark for this state
					getTopFlow().clearSnapshot(getSnapshotKey());
					reuseSnapshotKey = true;
				} else if ("clear".equals(bookmark)) { //clear all history bookmarks
					getTopFlow().clearSnapshots();
				} else { //yes, bookmark for this state.
					snapshot(false);
				}
			}
			
			//fire Transition.onTransit
			trs.fireFlowEvent(new FlowEvent(FlowEvent.ON_TRANSIT, trs, getView()));

			//change State
			String to = trs.getTo();
			if (to == null) {
				to = state.getId();
			}
			//check if next state exists
			final StateImpl nextState = lookupState(to);
			if (nextState == null) {
				throw new UiException("Unknown state in Transition: "+trs.getId()+", state:"+to);
			}
			gotoState(to, reuseSnapshotKey);
		}
	}
}
