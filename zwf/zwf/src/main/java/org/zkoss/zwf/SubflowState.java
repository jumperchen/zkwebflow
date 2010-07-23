/* SubflowState.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		May 19, 2009 7:00:41 PM, Created by henrichen
}}IS_NOTE

Copyright (C) 2009 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under GPL Version 2.0 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
*/

package org.zkoss.zwf;

/**
 * A state represent a sub-flow.
 * @author henrichen
 *
 */
public interface SubflowState extends State {
	/**
	 * Returns the sub-flow definition uri.
	 * @return the sub-flow definition uri.
	 */
	public String getSubflow();
}
