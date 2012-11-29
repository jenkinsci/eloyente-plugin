/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.technicolor.eloyente;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 *
 * @author fernandezdiazi
 */
public class SubscriptionProperties {

    public String node;
    public Expressions[] envVars;
    public final String filter;

    @DataBoundConstructor
    public SubscriptionProperties(String filter, String node, Expressions[] v) {
        this.node = node;
        this.filter = filter;
        this.envVars = v;
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
        return filter;
    }

    public List<Expressions> getEnvVars() {
        if (envVars == null) {
            return new ArrayList<Expressions>();
        } else {
            return Arrays.asList(envVars);
        }
    }
}