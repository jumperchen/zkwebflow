/* FlowComponent.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		May 20, 2009 9:18:54 AM, Created by henrichen
}}IS_NOTE

Copyright (C) 2009 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under GPL Version 2.0 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
*/

package org.zkoss.zwf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Express;
import org.zkoss.zwf.event.FlowEventListener;

/**
 * Generic interface for Flow related component.
 * @author henrichen
 *
 */
public interface FlowComponent {
	/**
	 * Returns the id of this flow component.
	 * @return the id of this flow component.
	 */
	public String getId();
	
	/**
	 * Add an {@link FlowEventListener} to this flow component(i.e. @{link Flow}, @{link State}, @{link Transition}).
	 * @param name the event name
	 * @param lsn the event listener
	 */
	public void addFlowEventListener(String name, FlowEventListener lsn);
	
	/**
	 * Remove an {@link FlowEventListener} from this flow component(i.e. @{link Flow}, @{link State}, @{link Transition}).
	 * @param name the event name
	 * @param lsn the event listener
	 */
	public void removeFlowEventListener(String name, FlowEventListener lsn);
}
