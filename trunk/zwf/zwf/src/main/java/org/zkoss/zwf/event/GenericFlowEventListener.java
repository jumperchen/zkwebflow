/* GenericFlowEventListener.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		May 20, 2009 10:38:56 AM, Created by henrichen
}}IS_NOTE

Copyright (C) 2009 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under GPL Version 2.0 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
*/

package org.zkoss.zwf.event;

import java.lang.reflect.Method;

import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ComponentsCtrl;
import org.zkoss.zwf.FlowComponent;

/**
 * <p>An abstract event listener that you can extend and write intuitive onXxx flow event 
 * handler methods; this class dispatch {@link FlowEvent} to the implemented onXxx event 
 * handler methods automatically. It also provides a convenient method 
 * {@link #bindFlowComponent} that you can bind a target flow component to this event listener easily.</p>
 * <p>Following is an example. Whenever onEntry or onExit is sent to this flow event
 * listener, it dispatch the control to the corresponding defined onEntry and onExit methods
 * respectively. Note how the bindComponent() method bind this flow event listener to the flow
 * component.</p>
 *<pre><code>
 * &lt;flow id="main">
 *     ...
 * &lt;/flow>
 * 
 * &lt;zscript>&lt;!-- both alright in zscript or a compiled Java class -->
 * public class MyEventListener extends GenericFlowEventListener {
 *    public void onEntry(FlowEvent evt) {
 *        //doEntry!
 *        //...
 *    }
 *    public void onExit() {
 *        //doExit
 *        //...
 *    } 
 * }
 *
 * new MyEventListener().bindFlowComponent(main);
 * &lt;/zscript>
 * </code></pre>
 * 
 * @author henrichen
 */
public class GenericFlowEventListener implements FlowEventListener {
	private Object _controller;
	private String _mdname;
	
	/**
	 * Associated event handler method.
	 * @param mdname associated method name
	 * @param controller controller where the method is called
	 */
	public GenericFlowEventListener(String mdname, Object controller) {
		_mdname = mdname;
		_controller = controller;
	}
	
	/* Process the event by dispatching the invocation to
	 * the corresponding method called onXxx.
	 *
	 * <p>You rarely need to override this method.
	 * Rather, provide corresponding onXxx method to handle the event.
	 * 
	 * @see org.zkoss.zk.ui.event.EventListener#onEvent(org.zkoss.zk.ui.event.Event)
	 */	
	public void onEvent(FlowEvent evt) throws Exception {		
		final Method mtd =	ComponentsCtrl.getEventMethod(_controller.getClass(), _mdname);
		
		if (mtd != null) {
			if (mtd.getParameterTypes().length == 0)
				mtd.invoke(_controller, null);
			else
				mtd.invoke(_controller, new Object[] {evt});
		}
	}
}
