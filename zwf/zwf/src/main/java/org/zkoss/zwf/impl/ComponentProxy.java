/* ComponentProxy.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		May 22, 2009 11:12:41 AM, Created by henrichen
}}IS_NOTE

Copyright (C) 2009 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under GPL Version 2.0 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
*/

package org.zkoss.zwf.impl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.zkoss.zk.scripting.Namespaces;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.UiException;
import org.zkoss.zk.ui.sys.ExecutionCtrl;
import org.zkoss.zwf.Flow;

/**
 * Dynamic Component Proxy to be used with changing view pages.
 * @author henrichen
 * @see org.zkoss.zwf.GenericFlowComposer
 */
public class ComponentProxy implements InvocationHandler {
	private Component _ref; //Initial reference component
	private String _fdname; //field name
	private ComponentProxy(Component ref, String fdname) {
		_ref = ref;
		_fdname = fdname;
	}
	/**
	 * Returns the Proxy to the ZK UI component in the specified referenced component context.
	 * @param interfaces ZK ui component interface org.zkoss.zul.ui.api.*
	 * @param ref the reference component
	 * @return the Proxy to the ZK UI component in the specified {@link Flow} context.
	 */
	public static Object newInstance(Class[] interfaces, Component ref, String fdname) {
		return Proxy.newProxyInstance(Thread.currentThread()
				.getContextClassLoader(), interfaces, new ComponentProxy(ref, fdname));
	}

	//--InvocationHandler--//
	public Object invoke(Object proxy, Method method, Object[] args)
	throws Throwable {
		
		final Object target = getVariable(_fdname);
		final Class cls = method.getDeclaringClass();
		if (target == null) { //fail to find the target
			return null;
		}
		if (!cls.isInstance(target))
			throw new UiException("Cannot find the specified component: "+_fdname+". Expect an instance of "+cls+", but is "+target);
		
		return method.invoke(target, args);
	}

	private Component getRef() {
		Component ref = ZKProxy.getProxy().getSelf((ExecutionCtrl)Executions.getCurrent());
		if (ref == null || ref.getPage() == null) {
			ref = _ref;
		}
		return ref;
	}
	private Object getVariable(String fdname) {
		final Component ref = getRef();
		Object arg = ref.getPage().getZScriptVariable(ref, fdname);
		if (arg == null) {
			arg = ref.getVariable(fdname, false);
		}
		return arg;
	}
}
