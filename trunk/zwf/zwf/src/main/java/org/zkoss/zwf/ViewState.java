/* ViewState.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		May 19, 2009 7:11:48 PM, Created by henrichen
}}IS_NOTE

Copyright (C) 2009 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under GPL Version 2.0 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
*/

package org.zkoss.zwf;

/**
 * state to assocate a view in a flow.
 * @author henrichen
 *
 */
public interface ViewState extends State {
	/**
	 * Returns the hint (no | yes | poup-template-uri) for whether popup this view state;
	 * @return Returns the hint (no | yes | poup-template-uri) for whether popup this view state
	 */
	public String getPopup();
}
