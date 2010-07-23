/* EndState.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		May 2, 2009 10:20:40 PM, Created by henrichen
}}IS_NOTE

Copyright (C) 2009 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under GPL Version 2.0 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
*/

package org.zkoss.zwf.impl;

import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.metainfo.EventHandler;
import org.zkoss.zwf.metainfo.EndStateInfo;
import org.zkoss.zwf.metainfo.NodeInfo;

/**
 * Runtime instance of {@link EndStateInfo}.
 * @author henrichen
 *
 */
public class EndStateImpl extends StateImpl {
	public EndStateImpl(NodeInfo info) {
		super(info);
	}
	
	public void enter() {
		super.enter();
	}
	public void exit() {
		//default flow output value is the exit state id
		final FlowImpl flow = getFlow();
		flow.setOutput(getId());
		
		//exit event might do something to override the output value
		super.exit();
		flow.exit();
	}
}
