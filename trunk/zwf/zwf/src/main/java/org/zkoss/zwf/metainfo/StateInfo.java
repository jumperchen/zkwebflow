/* StateInfo.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		Apr 30, 2009 7:52:24 PM, Created by henrichen
}}IS_NOTE

Copyright (C) 2009 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under GPL Version 2.0 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
*/

package org.zkoss.zwf.metainfo;

/**
 * Base class of all &lt;xxx-state> tag class in ZK Web Flow definition.
 * @author henrichen
 *
 */
abstract public class StateInfo extends NodeInfo implements RealNode {
	public StateInfo(FlowDefinition wfdef) {
		super(wfdef);
	}
}
