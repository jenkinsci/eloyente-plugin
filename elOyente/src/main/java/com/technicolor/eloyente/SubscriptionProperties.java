/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.technicolor.eloyente;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 *
 * @author fernandezdiazi
 */
public class SubscriptionProperties {

//    public String node;
    //public Expressions[] expressions;
    public final String filter;
    
    @DataBoundConstructor
    public SubscriptionProperties(String filter) {
//        this.node = node;
        this.filter = filter;
    }

//    public void setExpressions(Expressions[] expressions) {
//        this.expressions = expressions;
//    }
//    public String getNode() {
//        return node;
//    }
//
//    public void setNode(String node){
//        this.node = node;
//    }
    public String getFilter() {
        return filter;
    }
//    public Expressions[] getExpressions() {
//        return expressions;
//    }
}