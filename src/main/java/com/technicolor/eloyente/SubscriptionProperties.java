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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.xml.xpath.XPathExpressionException;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author Juan Luis Pardo Gonz&aacute;lez
 * @author Isabel Fern&aacute;ndez D&iacute;az
 */
public class SubscriptionProperties {

    /**
     * The node to which a job will subscribe.
     */
    protected String node;
    /**
     * The environment variables that a job will have.
     */
    protected Variable[] variables;
    private XPathExpressionHandler filter;

    /**
     * Constructor for the properties of a subscription.
     *
     * @param filter Filter to be applied to the XMPP messages received
     * @param node Node to which the subscription is done.
     * @param v Environment variables for that subscription.
     * @throws XPathExpressionException
     */
    @DataBoundConstructor
    public SubscriptionProperties(String filter, String node, Variable[] v) throws XPathExpressionException {
        this.node = node;
        this.filter = new XPathExpressionHandler(filter);
        this.variables = v;
    }

    /**
     * Retrieves the node the user input.
     */
    public String getNode() {
        return node;
    }

    /**
     * Retrieves the filter the user input.
     */
    public String getFilter() {
        return filter.getExpression();
    }

    public XPathExpressionHandler getFilterXPath() {
        return filter;
    }

    /**
     * Retrieves the environment variables the user input
     */
    public List<Variable> getVariables() {
        if (variables == null) {
            return new ArrayList<Variable>();
        } else {
            return Arrays.asList(variables);
        }
    }
}
