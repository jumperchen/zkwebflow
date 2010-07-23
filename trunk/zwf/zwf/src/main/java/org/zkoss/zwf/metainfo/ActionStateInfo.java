/* ActionStateInfo.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		May 21, 2009 6:29:56 PM, Created by henrichen
}}IS_NOTE

Copyright (C) 2009 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under GPL Version 2.0 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
*/

package org.zkoss.zwf.metainfo;

import org.zkoss.zk.ui.metainfo.ZScript;

/**
 * &lt;action-state> tag in ZK Web Flow definition.
 * @author henrichen
 */
public class ActionStateInfo extends StateInfo {
	private ZScript _zscript;
	
	public ActionStateInfo(FlowDefinition flowdef) {
		super(flowdef);
	}
	
	public void setTest(ZScript zscript) {
		_zscript = zscript;
	}
	
	public ZScript getTest() {
		return _zscript;
	}
}
