/* SubflowState.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		May 2, 2009 9:55:21 PM, Created by henrichen
}}IS_NOTE

Copyright (C) 2009 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under GPL Version 2.0 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
*/

package org.zkoss.zwf.impl;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.metainfo.EventHandler;
import org.zkoss.zwf.FlowHandler;
import org.zkoss.zwf.SubflowState;
import org.zkoss.zwf.metainfo.FlowDefinition;
import org.zkoss.zwf.metainfo.NodeInfo;
import org.zkoss.zwf.metainfo.SubflowStateInfo;

/**
 * Runtime instance of {@link SubflowStateInfo}.
 * @author henrichen
 *
 */
public class SubflowStateImpl extends StateImpl implements SubflowState {
	
	public SubflowStateImpl(NodeInfo info) {
		super(info);
	}
	
	public String getSubflow() {
		return (String) _info.getAttribute("subflow");
	}
	
	public void enter() {
		super.enter();
		//TODO, if a new view, override here
		FlowImpl flow = ((SubflowStateInfo)getInfo()).getSubflowDefinition().newFlow(this, getFlow().getView());
		flow.enter();
	}
	
	public boolean restore(String statePath) {
		//TODO, if a new view, override here
		FlowImpl flow = ((SubflowStateInfo)getInfo()).getSubflowDefinition().newFlow(this, getFlow().getView());
		return flow.restoreState(statePath); //recursive back here
	}
	
	public void exit() {
		super.exit();
	}
}
