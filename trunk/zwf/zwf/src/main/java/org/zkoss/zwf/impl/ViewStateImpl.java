/* ViewState.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		May 2, 2009 9:38:31 PM, Created by henrichen
}}IS_NOTE

Copyright (C) 2009 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under GPL Version 2.0 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
*/

package org.zkoss.zwf.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.zkoss.util.logging.Log;
import org.zkoss.util.resource.Locator;
import org.zkoss.web.servlet.Servlets;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Execution;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.UiException;
import org.zkoss.zk.ui.WebApp;
import org.zkoss.zk.ui.event.BookmarkEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.metainfo.Annotation;
import org.zkoss.zk.ui.metainfo.EventHandler;
import org.zkoss.zk.ui.metainfo.PageDefinition;
import org.zkoss.zk.ui.metainfo.PageDefinitions;
import org.zkoss.zk.ui.sys.ComponentCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Window;
import org.zkoss.zwf.FlowHandler;
import org.zkoss.zwf.ViewState;
import org.zkoss.zwf.metainfo.FlowDefinition;
import org.zkoss.zwf.metainfo.NodeInfo;
import org.zkoss.zwf.metainfo.StateInfo;
import org.zkoss.zwf.metainfo.ViewStateInfo;

/**
 * Runtime instance of the {@link ViewStateInfo}.
 * @author henrichen
 *
 */
public class ViewStateImpl extends StateImpl implements ViewState {
	private static final Log log = Log.lookup(ViewStateImpl.class);
	private static final String POPUP_TEMPLATE = "~./zwf/popupTemplate.zul";
	
	private transient String _path;
	private TransitListener _transitListener;
	
	public ViewStateImpl(NodeInfo info) {
		super(info);
	}
	public String getUri() {
		return (String) _info.getAttribute("uri");
	}
	public void enter() {
		super.enter();
		renderView();
		
		//flashScope cleared after rendering
		final FlowImpl flow = getFlow();
		flow.getFlashScope().clear();
	}
	
	public String getPopup() {
		return (String)_info.getAttribute("popup");
	}
	
	protected Component getView(boolean render) {
		if (!render) {
			return getFlow().getView();
		}
		final Component view = getFlow().getView();
		
		String popup = getPopup();
		if ("yes".equalsIgnoreCase(popup) || "true".equalsIgnoreCase(popup)) {
			popup = POPUP_TEMPLATE; //use embedded popupTemplate
		} 
		
		//no popup
		if (popup == null || "no".equalsIgnoreCase(popup)) {
			return view;
		}
		
		//prepare the popupTempalte per the given popup uri
		final Component comp = renderView0(popup, view);
		if (!(comp instanceof Window)) {
			throw new UiException("Flow's popup template file must be rooted with a Window: "+popup);
		}
		final Window win = (Window) comp;
		try {
			win.addEventListener("onClose", new EventListener() {
				//when user press [x] of the Window, simulate a back button click
				public void onEvent(Event event) throws Exception {
					Clients.evalJavaScript("history.go(-1)");
				}
			});
			win.setMode("highlighted");
			return FlowHandler.locateViewComponent(win, "content"); //return the content anchor point of the popup window
		} catch (InterruptedException ex) {
			//ignore
			return view;
		}
	}
	
	public void exit() {
		//unregister TransitListener
		final Component view = getView(false);
		for (final Iterator it=view.getChildren().iterator(); it.hasNext();) {
			unregisterTransitListener((Component)it.next());
		}
		super.exit();
	}

	protected void restore() {
		renderView();
		//TODO fire restore event
	}
	
	/**
	 * Returns the path of this ViewState in the flow hierarchy. 
	 * @return the path of this ViewState in the flow hierarchy.
	 */
	public String getStatePath() {
		if (_path == null) {
			final FlowImpl flow = getFlow();
			_path = flow.getPath() + "/" + getId(); 
		}
		return _path;
	}
	
	private Component renderView0(String uri, Component parent) {
		if (uri != null && uri.length() > 0 && uri.charAt(0) != '/' && uri.charAt(0) != '~') {
			final Locator locator = ((FlowDefinition)getFlow().getInfo()).getLocator();
			uri = FlowHandler.getNormalizedPath(locator.getDirectory() + uri); 
		}
		final Execution exec = Executions.getCurrent();
		return exec.createComponents(uri, parent, null);
	}
	
	protected void renderView() {
		final Component view = getView(true);
		view.getChildren().clear();
		
		String uris = getUri();
		if (uris == null) {
			//TODO uri name pattern id + ".zul"; could be configurable
			uris = getId() + ".zul";
		}
		String[] split = uris.split(",", 0);
		for (int j= 0; j < split.length; ++j) {
			final String uri = split[j].trim();
			if (log.debugable()) log.debug("renderView:"+uri);
			
			//create components
			renderView0(uri, view);
				
			//register TransitListener
			for (final Iterator it=view.getChildren().iterator(); it.hasNext();) {
				registerTransitListener((Component)it.next());
			}
		}
	}
	
	/**
	 * Internal use only
	 * @param comp
	 */
	private void registerTransitListener(Component comp) {
		final Annotation annot = ((ComponentCtrl)comp).getAnnotation("action"); 
		if (annot != null) {
			String when = annot.getAttribute("when");
			if (when == null) {
				when = "onClick";
			}
			comp.addEventListener(when, getTransitListener());
		}
		final List kids = comp.getChildren();
		for(final Iterator it = kids.iterator(); it.hasNext();) {
			final Component kid = (Component) it.next();
			registerTransitListener(kid); //recursive to child component
		}
	}
	/**
	 * Internal use only
	 * @param comp
	 */
	private void unregisterTransitListener(Component comp) {
		final Annotation annot = ((ComponentCtrl)comp).getAnnotation("action"); 
		if (annot != null) {
			String when = annot.getAttribute("when");
			if (when == null) {
				when = "onClick";
			}
			comp.removeEventListener(when, getTransitListener());
		}
		final List kids = comp.getChildren();
		for(final Iterator it = kids.iterator(); it.hasNext();) {
			final Component kid = (Component) it.next();
			unregisterTransitListener(kid); //recursive to child component
		}
	}
	
	private TransitListener getTransitListener() {
		if (_transitListener == null) {
			_transitListener = new TransitListener();
		}
		return _transitListener;
	}
	
	private class TransitListener implements EventListener {
		public void onEvent(Event event) throws Exception {
			final Component target = event.getTarget();
			final Annotation annot = ((ComponentCtrl)target).getAnnotation("action");
			final String action = annot.getAttribute("value");
			getFlow().transit(ViewStateImpl.this, action, false);
		}
	}
}
