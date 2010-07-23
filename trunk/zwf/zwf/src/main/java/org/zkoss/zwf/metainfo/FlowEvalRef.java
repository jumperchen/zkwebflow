/* FlowEvalRef.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		Apr 30, 2009 8:02:09 PM, Created by henrichen
}}IS_NOTE

Copyright (C) 2009 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under GPL Version 2.0 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
*/
package org.zkoss.zwf.metainfo;

import org.zkoss.xel.Expression;
import org.zkoss.xel.FunctionMapper;
import org.zkoss.xel.XelException;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.metainfo.PageDefinition;
import org.zkoss.zk.ui.sys.ExecutionCtrl;
import org.zkoss.zk.xel.Evaluator;
import org.zkoss.zk.xel.impl.EvaluatorRef;
import org.zkoss.zk.xel.impl.SimpleEvaluator;

/**
 * The evaluator reference based on flow definition.
 * Used by {@link FlowDefinition} only.
 * 
 * @author henrichen
 *
 */
public class FlowEvalRef implements EvaluatorRef, java.io.Serializable {
	private transient FlowDefinition _flowdef;
	
	/** Used only if _flowdef == null. */
	private transient Evaluator _eval;
	/** The implementation of the expression factory.
	 * Used only if _flowdef == null.
	 */
	private Class _expfcls;
	/** The function mapper for the evaluator.
	 * Used only if _flowdef == null.
	 */
	private FunctionMapper _mapper;
	
	/*package*/ FlowEvalRef(FlowDefinition flowdef) {
		_flowdef = flowdef;
	}

	//Evaluator//
	public Expression parseExpression(String expression, Class expectedType)
	throws XelException {
		return getEvaluator().parseExpression(expression, expectedType);
	}
	public Object evaluate(Page page, Expression expression)
	throws XelException {
		return getEvaluator().evaluate(page, expression);
	}
	public Object evaluate(Component comp, Expression expression)
	throws XelException {
		return getEvaluator().evaluate(comp, expression);
	}

	//EvaluatorRef//
	public Evaluator getEvaluator() {
		if (_flowdef != null)
			return _flowdef.getEvaluator();
		if (_eval == null)
			_eval = new SimpleEvaluator(_mapper, _expfcls);
		return _eval;
	}
	
	//Dummy implementation
	public PageDefinition getPageDefinition() {
		return null;//((ExecutionCtrl)Executions.getCurrent()).getCurrentPageDefinition();
	}
	
	//Serializable//
	private synchronized void writeObject(java.io.ObjectOutputStream s)
	throws java.io.IOException {
		s.defaultWriteObject();
		s.writeObject(_flowdef != null ? _flowdef.getExpressionFactoryClass(): _expfcls);
		s.writeObject(_flowdef != null ? _flowdef.getTaglibMapper(): _mapper);
	}
	private synchronized void readObject(java.io.ObjectInputStream s)
	throws java.io.IOException, ClassNotFoundException {
		s.defaultReadObject();
		_expfcls = (Class)s.readObject();
		_mapper = (FunctionMapper)s.readObject();
	}
}
