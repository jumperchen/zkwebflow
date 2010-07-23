/* FlowDefinition.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		Apr 30, 2009 2:53:33 PM, Created by henrichen
}}IS_NOTE

Copyright (C) 2009 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under GPL Version 2.0 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
*/

package org.zkoss.zwf.metainfo;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.zkoss.util.resource.Locator;
import org.zkoss.xel.ExpressionFactory;
import org.zkoss.xel.Expressions;
import org.zkoss.xel.Function;
import org.zkoss.xel.FunctionMapper;
import org.zkoss.xel.VariableResolver;
import org.zkoss.xel.taglib.Taglib;
import org.zkoss.xel.taglib.Taglibs;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.metainfo.FunctionMapperInfo;
import org.zkoss.zk.ui.metainfo.VariableResolverInfo;
import org.zkoss.zk.ui.sys.ExecutionCtrl;
import org.zkoss.zk.xel.Evaluator;
import org.zkoss.zk.xel.impl.EvaluatorRef;
import org.zkoss.zk.xel.impl.SimpleEvaluator;
import org.zkoss.zwf.impl.FlowImpl;
import org.zkoss.zwf.impl.SubflowStateImpl;

/**
 * &lt;flow> tag in ZK Web Flow definition. It represents an ZK Web Flow definition.  
 * @author henrichen
 */
public class FlowDefinition extends NodeInfo implements RealNode {
	public static final String FLOW_CONTEXT = "zkoss.zwf.FLOW_CONTEXT";

	private Locator _locator;
	private String _path;
	private String _zscriptLanguage;
	private String _apply;
	private EvaluatorRef _evaluatorRef;
	private Evaluator _eval;
	/** The function mapper. */
	private FunctionMapper _mapper;
	private List _taglibs;
	/** The expression factory (ExpressionFactory).*/
	private Class _expfcls;
	/** A map of XEL methods, List<[String prefix, String name, Function func]>. */
	private List _xelmtds;
	/** List(VariableResolverInfo). */
	private List _resolvdefs;
	/** List(FunctionMapper mapper). */
	private List _mapperdefs;
	/** A map of imported classes for expression, Map<String nm, Class cls>. */
	private Map _expimps;
	
	public FlowDefinition(Locator locator) {
		super(null);
		_locator = locator;
	}
	
	public Locator getLocator() {
		return _locator;
	}
	
	public FlowDefinition getFlowDefinition() {
		return this;
	}
	
	public void setPath(String path) {
		_path = path;
	}
	
	public InputStream getInputStream(String name) {
		return _locator.getResourceAsStream(name);
	}
	
	public StateInfo getFirstState() {
		for (final Iterator it = _children.iterator(); it.hasNext();) {
			final Object kid = it.next();
			if (kid instanceof StateInfo) {
				return (StateInfo) kid; 
			}
		}
		return null;
	}
	
	public void setZScriptLanguage(String lang) {
		_zscriptLanguage = lang;
	}
	/** Returns the zscript language used in this flow. If not specified, then use
	 * the one defined in currently operated ZUML page 
	 */
	public String getZScriptLanguage() {
		if (_zscriptLanguage == null) {
			final Page page = ((ExecutionCtrl)Executions.getCurrent()).getCurrentPage(); 
			return page.getZScriptLanguage();
		}
		return _zscriptLanguage;
	}
	
	/** Imports the specified directives from the specified ZK Web Flow definition.
	 *
	 * @param flowdef the page definition to import from.
	 * @param directives an array of the directive names to import.
	 * If null, {"taglib", and "xel-method"} is assumed, i.e., only the
	 * taglib and xel-method definitions are imported.<br/>
	 * Importable directives include "taglib", "variable-resolver", "xel-method"
	 * , and "function-mapper".
	 * If "*", all of them are imported.
	 */
	public void imports(FlowDefinition flowdef, String[] directives) {
		if (flowdef._taglibs != null
		&& (directives == null || contains(directives, "taglib"))) {
			for (Iterator it = flowdef._taglibs.iterator(); it.hasNext();)
				addTaglib((Taglib)it.next());
		}

		if (flowdef._resolvdefs != null
		&& directives != null && contains(directives, "variable-resolver")) {
			for (Iterator it = flowdef._resolvdefs.iterator(); it.hasNext();)
				addVariableResolverInfo((VariableResolverInfo)it.next());
		}

		if (flowdef._mapperdefs != null
		&& directives != null && contains(directives, "function-mapper")) {
			for (Iterator it = flowdef._mapperdefs.iterator(); it.hasNext();)
				addFunctionMapperInfo((FunctionMapperInfo)it.next());
		}

		if (flowdef._xelmtds != null
		&& (directives == null || contains(directives, "xel-method"))) {
			for (Iterator it = flowdef._xelmtds.iterator(); it.hasNext();) {
				final Object[] inf = (Object[])it.next();
				addXelMethod((String)inf[0], (String)inf[1], (Function)inf[2]);
			}
		}
	}
	private static boolean contains(String[] dirs, String dir) {
		for (int j = dirs.length; --j >= 0;)
			if ("*".equals(dirs[j]) || dir.equalsIgnoreCase(dirs[j]))
				return true;
		return false;
	}
	/** Imports the init directives and component definitions from
	 * the specified page definition.
	 *
	 * <p>It is the same as imports(flowdef, null).
	 */
	public void imports(FlowDefinition flowdef) {
		imports(flowdef, null);
	}

	/** Adds a defintion of {@link VariableResolver}.
	 */
	public void addVariableResolverInfo(VariableResolverInfo resolver) {
		if (resolver == null)
			throw new IllegalArgumentException("null");

		if (_resolvdefs == null)
			_resolvdefs = new LinkedList();
		_resolvdefs.add(resolver);
	}
	/** Adds a defintion of {@link FunctionMapper}.
	 * @since 3.5.0
	 */
	public void addFunctionMapperInfo(FunctionMapperInfo mapper) {
		if (mapper == null)
			throw new IllegalArgumentException("null");

		if (_mapperdefs == null)
			_mapperdefs = new LinkedList();
		_mapperdefs.add(mapper);
	}
	/** Adds a XEL method.
	 *
	 * @param prefix the prefix of the method name
	 * @param name the method name. The final name is "prefix:name"
	 * @param func the function.
	 * @since 3.0.0
	 */
	public void addXelMethod(String prefix, String name, Function func) {
		if (name == null || prefix == null || func == null)
			throw new IllegalArgumentException();
		if (_xelmtds == null)
			_xelmtds = new LinkedList();
		_xelmtds.add(new Object[] {prefix, name, func});
		_eval = null; //ask for re-gen
		_mapper = null; //ask for re-parse
	}

	/** Initializes XEL context for the specified page.
	 *
	 * @param page the page to initialize the context. It cannot be null.
	 */
/*
//TODO, Unmark until 3.6.2 freshly is released 
  	public void initXelContext(Page page) {
		page.addFunctionMapper(getTaglibMapper());

		if (_mapperdefs != null)
			for (Iterator it = _mapperdefs.iterator(); it.hasNext();) {
				final FunctionMapperInfo def = (FunctionMapperInfo)it.next();
				try {
					FunctionMapper mapper =
						def.newFunctionMapper(getEvaluator(), page);
					if (mapper != null) 
						page.addFunctionMapper(mapper);
				} catch (Throwable ex) {
					throw UiException.Aide.wrap(ex);
				}
			}

		if (_resolvdefs != null)
			for (Iterator it = _resolvdefs.iterator(); it.hasNext();) {
				final VariableResolverInfo def = (VariableResolverInfo)it.next();
				try {
					VariableResolver resolver =
						def.newVariableResolver(getEvaluator(), page);
					if (resolver != null) 
						page.addVariableResolver(resolver);
				} catch (Throwable ex) {
					throw UiException.Aide.wrap(ex);
				}
			}
	}
*/
	/** Adds a tag lib. */
	public void addTaglib(Taglib taglib) {
		if (taglib == null)
			throw new IllegalArgumentException("null");

		if (_taglibs == null)
			_taglibs = new LinkedList();
		_taglibs.add(taglib);
		_eval = null; //ask for re-gen
		_mapper = null; //ask for re-parse
	}
	/** Adds an imported class to the expression factory.
	 * @since 3.0.0
	 */
	public void addExpressionImport(String nm, Class cls) {
		if (nm == null || cls == null)
			throw new IllegalArgumentException();
		if (_expimps == null)
			_expimps = new HashMap(4);
		_expimps.put(nm, cls);
		_eval = null; //ask for re-gen
		_mapper = null; //ask for re-parse
	}
	/** Sets the implementation of the expression factory that shall
	 * be used by this page.
	 *
	 * <p>Default: null (use the default).
	 *
	 * @param expfcls the implemtation class, or null to use the default.
	 * Note: expfcls must implement {@link ExpressionFactory}.
	 * If null is specified, the class defined in
	 * {@link org.zkoss.zk.ui.util.Configuration#getExpressionFactoryClass}
	 */
	public void setExpressionFactoryClass(Class expfcls) {
		if (expfcls != null && !ExpressionFactory.class.isAssignableFrom(expfcls))
			throw new IllegalArgumentException(expfcls+" must implement "+ExpressionFactory.class);
		_expfcls = expfcls;
	}
	/** Returns the implementation of the expression factory that
	 * is used by this page, or null if
	 * {@link org.zkoss.zk.ui.util.Configuration#getExpressionFactoryClass}
	 * is used.
	 *
	 * @see #setExpressionFactoryClass
	 */
	public Class getExpressionFactoryClass() {
		return _expfcls;
	}

	/** Returns the evaluator based on this ZK Web Flow definition (never null).
	 */
	public Evaluator getEvaluator() {
		if (_eval == null)
			_eval = newEvaluator();
		return _eval;
	}
	private Evaluator newEvaluator() {
		return new SimpleEvaluator(getTaglibMapper(), _expfcls);
	}
	/** Returns the mapper representing the functions defined in
	 * taglib and xel-method.
	 */
	public FunctionMapper getTaglibMapper() {
		if (_mapper == null) {
			_mapper = Taglibs.getFunctionMapper(_taglibs, null, _xelmtds, _locator);
			if (_mapper == null)
				_mapper = Expressions.EMPTY_MAPPER;
		}
		return _mapper != Expressions.EMPTY_MAPPER ? _mapper: null;
	}
	/** Returns the evaluator reference (never null).
	 */
	public EvaluatorRef getEvaluatorRef() {
		if (_evaluatorRef == null)
			_evaluatorRef = newEvaluatorRef();
		return _evaluatorRef;
	}
	private EvaluatorRef newEvaluatorRef() {
		return new FlowEvalRef(this);
	}
	protected void setApply(String val) {
		_apply = val;
	}
	public FlowImpl newFlow(SubflowStateImpl subflowState, Component comp) {
		return new FlowImpl(this, subflowState, comp);
	}
}
