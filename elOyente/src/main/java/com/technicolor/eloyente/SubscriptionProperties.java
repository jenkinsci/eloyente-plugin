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
    protected Expressions[] expressions;
    private XPathExpressionHandler filter;
   

    @DataBoundConstructor
    public SubscriptionProperties(String filter, String node, Expressions[] v) throws XPathExpressionException {
        this.node = node;
        this.filter = new XPathExpressionHandler(filter);
        this.expressions = v;
    }

//    public void setExpressions(Expressions[] expressions) {
//        this.expressions = expressions;
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

    public List<Expressions> getExpressions() {
        if (expressions == null) {
            return new ArrayList<Expressions>();
        } else {
            return Arrays.asList(expressions);
        }
    }
}
