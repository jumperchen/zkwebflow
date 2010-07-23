/* ZKProxy.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		Oct 28, 2009 3:10:07 PM, Created by henrichen
}}IS_NOTE

Copyright (C) 2009 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under GPL Version 2.0 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
*/

package org.zkoss.zwf.impl;

import java.lang.reflect.Method;

import javax.servlet.ServletRequest;

import org.zkoss.lang.Classes;
import org.zkoss.zk.scripting.Namespace;
import org.zkoss.zk.scripting.Namespaces;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Execution;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.UiException;
import org.zkoss.zk.ui.WebApp;
import org.zkoss.zk.ui.ext.Scope;
import org.zkoss.zk.ui.ext.Scopes;
import org.zkoss.zk.ui.sys.DesktopCtrl;
import org.zkoss.zk.ui.sys.ExecutionCtrl;

/**
 * A proxy used to access ZK functions that depends on ZK versions.
 * 
 * @author henrichen
 */
public class ZKProxy {
	private static Proxy _proxy;
	
	/** Reeturns the ZK Proxy used to access version-dependent features.
	 */
	public static Proxy getProxy() {
		if (_proxy == null) {//no need to synchronized
			try {
				Classes.forNameByThread("org.zkoss.zk.ui.sys.PageRenderer");
				_proxy = newProxy5();
			} catch (ClassNotFoundException ex) {
				_proxy = newProxy3();
			}
		}
		return _proxy;
	}
	
	/** Interface to access version-dependent features of ZK.
	 */
	public static interface Proxy {
		/** 
		 * Returns self of current context.
		 * @return self of current context.
		 */
		public Component getSelf(ExecutionCtrl exec);
		/** Sets an WebApp attribute.
		 */
		public void setAttribute(WebApp webapp, String name, Object value);
		/** Sets an session attribute.
		 */
		public void setAttribute(Session sess, String name, Object value);
		
		/** Sets an execution attribute.
		 */
		public void setAttribute(Execution exec, String name, Object value);
		
		/** Removes an execution attribute.
		 */
		public void removeAttribute(Execution exec, String name);
	}
	
	private static Proxy newProxy5() {
		return new Proxy() {
			public void setAttribute(Execution exec, String name, Object value) {
				exec.setAttribute(name, value);
			}

			public void setAttribute(WebApp webapp, String name, Object value) {
				webapp.setAttribute(name, value);
			}
			
			public void setAttribute(Session sess, String name, Object value) {
				sess.setAttribute(name, value);
			}
			
			public void removeAttribute(Execution exec, String name) {
				exec.removeAttribute(name);
			}

			public Component getSelf(ExecutionCtrl exec) {
				final Page page = exec.getCurrentPage();
				final Scope scope = Scopes.getCurrent(page);
				if (scope != null) {
					Component self = (Component) scope.getAttribute("self", true);
					if (self == null) {
						self = (Component) Scopes.getImplicit("self", null);
					}
					return self;
				}
				return null;
			}
		};
	}
	
	private static Proxy newProxy3() {
		return new Proxy() {
			public void setAttribute(Execution exec, String name, Object value) {
				((ServletRequest)exec.getNativeRequest()).setAttribute(name, value);
				//can't access setAttribute directly, since signature of ZK 5 changed
			}

			public void setAttribute(Session sess, String name, Object value) {
				final Object nsess = sess.getNativeSession();
				if (nsess instanceof javax.portlet.PortletSession) {
					((javax.portlet.PortletSession) nsess).setAttribute(name, value);
				} else { //	if (nsess instanceof javax.servlet.http.HttpSession) {
					((javax.servlet.http.HttpSession) nsess).setAttribute(name, value);
				}
			}
			
			public void setAttribute(WebApp webapp, String name, Object value) {
				((javax.servlet.ServletContext)webapp.getNativeContext()).setAttribute(name, value);
			}
			
			public void removeAttribute(Execution exec, String name) {
				((ServletRequest)exec.getNativeRequest()).removeAttribute(name);
				//can't access removeAttribute directly, since signature of ZK 5 changed
			}
			
			public Component getSelf(ExecutionCtrl exec) {
				final Page page = exec.getCurrentPage();
				final Namespace ns = Namespaces.getCurrent(page);
				if (ns != null) {
					Component self = (Component) ns.getVariable("self", false);
					//since ZK 3.6.1, event handling, use getImplicit()
					if (self == null) {
						self = (Component) Namespaces.getImplicit("self", null);
					}
					return self;
				}
				return null;
			}
		};
	}
}
