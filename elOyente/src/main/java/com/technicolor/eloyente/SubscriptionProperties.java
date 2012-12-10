/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.technicolor.eloyente;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.xml.xpath.XPathExpressionException;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 *
 * @author fernandezdiazi
 */
public class SubscriptionProperties {

    protected String node;
    protected Variable[] variables;
    private XPathExpressionHandler filter;


    @DataBoundConstructor
    public SubscriptionProperties(String filter, String node, Variable[] v) throws XPathExpressionException {
        this.node = node;
        this.filter = new XPathExpressionHandler(filter);
        this.variables = v;
    }

//    public void setExpressions(Expressions[] variables) {
//        this.variables = variables;
//    }
    public String getNode() {
        return node;
    }
//
//    public void setNode(String node){
//        this.node = node;
//    }

    public String getFilter() {
        return filter.getExpression();
    }

    public XPathExpressionHandler getFilterXPath() {
        return filter;
    }

    public List<Variable> getVariables() {
        if (variables == null) {
            return new ArrayList<Variable>();
        } else {
            return Arrays.asList(variables);
        }
    }
}
