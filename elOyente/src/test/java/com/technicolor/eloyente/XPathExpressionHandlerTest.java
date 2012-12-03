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

import static org.junit.Assert.*;
import org.junit.Test;

import javax.xml.parsers.*;
import javax.xml.xpath.*;

public class XPathExpressionHandlerTest {

	private static final String XML0 = null;
	private static final String XML1 = "";
	private static final String XML2 = "<foo><bar>baz</bar></foo>";
	private static final String XML3 = "<foo id=\"1\"><bar>baz</bar></foo>";
	private static final String XML4 = "<foo id=\"1\"><bar id=\"1\">baz</bar><bar id=\"2\"/></foo>";
	private static final String XML5 = "<foo></bar>baz<bar></foo>";

	@Test
	public void testConstructorOK() throws Exception {
		String exp;

		XPathExpressionHandler eh0 = new XPathExpressionHandler();
		assertNull(eh0.getExpression());

		XPathExpressionHandler eh1 = new XPathExpressionHandler(null);
		assertNull(eh1.getExpression());

		exp = "/node/foo[@bar='baz']/name/text()";
		XPathExpressionHandler eh2 = new XPathExpressionHandler(exp);
		assertEquals(exp, eh2.getExpression());

		exp = "//foo | //bar";
		XPathExpressionHandler eh3 = new XPathExpressionHandler(exp);
		assertEquals(exp, eh3.getExpression());
		eh3.setExpression(exp);
		assertEquals(exp, eh3.getExpression());
	}

	@Test(expected=XPathExpressionException.class)
	public void testConstructorNOK() throws Exception {
		new XPathExpressionHandler("");
	}

	@Test(expected=XPathExpressionException.class)
	public void testEmpty() throws Exception {
		XPathExpressionHandler eh = new XPathExpressionHandler("");
		assertEquals("", eh.evaluate(XML0));
		assertEquals("", eh.evaluate(XML1));
		assertEquals("", eh.evaluate(XML2));
		assertEquals("", eh.evaluate(XML3));
		assertEquals("", eh.evaluate(XML4));
		assertEquals("", eh.evaluate(XML5));
		assertFalse(eh.test(XML0));
		assertFalse(eh.test(XML1));
		assertFalse(eh.test(XML2));
		assertFalse(eh.test(XML3));
		assertFalse(eh.test(XML4));
		assertFalse(eh.test(XML5));
	}

	@Test
	public void testDocument() throws Exception {
		XPathExpressionHandler eh = new XPathExpressionHandler("/");
		assertEquals(XML0, eh.evaluate(XML0));
		assertEquals(XML1, eh.evaluate(XML1));
		assertEquals(XML2, eh.evaluate(XML2));
		assertEquals(XML3, eh.evaluate(XML3));
		assertEquals(XML4, eh.evaluate(XML4));
		assertEquals(XML5, eh.evaluate(XML5));
		assertFalse(eh.test(XML0));
		assertEquals(XML1.length() > 0, eh.test(XML1));
		assertEquals(XML2.length() > 0, eh.test(XML2));
		assertEquals(XML3.length() > 0, eh.test(XML3));
		assertEquals(XML4.length() > 0, eh.test(XML4));
		assertEquals(XML5.length() > 0, eh.test(XML5));
	}

	@Test
	public void testRoot() throws Exception {
		XPathExpressionHandler eh = new XPathExpressionHandler("/foo");
		assertEquals(XML0, eh.evaluate(XML0));
		assertEquals(XML1, eh.evaluate(XML1));
		assertEquals(XML2, eh.evaluate(XML2));
		assertEquals(XML3, eh.evaluate(XML3));
		assertEquals(XML4, eh.evaluate(XML4));
		assertEquals("", eh.evaluate(XML5));
		assertFalse(eh.test(XML0));
		assertEquals(XML1.length() > 0, eh.test(XML1));
		assertEquals(XML2.length() > 0, eh.test(XML2));
		assertEquals(XML3.length() > 0, eh.test(XML3));
		assertEquals(XML4.length() > 0, eh.test(XML4));
		assertFalse(eh.test(XML5));
	}

	@Test
	public void testNonExistingRoot() throws Exception {
		XPathExpressionHandler eh = new XPathExpressionHandler("/non-existing-root");
		assertEquals(XML0, eh.evaluate(XML0));
		assertEquals("", eh.evaluate(XML1));
		assertEquals("", eh.evaluate(XML2));
		assertEquals("", eh.evaluate(XML3));
		assertEquals("", eh.evaluate(XML4));
		assertEquals("", eh.evaluate(XML5));
		assertFalse(eh.test(XML0));
		assertFalse(eh.test(XML1));
		assertFalse(eh.test(XML2));
		assertFalse(eh.test(XML3));
		assertFalse(eh.test(XML4));
		assertFalse(eh.test(XML5));
	}

	@Test
	public void testMultipleMatches() throws Exception {
		String xml = "<bar>bar1</bar><bar>bar2</bar>";
		XPathExpressionHandler eh = new XPathExpressionHandler("//bar");
		assertEquals(xml, eh.evaluate("<foo>" + xml + "</foo>"));
	}

	@Test
	public void testWithNamespace() throws Exception {
		String expected = "<foo><bar>bar1</bar><bar>bar2</bar></foo>";
		String full = "<message xmlns=\"mynamespace\">" + expected + "</message>";
		XPathExpressionHandler eh = new XPathExpressionHandler("//foo");
		assertEquals(expected, eh.evaluate(full));
	}

}

// vim: set tabstop=4 softtabstop=4 shiftwidth=4 noexpandtab :
