/* FlowDefinitions.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		Apr 30, 2009 12:46:35 PM, Created by henrichen
}}IS_NOTE

Copyright (C) 2009 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under GPL Version 2.0 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
*/

package org.zkoss.zwf.metainfo;

import java.io.File;
import java.net.URL;

import javax.servlet.ServletContext;

import org.zkoss.util.resource.Locator;
import org.zkoss.util.resource.ResourceCache;
import org.zkoss.web.util.resource.ResourceCaches;
import org.zkoss.web.util.resource.ResourceLoader;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.WebApp;
import org.zkoss.zk.ui.metainfo.PageDefinitions;
import org.zkoss.zwf.impl.ZKProxy;

/**
 * Parser for ZK Flow definition files.
 * @author henrichen
 *
 */
public class FlowDefinitions {
	private static final String ATTR_FLOW_CACHE = "org.zkoss.zk.ui.FlowCache";
	/** Returns the flow definition of the specified path, or null if not
	 * found or failed to parse.
	 *
	 * @param locator the locator used to locate taglib and other resources.
	 * If null, wapp is assumed ({@link WebApp} is also assumed).
	 */
	public static final
	FlowDefinition getFlowDefinition(String path) {
		final Session sess = Sessions.getCurrent();
		final WebApp wapp = sess.getWebApp();
		final Locator locator = FlowDefinitions.getLocator(wapp, path);

		return getFlowDefinition(wapp, locator, path);
	}
	
	/** Returns the flow definition of the specified path, or null if not
	 * found or failed to parse.
	 *
	 * @param locator the locator used to locate taglib and other resources.
	 * If null, wapp is assumed ({@link WebApp} is also assumed).
	 */
	public static final
	FlowDefinition getFlowDefinition(WebApp wapp, Locator locator, String path) {
		final Object ctx = wapp.getNativeContext();
		if (ctx instanceof ServletContext) {
			return (FlowDefinition)ResourceCaches.get(
				getCache(wapp), (ServletContext)ctx, path, locator);
		}
		throw new UnsupportedOperationException("Unknown context: "+ctx);
	}
	
	/** Returns the locator for the specified context.
	 *
	 * @param path the original path, or null if not available.
	 * The original path is used to resolve a relative path.
	 * If not specified, {@link org.zkoss.zk.ui.Desktop#getCurrentDirectory}
	 * is used.
	 */
	public static final Locator getLocator(WebApp wapp, String path) {
		return PageDefinitions.getLocator(wapp, path);
	}
	
	private static final ResourceCache getCache(WebApp wapp) {
		ResourceCache cache = (ResourceCache)wapp.getAttribute(ATTR_FLOW_CACHE);
		if (cache == null) {
			synchronized (PageDefinitions.class) {
				cache = (ResourceCache)wapp.getAttribute(ATTR_FLOW_CACHE);
				if (cache == null) {
					cache = new ResourceCache(new MyLoader(wapp), 167);
					cache.setMaxSize(1024);
					cache.setLifetime(60*60000); //1hr
					ZKProxy.getProxy().setAttribute(wapp, ATTR_FLOW_CACHE, cache);
				}
			}
		}
		return cache;
	}

	private static class MyLoader extends ResourceLoader {
		private final WebApp _wapp;
		private MyLoader(WebApp wapp) {
			_wapp = wapp;
		}

		//-- super --//
		protected Object parse(String path, File file, Object extra)
		throws Exception {
			final Locator locator =
				extra != null ? (Locator)extra: getLocator(_wapp, path);
			return new Parser(_wapp, locator).parse(file, path);
		}
		protected Object parse(String path, URL url, Object extra)
		throws Exception {
			final Locator locator =
				extra != null ? (Locator)extra: getLocator(_wapp, path);
			return new Parser(_wapp, locator).parse(url, path);
		}
	}
}
