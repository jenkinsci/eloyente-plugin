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
import javax.xml.xpath.XPathExpressionException;
import java.util.List;

public class SubscriptionPropertiesTest {

	@Test
	public void constructorAllNull() throws XPathExpressionException {
		SubscriptionProperties p = new SubscriptionProperties(null, null, null);
		assertEquals(null, p.getNode());
		assertEquals("", p.getFilter());
		Object filter = p.getFilterXPath();
		assertEquals("XPathExpressionHandler", filter.getClass().getSimpleName());
		Object vars = p.getVariables();
		assertEquals("ArrayList", vars.getClass().getSimpleName());
	}

	@Test
	public void constructorAllOK() throws XPathExpressionException {
		Variable[] vars = new Variable[2];
		vars[0] = new Variable("FOO", "//foo");
		vars[1] = new Variable("BAR", "//bar");
		SubscriptionProperties p = new SubscriptionProperties("//foo/bar", "node", vars);
		assertEquals("node", p.getNode());
		assertEquals("//foo/bar", p.getFilter());
		assertEquals(2, p.getVariables().size());
		assertEquals("FOO", ((Variable)p.getVariables().get(0)).getEnvName());
		assertEquals("//foo", ((Variable)p.getVariables().get(0)).getEnvExpr());
		assertEquals("BAR", ((Variable)p.getVariables().get(1)).getEnvName());
		assertEquals("//bar", ((Variable)p.getVariables().get(1)).getEnvExpr());
	}
}

// vim: set tabstop=4 softtabstop=4 shiftwidth=4 noexpandtab :
