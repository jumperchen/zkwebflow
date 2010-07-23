/* ActionStateImpl.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		May 21, 2009 6:28:34 PM, Created by henrichen
}}IS_NOTE

Copyright (C) 2009 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under GPL Version 2.0 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
*/

package org.zkoss.zwf.impl;

import org.zkoss.zk.scripting.Namespaces;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.metainfo.ZScript;
import org.zkoss.zk.ui.sys.ExecutionCtrl;
import org.zkoss.zwf.metainfo.ActionStateInfo;
import org.zkoss.zwf.metainfo.NodeInfo;
import org.zkoss.zwf.metainfo.SubflowStateInfo;

/**
 * Runtime instance of {@link ActionStateInfo}.
 * 
 * @author henrichen
 *
 */
public class ActionStateImpl extends StateImpl {

	public ActionStateImpl(NodeInfo info) {
		super(info);
	}

	private Component getRef() {
		Component comp = ZKProxy.getProxy().getSelf((ExecutionCtrl)Executions.getCurrent());
		if (comp == null || comp.getPage() == null) {
			comp = getFlow().getView();
		}
		return comp;
		
	}
	
	//ActionState's test result.
	/* package */ Object getTestResult() {
		final ZScript test = ((ActionStateInfo) _info).getTest();
		Component ref = getRef();
		getStateScope().remove("ACTION_STATE_TEST"); //clear it first
		interpretZScript(test, ref);
		return getStateScope().get("ACTION_STATE_TEST");
	}

	public void enter() {
		//TODO might have to plug something here; otherwise, shall remove the method
		super.enter();
	}
	
	public void exit() {
		//TODO might have to plug something here; otherwise, shall remove the method
		super.exit();
	}
}
