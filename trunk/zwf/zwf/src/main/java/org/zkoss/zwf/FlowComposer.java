/* FlowComposer.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		May 19, 2009 5:40:29 PM, Created by henrichen
}}IS_NOTE

Copyright (C) 2009 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under GPL Version 2.0 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
*/

package org.zkoss.zwf;

import org.springframework.stereotype.Controller;
import org.zkoss.zk.ui.Component;
import org.zkoss.zwf.impl.FlowImpl;

/**
 * Represents a composer to initialize a flow when {@link FlowHandler} is 
 * composing a {@link FlowImpl}.
 * 
 * @author henrichen
 */
public interface FlowComposer {
	/** Invokes after FlowHandler creates this {@link FlowImpl},
	 * initializes it and instantiate all its children {@link State}s.
	 * @param flow the Flow has been instantiated
	 */
	public void doAfterCompose(Flow flow) throws Exception;
}
