/* SubflowStateInfo.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		Apr 30, 2009 7:37:03 PM, Created by henrichen
}}IS_NOTE

Copyright (C) 2009 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under GPL Version 2.0 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
*/

package org.zkoss.zwf.metainfo;

import org.zkoss.zwf.FlowHandler;
import org.zkoss.zwf.impl.FlowImpl;

/**
 * &lt;subflow-state> tag in ZK Web Flow definition.
 * @author henrichen
 *
 */
public class SubflowStateInfo extends StateInfo {
	private String _path;
	private FlowDefinition _subflowDefinition;
	
	public SubflowStateInfo(FlowDefinition wfdef) {
		super(wfdef);
	}
	public FlowDefinition getSubflowDefinition() {
		if (_subflowDefinition == null) {
			final String path = getFlowDefinition().getLocator().getDirectory() + getAttribute("subflow");
			//TODO 3.6.2 freshly, use Servlets.getNormalizedPath()
			//_path = Servlets.getNormalizedPath(path);
			_path = FlowHandler.getNormalizedPath(path);
			_subflowDefinition = FlowDefinitions.getFlowDefinition(_path);
		}
		return _subflowDefinition;
	}
}
