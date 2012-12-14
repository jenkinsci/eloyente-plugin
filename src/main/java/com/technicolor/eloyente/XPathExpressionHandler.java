/*
   Copyright 2012 Technicolor

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package com.technicolor.eloyente;

import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.*;
import org.w3c.dom.ls.*;
import org.xml.sax.*;
import javax.xml.xpath.*;
import javax.xml.parsers.*;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.nio.charset.Charset;

/**
 * This is a convenience class for working with XPath expressions.
 * <p>
 * It is used in ElOyente for filters and for environment variables,
 * based on XMPP events.  XMPP events are XML messages, and a filters
 * uses the test() function of this class to check whether the XMPP
 * event should trigger a Jenkins job.  The evaluate() function at the
 * other hand will be used to extract data from the XMPP event, and to
 * store it in environment variables, such that the Jenkins job which
 * is triggered by the event can use the data.
 *
 * @author Frank Vanderhallen
 */

public class XPathExpressionHandler {

	private String expression;
	private transient XPathExpression expressionCompiled;

	private transient DOMImplementationRegistry domImplementationRegistry;
	private transient DocumentBuilderFactory docBuilderFactory;
	private transient DocumentBuilder docBuilder;
	private transient Document xmldoc;
	private transient DOMImplementationLS domImplementation;

	private static final String EMPTY_STR = "";

	/**
	 * Default constructor
	 * <p>
	 * Creates an XPathExpression handler with an empty expression.
	 */
	public XPathExpressionHandler() throws XPathExpressionException {
		this(EMPTY_STR);
	}

	/**
	 * Constructor which configures an expression
	 * <p>
	 * @param expression An expression in XPath notation
	 */
	public XPathExpressionHandler(String expression) throws XPathExpressionException {
		init(expression);
	}

	private void init(String expression) throws XPathExpressionException {
		try {
			domImplementationRegistry = DOMImplementationRegistry.newInstance();
			domImplementation = (DOMImplementationLS)domImplementationRegistry.getDOMImplementation("LS");
			docBuilderFactory = DocumentBuilderFactory.newInstance();
			docBuilderFactory.setNamespaceAware(false);
			docBuilder = docBuilderFactory.newDocumentBuilder();
			xmldoc = docBuilder.newDocument();
			setExpression(expression);
		} catch (XPathExpressionException e) {
			throw e;
		} catch (Exception e) {
			/**
			* If an exception is thrown, there is a problem with the XML parsing
			* environment on the machine on which the code is executed.
			* Inform the user about the problem, but don't throw the exception
			* to the user.
			*/
			e.printStackTrace();
		}
	}

	/**
	 * This function initializes the transient members when the object is deserialized.
	 */
	private Object readResolve() throws XPathExpressionException {
		String expr = this.expression;
		// this is to avoid the optimalization when deserializing
		this.expression = null;
		init(expr);
		return this;
	}

	/**
	 * Function to change the expression
	 * <p>
	 * @param expression An expression in XPath notation.
	 */
	public void setExpression(String expression) throws XPathExpressionException {
		String newExpr = (null == expression ? EMPTY_STR : expression);
		if (!newExpr.equals(this.expression)) {
			if (!EMPTY_STR.equals(newExpr)) {
				XPath xpath = XPathFactory.newInstance().newXPath();
				this.expressionCompiled = xpath.compile(newExpr);
			} else {
				this.expressionCompiled = null;
			}
			this.expression = newExpr;
		}
	}

	/**
	 * Function to retrieve the expression.
	 * <p>
	 * @return The expression that the object holds
	 */
	public String getExpression() {
		return expression;
	}

	/**
	 * Evaluate the expression.
	 * <p>
	 * This function evaluates the expression against the provided
	 * XML document and returns the result as a string.
	 *
	 * @param xml The XML document to evaluate
	 * @return The result of evaluating the expression
	 */
	public String evaluate(String xml) throws XPathExpressionException {
		if (null == expressionCompiled) return (null != xml ? xml : EMPTY_STR);
		Document doc = getDOM(xml);
		if (null == doc) return EMPTY_STR;
		NodeList n = (NodeList)expressionCompiled.evaluate(doc, XPathConstants.NODESET);
		return getXML(n);
	}

	/**
	 * Test the expression
	 * <p>
	 * This function evaluates the expression against the provided
	 * XML document and returns true if the expression result is not empty.
	 *
	 * @param xml The XML document to evaluate
	 * @return True if the result is not empty
	 */
	public boolean test(String xml) throws XPathExpressionException {
		if (null == expressionCompiled) return (null != xml && !xml.isEmpty());
		Document doc = getDOM(xml);
		if (null == doc) return false;
		// a node-set is true if and only if it is not empty
		// (source: http://www.w3.org/TR/xpath/#function-boolean)
		NodeList n = (NodeList)expressionCompiled.evaluate(doc, XPathConstants.NODESET);
		return (n.getLength() > 0);
	}

	private Document getDOM(String xml) {
		try {
			InputStream is = new ByteArrayInputStream(xml.getBytes(Charset.forName("UTF-16")));
			return docBuilder.parse(is);
		} catch (Exception e) {
			/**
			* Exceptions are due to malformed XML messages, which can be
			* received from anywhere.  Since we cannot throw the exception to the
			* sender of the XML message, silently discard it.
			*/
			return null;
		}
	}

	private String getXML(NodeList list) {
		try {
			LSSerializer ser = domImplementation.createLSSerializer();
			DOMConfiguration cfg = ser.getDomConfig();
			cfg.setParameter("xml-declaration", false);
			StringBuilder docstr = new StringBuilder();

			for (int i=0; i<list.getLength(); i++) {
				Node n = list.item(i);
				docstr.append(ser.writeToString(n));
			}

			return docstr.toString();
		} catch (Exception e) {
			e.printStackTrace();
			return EMPTY_STR;
		}
	}

}

// vim: set tabstop=4 softtabstop=4 shiftwidth=4 noexpandtab :
