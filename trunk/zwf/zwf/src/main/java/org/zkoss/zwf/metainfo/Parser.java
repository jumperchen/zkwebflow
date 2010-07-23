/* Parser.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		Apr 30, 2009 2:53:51 PM, Created by henrichen
}}IS_NOTE

Copyright (C) 2009 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under GPL Version 2.0 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
*/

package org.zkoss.zwf.metainfo;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.zkoss.idom.Attribute;
import org.zkoss.idom.CData;
import org.zkoss.idom.Document;
import org.zkoss.idom.Element;
import org.zkoss.idom.Item;
import org.zkoss.idom.ProcessingInstruction;
import org.zkoss.idom.Text;
import org.zkoss.idom.input.SAXBuilder;
import org.zkoss.idom.util.IDOMs;
import org.zkoss.lang.Classes;
import org.zkoss.lang.PotentialDeadLockException;
import org.zkoss.lang.Strings;
import org.zkoss.util.CollectionsX;
import org.zkoss.util.logging.Log;
import org.zkoss.util.resource.Locator;
import org.zkoss.web.servlet.Servlets;
import org.zkoss.xel.taglib.Taglib;
import org.zkoss.xel.util.Evaluators;
import org.zkoss.xel.util.MethodFunction;
import org.zkoss.zk.ui.UiException;
import org.zkoss.zk.ui.WebApp;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.metainfo.FunctionMapperInfo;
import org.zkoss.zk.ui.metainfo.VariableResolverInfo;
import org.zkoss.zk.ui.metainfo.ZScript;

/**
 * Used to parse ZK Flow definition file.
 * @author henrichen
 *
 */
public class Parser {
	private static final Log log = Log.lookup(Parser.class);

	private final WebApp _wapp;
	private final Locator _locator;

	/** Constructor.
	 *
	 * @param locator the locator used to locate taglib and other resources.
	 * If null, wapp is assumed ({@link WebApp} is also assumed).
	 */	
	public Parser(WebApp wapp, Locator locator) {
		if (wapp == null)
			throw new IllegalArgumentException("null");
		_wapp = wapp;
		_locator = locator != null ? locator: (Locator)wapp;
	}
	
	/** Parses the specified file.
	 *
	 * @param path the request path.
	 * It is used as {@link org.zkoss.zwf.impl.FlowImpl#getPath}, or null
	 * if not available.
	 */
	public FlowDefinition parse(File file, String path) throws Exception {
		final FlowDefinition flowdef =
			parse(new SAXBuilder(true, false, true).build(file));
		flowdef.setPath(path);
		return flowdef;
	}
	/** Parses the specified URL.
	 *
	 * @param path the request path.
	 * It is used as {@link org.zkoss.zwf.impl.FlowImpl#getPath}, or null
	 * if not available.
	 */
	public FlowDefinition parse(URL url, String path) throws Exception {
		final FlowDefinition flowdef =
			parse(new SAXBuilder(true, false, true).build(url));
		flowdef.setPath(path);
		return flowdef;
	}


	/** Parss the raw content directly from a DOM tree.
	 * @doc the DOM document
	 */
	public FlowDefinition parse(Document doc)
	throws Exception {
		//1. parse the import directive if any
		final List pis = new LinkedList(), imports = new LinkedList();
		String lang = null;
		for (Iterator it = doc.getChildren().iterator(); it.hasNext();) {
			final Object o = it.next();
			if (!(o instanceof ProcessingInstruction)) continue;

			final ProcessingInstruction pi = (ProcessingInstruction)o;
			final String target = pi.getTarget();
			if ("import".equals(target)) { //import
				final Map params = pi.parseData();
				final String src = (String)params.remove("src");
				final String dirs = (String)params.remove("directives");
				if (!params.isEmpty())
					log.warning("Ignored unknown attributes: "+params.keySet()+", "+pi.getLocator());
				noELnorEmpty("src", src, pi);
				noEL("directives", dirs, pi);
				imports.add(new String[] {src, dirs});
			} else {
				pis.add(pi);
			}
		}

		//2. Create FlowDefinition
		final FlowDefinition flowdef = new FlowDefinition(getLocator());

		//3. resolve imports
		if (!imports.isEmpty()) {
			for (Iterator it = imports.iterator(); it.hasNext();) {
				final String[] imprt = (String[])it.next();
				final String path = imprt[0], dirs = imprt[1];
				try {
					final FlowDefinition fd = FlowDefinitions.getFlowDefinition(_wapp, _locator, path);
					if (fd == null)
						throw new UiException("Import page not found: "+path);
					flowdef.imports(fd, parseToArray(dirs));
				} catch (PotentialDeadLockException ex) {
					throw new UiException("Recursive import: "+path, ex);
				}
			}
		}

		//4. Processing the rest of processing instructions at the top level
		for (Iterator it = pis.iterator(); it.hasNext();)
			parse(flowdef, (ProcessingInstruction)it.next());

		//5. Processing from the root element
		final Element root = doc.getRootElement();
		//root must be <flow>
		if (root == null || !root.getLocalName().equals("flow")) {
			throw new UiException("root element must be <flow>: "+root.getLocator());
		}
		parse(flowdef, flowdef, root);
		
		return flowdef;
	}

	/** Parses Process Instruction
	 * 
	 */
	private void parse(FlowDefinition flowdef, ProcessingInstruction pi) 
	throws Exception {
		final String target = pi.getTarget();
		final Map params = pi.parseData();
		if ("variable-resolver".equals(target)
		|| "function-mapper".equals(target)) {
			final String clsnm = (String)params.remove("class");
			if (isEmpty(clsnm))
				throw new UiException("The class attribute is required, "+pi.getLocator());

			final Map args = new LinkedHashMap(params);
			if (!params.isEmpty())
				log.warning("Ignored unknown attributes: "+params.keySet()+", "+pi.getLocator());

			if ("variable-resolver".equals(target))
				flowdef.addVariableResolverInfo(
					clsnm.indexOf("${") >= 0 ? //class supports EL
						new VariableResolverInfo(clsnm, args):
						new VariableResolverInfo(locateClass(clsnm), args));
			else
				flowdef.addFunctionMapperInfo(
					clsnm.indexOf("${") >= 0 ? //class supports EL
						new FunctionMapperInfo(clsnm, args):
						new FunctionMapperInfo(locateClass(clsnm), args));
		} else if ("taglib".equals(target)) {
			final String uri = (String)params.remove("uri");
			final String prefix = (String)params.remove("prefix");
			if (!params.isEmpty())
				log.warning("Ignored unknown attributes: "+params.keySet()+", "+pi.getLocator());
			if (uri == null || prefix == null)
				throw new UiException("Both uri and prefix attribute are required, "+pi.getLocator());
			noEL("prefix", prefix, pi);
			noEL("uri", uri, pi); //not support EL (kind of chicken-egg issue)
			flowdef.addTaglib(new Taglib(prefix, toAbsoluteURI(uri, false)));
		} else if ("evaluator".equals(target)) {
			parseEvaluatorDirective(flowdef, pi, params);
		} else if ("xel-method".equals(target)) {
			parseXelMethod(flowdef, pi, params);
		} else if ("import".equals(target)) { //import
			throw new UiException("The import directive can be used only at the top level, "+pi.getLocator());
		} else {
			log.warning("Unknown processing instruction: "+target+", "+pi.getLocator());
		}
	}
	
	/** Parses the specified elements.
	 */
	private void parse(FlowDefinition flowdef, NodeInfo parent, Collection items)
	throws Exception {
		for (Iterator it = items.iterator(); it.hasNext();) {
			final Object o = it.next();
			if (o instanceof Element) {
				parse(flowdef, parent, (Element)o);
			} else if (o instanceof ProcessingInstruction) {
				parse(flowdef, (ProcessingInstruction)o);
			} else if ((o instanceof Text) || (o instanceof CData)) {
				String label = ((Item)o).getText().trim();
				
				//Ingore blank text
				if (label.length() == 0)
					continue;
				else {
					throw new UiException("Unknown text: "+ 
							(o instanceof Text ? ((Text)o).getLocator() : ((CData)o).getLocator()));
				}
			}
		}
	}
	
	/** Parse an component definition specified in the given element.
	 * @param bNativeContent whether to consider the child element all native
	 * It is true if a component definition with text-as is found
	 */
	private void parse(FlowDefinition flowdef, NodeInfo parent, Element el)
	throws Exception {
		final String nm = el.getLocalName();
		NodeInfo info = null;
		if ("view-state".equals(nm)){
			info = parseViewState(flowdef, parent, el);
		} else if ("transition".equals(nm)) {
			info = parseTransition(flowdef, parent, el);
		} else if ("zscript".equals(nm)) {
			parseZScript(flowdef, parent, el);
		} else if ("attribute".equals(nm)) {
			if (!(parent instanceof RealNode))
				throw new UiException("<attribute> cannot be the root element, "+el.getLocator());
			parseAttribute(flowdef, parent, el);
		} else if ("subflow-state".equals(nm)) {
			info = parseSubflowState(flowdef, parent, el);
		} else if ("end-state".equals(nm)) {
			info = parseEndState(flowdef, parent, el);
		} else if ("action-state".equals(nm)) {
			info = parseActionState(flowdef, parent, el);
		} else if ("flow".equals(nm)) {
			info = parseFlow(flowdef, parent, el);
		} else {
			throw new UiException("Unknown tag \""+nm+"\" in ZK Web Flow definition file, "+el.getLocator());
		}
		
		if (info != null) {
			parse(flowdef, info, el.getChildren()); //recursive
		} 
	}
	
	private NodeInfo parseTransition(FlowDefinition flowdef, NodeInfo parent, Element el) 
	throws Exception {
		final TransitionInfo info = new TransitionInfo(flowdef); 
		parent.appendChild(info);
		addAttributes(flowdef, info, el);
		return info;
	}
	
	private NodeInfo parseViewState(FlowDefinition flowdef, NodeInfo parent, Element el) 
	throws Exception {
		final ViewStateInfo info = new ViewStateInfo(flowdef); 
		parent.appendChild(info);
		addAttributes(flowdef, info, el);
		return info;
	}
	
	private NodeInfo parseEndState(FlowDefinition flowdef, NodeInfo parent, Element el) 
	throws Exception {
		final EndStateInfo info = new EndStateInfo(flowdef); 
		parent.appendChild(info);
		addAttributes(flowdef, info, el);
		return info;
	}
	
	private NodeInfo parseActionState(FlowDefinition flowdef, NodeInfo parent, Element el)
	throws Exception {
		final ActionStateInfo info = new ActionStateInfo(flowdef);
		parent.appendChild(info);
		addAttributes(flowdef, info, el);
		return info;
	}
	
	private NodeInfo parseSubflowState(FlowDefinition flowdef, NodeInfo parent, Element el) 
	throws Exception {
		final SubflowStateInfo info = new SubflowStateInfo(flowdef); 
		parent.appendChild(info);
		addAttributes(flowdef, info, el);
		return info;
	}
	
	private NodeInfo parseFlow(FlowDefinition flowdef, NodeInfo parent, Element el) 
	throws Exception {
		addAttributes(flowdef, flowdef, el);
		return flowdef;
	}
	
	private void parseZScript(FlowDefinition flowdef, NodeInfo parent, Element el) {
		final boolean
			deferred = "true".equals(el.getAttributeValue("deferred"));

		String zslang = el.getAttributeValue("language");
		if (zslang == null) {
			zslang = flowdef.getZScriptLanguage();
			//we have to resolve it in parser since a page might be
			//created by use of createComponents
		} else {
			noEmpty("language", zslang, el);
			noEL("language", zslang, el);
		}

		final String zsrc = el.getAttributeValue("src");
		if (!isEmpty(zsrc)) { //ignore empty (not error)
			final ZScript zs;
			if (zsrc.indexOf("${") >= 0) {
				zs = new ZScript(flowdef.getEvaluatorRef(),
					zslang, zsrc, null, getLocator());
			} else {
				final URL url = getLocator().getResource(zsrc);
				if (url == null) throw new UiException("File not found: "+zsrc+", at "+el.getLocator());
					//don't throw FileNotFoundException since Tomcat 'eats' it
				zs = new ZScript(flowdef.getEvaluatorRef(), zslang, url, null);
			}

			if (deferred) zs.setDeferred(true);
			parent.appendChild(zs);
		}

		String script = el.getText(false);
		if (!isEmpty(script.trim())) {
			final org.zkoss.xml.Locator l = el.getLocator();
			int lno = l != null ? l.getLineNumber(): 0;
			if (lno > 1) {
				final StringBuffer sb = new StringBuffer(lno);
				while (--lno > 0)
					sb.append('\n');
				script = sb.toString() + script;
			}
			final ZScript zs =
				new ZScript(flowdef.getEvaluatorRef(), zslang, script, null);
			if (deferred) zs.setDeferred(true);
			parent.appendChild(zs);
		}
	}
	private void parseAttribute(FlowDefinition flowdef, NodeInfo parent, Element el)
	throws Exception {
		final String attnm = IDOMs.getRequiredAttributeValue(el, "name");
		noEmpty("name", attnm, el);
		final String trim = el.getAttributeValue("trim");
		noEL("trim", trim, el);
		final String attval = el.getText(trim != null && "true".equals(trim));
		addAttribute(flowdef, parent, attnm, attval, el.getLocator());
	}

	private static void addAttributes(FlowDefinition flowdef, NodeInfo info, Element el) 
	throws Exception {
		org.zkoss.xml.Locator locator = el.getLocator();
		for (Iterator it = el.getAttributeItems().iterator(); it.hasNext();) {
			final Attribute attr = (Attribute)it.next();
			final String attnm = attr.getLocalName();
			final String attval = attr.getValue();
			addAttribute(flowdef, info, attnm, attval, locator);
		}
	}
	
	/** Parse an attribute and adds it to the definition.
	 */
	private static void addAttribute(FlowDefinition flowdef, NodeInfo info, 
	String name, String value, org.zkoss.xml.Locator xl)
	throws Exception {
		if (Events.isValid(name)) { //a kind of EventListener
			final int lno = xl != null ? xl.getLineNumber(): 0;
			final ZScript zscript = ZScript.parseContent(value, lno);
			if (zscript.getLanguage() == null)
				zscript.setLanguage(flowdef.getZScriptLanguage());
			info.addEventHandler(name, zscript);
		} else if (info instanceof ActionStateInfo && "test".equals(name)) {
			final int lno = xl != null ? xl.getLineNumber(): 0;
			final ZScript zscript = ZScript.parseContent(value, lno);
			String lng = zscript.getLanguage();
			if (lng == null) {
				lng = flowdef.getZScriptLanguage();
			}
			final String cnt = zscript.getRawContent(); //without ":"
			final String assign = formatAssign(lng, cnt);
			final ZScript zscript2 = ZScript.parseContent(assign, lno);
			((ActionStateInfo)info).setTest(zscript2);
		} else {
			info.addAttribute(name, value);
		}
	}
	
	private static final Map FORMATERS = new HashMap();
	//TODO, ACTION_STATE_TEST: add for other script language.  
	static {
		FORMATERS.put("JAVA", new MessageFormat("stateScope.put(\"ACTION_STATE_TEST\", {0})"));
	}
	
	private static String formatAssign(String lng, String value) {
		if (lng == null) {
			throw new UiException("Unknown script language: "+ lng);
		}
		final MessageFormat formater = (MessageFormat)FORMATERS.get(lng.toUpperCase());
		if (formater == null) {
			throw new UiException("Unknown script language: "+ lng);
		}
		//clear the leading '\n'
		int j = 0;
		for (; value.charAt(j) == '\n'; ++j) {
			//do nothing;
		}
		final String expr = value.substring(j);
		return (j > 0 ? value.substring(0, j) : "") + formater.format(new Object[] {expr}, new StringBuffer(), null).toString();
	}

	private Locator getLocator() {
		return _locator;
	}
	
	private String toAbsoluteURI(String uri, boolean allowEL) {
		if (uri != null && uri.length() > 0) {
			final char cc = uri.charAt(0);
			if (cc != '/' && cc != '~' && (!allowEL || uri.indexOf("${") < 0)
			&& !Servlets.isUniversalURL(uri)) {
				final String dir = getLocator().getDirectory();
				if (dir != null && dir.length() > 0)
					return dir.charAt(dir.length() - 1) == '/' ?
							dir + uri: dir + '/' + uri;
			}
		}
		return uri;
	}

	/** Parse the evaluator directive. */
	private static void parseEvaluatorDirective(FlowDefinition flowdef,
	ProcessingInstruction pi, Map params) throws Exception {
		final String clsnm = (String)params.remove("class");
		if (clsnm != null && clsnm.length() > 0) {
			noELnorEmpty("class", clsnm, pi);
			flowdef.setExpressionFactoryClass(locateClass(clsnm));
		} else { //name has the lower priorty
			final String nm = (String)params.remove("name");
			if (nm != null)
				flowdef.setExpressionFactoryClass(Evaluators.getEvaluatorClass(nm));
		}

		final String imports = (String)params.remove("import");
		if (imports != null && imports.length() > 0) {
			Collection ims = CollectionsX.parse(null, imports, ',', false); //No EL
			for (Iterator it = ims.iterator(); it.hasNext();) {
				final String im = (String)it.next();

				final int k = im.indexOf('=');
				String nm = k > 0 ? im.substring(0, k).trim(): null;
				String cn = (k >= 0 ? im.substring(k + 1): im).trim();

				if (cn.length() != 0) {
					final Class cs = locateClass(cn);
					if (nm == null || nm.length() == 0) {
						final int j = cn.lastIndexOf('.');
						nm = j >= 0 ? cn.substring(j + 1): cn;
					}
					flowdef.addExpressionImport(nm, cs);
				}
			}
		}
	}
	/** Parse the XEL method. */
	private static void parseXelMethod(FlowDefinition flowdef,
	ProcessingInstruction pi, Map params) throws Exception {
		final String prefix = (String)params.remove("prefix");
		noELnorEmpty("prefix", prefix, pi);
		final String nm = (String)params.remove("name");
		noELnorEmpty("name", nm, pi);
		final String clsnm = (String)params.remove("class");
		noELnorEmpty("class", clsnm, pi);
		final String sig = (String)params.remove("signature");
		noELnorEmpty("signature", sig, pi);

		final Method mtd;
		try {
			final Class cls = Classes.forNameByThread(clsnm);
			mtd = Classes.getMethodBySignature(cls, sig, null);
		} catch (ClassNotFoundException ex) {
			throw new UiException("Class not found: "+clsnm+", "+pi.getLocator());
		} catch (Exception ex) {
			throw new UiException("Method not found: "+sig+" in "+clsnm+", "+pi.getLocator());
		}
		if ((mtd.getModifiers() & Modifier.STATIC) == 0)
			throw new UiException("Not a static method: "+mtd);

		flowdef.addXelMethod(prefix, nm, new MethodFunction(mtd));
	}
	
	/** Checks whether the value is an empty string.
	 * Note: Like {@link #noEL}, it is OK to be null!!
	 * To check neither null nor empty, use IDOMs.getRequiredXxx.
	 */
	private static void noEmpty(String nm, String val, Item item)
	throws UiException {
		if (val != null && val.length() == 0)
			throw new UiException(nm+" cannot be empty, "+item.getLocator());
	}
	
	private static void noELnorEmpty(String nm, String val, Item item)
	throws UiException {
		if (isEmpty(val))
			throw new UiException(nm + " cannot be empty, "+item.getLocator());
		noEL(nm, val, item);
	}
	
	private static void noEL(String nm, String val, Item item)
	throws UiException {
		if (val != null && val.indexOf("${") >= 0)
			throw new UiException(nm+" does not support EL expressions, "+item.getLocator());
	}

	/** Whether a string is null or empty. */
	private static boolean isEmpty(String s) {
		return s == null || s.length() == 0;
	}
	
	/** Parses a list of string separated by comma, into a String array.
	 */
	private static String[] parseToArray(String s) {
		if (s == null)
			return null;
		Collection ims = CollectionsX.parse(null, s, ',', false); //NO EL
		return (String[])ims.toArray(new String[ims.size()]);
	}

	private static Class locateClass(String clsnm) throws Exception {
		try {
			return Classes.forNameByThread(clsnm);
		} catch (ClassNotFoundException ex) {
			throw new ClassNotFoundException("Class not found: "+clsnm, ex);
		}
	}
}
