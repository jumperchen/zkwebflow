/* FlowHandler.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		Apr 30, 2009 11:49:19 PM, Created by henrichen
}}IS_NOTE

Copyright (C) 2009 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under GPL Version 2.0 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
*/

package org.zkoss.zwf;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.zkoss.util.logging.Log;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.UiException;
import org.zkoss.zk.ui.event.BookmarkEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.Express;
import org.zkoss.zk.ui.metainfo.Annotation;
import org.zkoss.zk.ui.sys.ComponentCtrl;
import org.zkoss.zk.ui.util.Initiator;
import org.zkoss.zk.ui.util.InitiatorExt;
import org.zkoss.zwf.impl.FlowImpl;
import org.zkoss.zwf.metainfo.FlowDefinition;
import org.zkoss.zwf.metainfo.FlowDefinitions;
import org.zkoss.zwf.metainfo.FlowVariableResolver;

/**
 * The ZK Web Flow handler. In the web flow ZUML file, specify this &lt;?init>
 * directive. e.g.
 * 
 * <pre><code>
 * &lt;?init class="org.zkoss.zwf.FlowHandler" arg0="/WEB-INF/flow/main/main.xml" [arg1="content"]?>
 * ...
 * <div ... self="@{view(content)}">
 * </div>
 * ...
 * </code></pre>
 * 
 * <ul>
 * <li>In the arg0 specifies the URL of the top flow definition file</li>
 * <li>In the OPTIONAL arg1 specifies the view id on which the flow will change 
 * its content; the default id is "content".</li>
 * </ul>
 * 
 * <p>Note that the view DO NOT necessary to be a &lt;div>. It can be any ZK 
 * component as long as we can use it as the root for each view state.</p>
 * 
 * @author henrichen
 *
 */
public class FlowHandler implements Initiator, InitiatorExt {
	private static final Log log = Log.lookup(FlowHandler.class);
	private static final String ON_AFTER_LOAD = "onAfterLoad";
	
	public static final String FLOW_BOOKMARK_LISTENER = "zkoss.zwf.FLOW_BOOKMARK_LISTENER";
	
	private String _path;
	private String _viewid = "content"; //default view id to "content"
	private Component _view;
	
	/* (non-Javadoc)
	 * @see org.zkoss.zk.ui.util.Initiator#doAfterCompose(org.zkoss.zk.ui.Page)
	 */
	public void doAfterCompose(Page page) throws Exception {
		//will not call into here since we also implement InitiatorExt
	}

	public boolean doCatch(Throwable ex) throws Exception {
		//do nothing
		return false;
	}

	/* (non-Javadoc)
	 * @see org.zkoss.zk.ui.util.Initiator#doFinally()
	 */
	public void doFinally() throws Exception {
		//do nothing
	}

	public void doInit(Page page, Map args) throws Exception {
		if (args.size() < 1) {
			throw new UiException("Must specify the uri to the main web flow defintion file!");
		}
		_path = getNormalizedPath(Executions.getCurrent().toAbsoluteURI((String) args.get("arg0"), false));
		if (args.size() > 1) {
			_viewid = (String) args.get("arg1");
		}
		//Flow implicit Object resolver
		page.addVariableResolver(new FlowVariableResolver());
	}

	public void doAfterCompose(Page page, Component[] comps) throws Exception {
		final FlowDefinition flowdef = FlowDefinitions.getFlowDefinition(_path);
		
		if (flowdef == null) {
			throw new UiException("Cannot find the specified Flow Definition file:"+_path);
		}
		
		//locate the anchor view component
		Component view = null;
		for(int j = 0; j < comps.length; ++j) {
			final Component comp = locateViewComponent(comps[j], _viewid);
			if (comp != null) {
				view = comp;
				break;
			}
		}
		if (view == null) {
			throw new UiException("Must have a \"@{view(content)}\" component as the anchor for view state changes!");
		}
		
		//get the flow
		FlowImpl flow = flowdef.newFlow(null, view);
		//flow.setView(view);

		//locate the root per the anchor view component and fire bookmark change
		prepareBookMarkChangeListener(view, flow);
	}
	
	public static Component locateViewComponent(Component comp, String viewid) {
		final Annotation annot = ((ComponentCtrl)comp).getAnnotation("view"); 
		if (annot != null && viewid.equals(annot.getAttribute("value"))) { //return the first found (deep first)
			return comp;
		}
		final List kids = comp.getChildren();
		for(final Iterator it = kids.iterator(); it.hasNext();) {
			final Component kid = (Component) it.next();
			final Component found = locateViewComponent(kid, viewid); //recursive
			if (found != null) {
				return found;
			}
		}
		return null;
	}
	
	/**
	 * Returns the topest root component of the specified component in page.
	 * @param comp the component
	 * @return the topest root component of the specified component in page.
	 */
	public static Component getComponentRoot(Component comp) {
		Component root = comp;
		while(root.getParent() != null) {
			root = root.getParent();
		}
		return root;
	}
	
	private void prepareBookMarkChangeListener(Component comp, FlowImpl flow) {
		Component root = getComponentRoot(comp);
		EventListener listener = 
			(EventListener) root.getAttribute(FLOW_BOOKMARK_LISTENER); 
		if (listener == null) { //
			listener = new FlowBookMarkChangeListener(flow);
			root.setAttribute(FLOW_BOOKMARK_LISTENER, listener);
			root.addEventListener(Events.ON_BOOKMARK_CHANGE, listener);
			root.addEventListener(ON_AFTER_LOAD, new AfterLoadBookmark1Listener(flow));
		}
		
		//fire echo event to force loading 1st bookmark if no other bookmark in URL
		final String bookmark1 = FlowImpl.FLOW_STATE0; //start flow loading
		flow.setUseBookmark1(true);
		Events.echoEvent(ON_AFTER_LOAD, root, bookmark1);
	}
	
	private static class FlowBookMarkChangeListener implements EventListener, Express {
		private FlowImpl _flow;
		
		public FlowBookMarkChangeListener(FlowImpl flow) {
			_flow = flow;
		}
		//if other bookmark in URL, this listener is called before {@link AfterLoadListner} 
		public void onEvent(Event event) throws Exception {
			//clear using 1st bookmark, so AfterLoadListener will not request 1st bookmark
			_flow.setUseBookmark1(false); 
			final BookmarkEvent evt = (BookmarkEvent) event;
			final String bookmark = evt.getBookmark();
			_flow.restore(bookmark);
		}
	}
	
	private static class AfterLoadBookmark1Listener implements EventListener, Express {
		private FlowImpl _flow;
		
		public AfterLoadBookmark1Listener(FlowImpl flow) {
			_flow = flow;
		}
		public void onEvent(Event event) throws Exception {
			//if no other bookmark in URL then use the 1st bookmark
			final Component target = event.getTarget();
			if (_flow.isUseBookmark1()) {
				final String bookmark1 = (String) event.getData();
				Events.sendEvent(target, new BookmarkEvent(Events.ON_BOOKMARK_CHANGE, bookmark1));
			}
		}
	}
	
	//TODO when 3.6.2 freshly released, use Servlets#getNormalizedPath(String path) instead
	/** Returns the normalized path; that is, will elminate the double dots ".."(parent) and 
	 * single dot "."(current) in the path. e.g. /abc/../def would be normalized to /def; /abc/./def would be
	 * normalized to /abc/def; /abc//def would be normalized to /abc/def.
	 * <p>Note that if found no way to navigate the path, it is deemed as an illegal path. e.g. 
	 * ../abc or /../abc or /../../abc is deemed as illegal path since we don't
	 * know how to continue doing the normalize.
	 * @since 3.6.2
	 */
	public static String getNormalizedPath(String path) {
		final StringBuffer sb = new StringBuffer(path);
		final IntStack slashes = new IntStack(32); //most 32 slash in a path
		slashes.push(-1);
		int j = 0, colon = -100, dot1 = -100, dot2 = -100;
		for (; j < sb.length(); ++j) {
			final char c = sb.charAt(j);
			switch(c) {
			case '/':
				if (dot1 >= 0) { //single dot or double dots
					if (dot2 >= 0) { //double dots
						int preslash = slashes.pop();
						if (preslash == 0) { //special case "/../"
							throw new IllegalArgumentException("Illegal path: "+path);
						}
						if (slashes.isEmpty()) {
							throw new IllegalArgumentException("Illegal path: "+path);
						}
						dot2 = -100;
					}
					int b = slashes.peek();
					sb.delete(b + 1, j+1);
					j = b;
					dot1 = -100;
				} else { //no dot
					int s = slashes.peek();
					if (s >= 0) {
						if (j == (s+1)) { //consequtive slashs
							if (colon == (s-1)) { //e.g. "http://abc"
								slashes.clear();
								slashes.push(-1);
								slashes.push(j);
							} else {
								--j;
								sb.delete(j, j+1);
							}
							continue;
						}
					}
					slashes.push(j);
				}
				break;
			case '.':
				if (dot1 < 0) {
					if (slashes.peek() == (j-1))
						dot1 = j;
				} else if (dot2 < 0){
					dot2 = j;
				} else { //more than 2 consecutive dots
					throw new IllegalArgumentException("Illegal path: "+path);
				}
				break;
			case ':': 
				if (colon >= 0) {
					throw new IllegalArgumentException("Illegal path: "+path);
				}
				colon = j;
			default:
				dot1 = dot2 = -100;
			}
		}
		return sb.toString();
	}
	
	private static class IntStack {
		private int _top = -1;
		private int[] _value;
		
		public IntStack(int sz) {
			_value = new int[sz];
		}
		public boolean isEmpty() {
			return _top < 0;
		}
		public int peek() {
			return _top >= 0 && _top < _value.length ? _value[_top] : -100;  
		}
		public int pop() {
			return _value[_top--];
		}
		public void push(int val) {
			_value[++_top] = val;
		}
		public void clear() {
			_top = -1;
		}
	}
}
