/* TransitionInfo.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		Apr 30, 2009 7:38:39 PM, Created by henrichen
}}IS_NOTE

Copyright (C) 2009 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under GPL Version 2.0 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
*/

package org.zkoss.zwf.metainfo;

/**
 * &lt;transition> tag in ZK Web Flow definition.
 * @author henrichen
 *
 */
public class TransitionInfo extends NodeInfo implements RealNode {
	public TransitionInfo(FlowDefinition wfdef) {
		super(wfdef);
	}
}
