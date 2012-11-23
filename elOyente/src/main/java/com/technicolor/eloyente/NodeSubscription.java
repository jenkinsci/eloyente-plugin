/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.technicolor.eloyente;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 *
 * @author pardogonzalezj
 */
public class NodeSubscription {

    public String nodeName;


    @DataBoundConstructor
    public NodeSubscription(String nodeName) {
        this.nodeName = nodeName;
    }

    private void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getNodeName() {
        return nodeName;
    }
}