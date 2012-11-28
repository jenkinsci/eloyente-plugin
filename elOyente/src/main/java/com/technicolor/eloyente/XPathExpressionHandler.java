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

import org.w3c.dom.*;
import org.xml.sax.*;
import javax.xml.xpath.*;
import javax.xml.parsers.*;
import java.io.*;


public class XPathExpressionHandler {

	private String expr_str; // the expression string
	private XPathExpression expr_cmp; // the compiled expression

	private static final String EMPTY_STR = "";

	public XPathExpressionHandler(String expression) throws XPathExpressionException {
		this.setExpression(expression);
	}

	public void setExpression(String expression) throws XPathExpressionException {
		if (null != expression && !expression.equals(expr_str)) {
			XPath xpath = XPathFactory.newInstance().newXPath();
			expr_cmp = xpath.compile(expression);
			expr_str = expression;
		}
	}

	public String getExpression() {
		return expr_str;
	}

	public String evaluate(String xml) throws XPathExpressionException {
		if (null == xml) return EMPTY_STR;
		Document doc = getDOM(xml);
		if (null == doc) return EMPTY_STR;
		return (String)expr_cmp.evaluate(doc, XPathConstants.STRING);
	}

	public boolean test(String xml) throws XPathExpressionException {
		if (null == xml) return false;
		Document doc = getDOM(xml);
		if (null == doc) return false;
		// a string is true if and only if its length is non-zero
		// (source: http://www.w3.org/TR/xpath/#function-boolean)
		return ! EMPTY_STR.equals(expr_cmp.evaluate(doc, XPathConstants.STRING));
	}

	private Document getDOM(String xml) {
		if (null == xml) return null;
		try {
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			docBuilderFactory.setNamespaceAware(true);
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			InputStream is = new ByteArrayInputStream(xml.getBytes());
			return docBuilder.parse(is);
		} catch (FactoryConfigurationError fc_ex) {
			fc_ex.printStackTrace();
			return null;
		} catch (Exception ex) {
			return null;
		}
	}

}

// vim: set tabstop=4 softtabstop=4 shiftwidth=4 noexpandtab :
