/* FlowEventListener.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		May 20, 2009 10:02:29 AM, Created by henrichen
}}IS_NOTE

Copyright (C) 2009 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under GPL Version 2.0 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
*/

package org.zkoss.zwf.event;

/**
 * Defines the methods used to listener when the flow executed.
 *
 * @author henrichen
 *
 */
public interface FlowEventListener {
	
	/**
	 * Sent when the flow is executed.
	 * @param event the flow event.
	 */
	public void onEvent(FlowEvent event) throws Exception;
}
