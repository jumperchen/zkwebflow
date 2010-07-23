/* State.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		May 19, 2009 6:56:46 PM, Created by henrichen
}}IS_NOTE

Copyright (C) 2009 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under GPL Version 2.0 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
*/

package org.zkoss.zwf;

import java.util.Map;

/**
 * A state in a {@link Flow}. 
 * @author henrichen
 *
 */
public interface State extends FlowComponent {
	/**
	 * Returns the associated {@link Flow} of this state.
	 * @return the associated {@link Flow} of this state.
	 */
	public Flow getFlow();
		
	/**
	 * Returns the state scope of this state.
	 * @return the state scope of this state.
	 */
	public Map getStateScope();
}
