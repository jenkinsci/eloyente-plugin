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

    /**
     * The text string which should be searched in the build log.
     */
    // public LogProperties[] logTexts;
    public String node;
    public String filter;

    @DataBoundConstructor
    public SubscriptionProperties(String node, String filter) {
        this.node=node;
        this.filter=filter;
    }

    public String getNode() {
        return node;
    }
    public void setNode(String node) {
         this.node=node;
    }
     public String getFilter() {
        return filter;
    }
    public void setFilter(String filter) {
         this.filter=filter;
    }
    /*
     * public TaskProperties(LogProperties[] logTexts, String script) {
     * this.logTexts = logTexts; this.script = script; }
     */

    /*
     * public TaskProperties(Collection<LogProperties> logTexts,String script) {
     * this((LogProperties[])logTexts.toArray(new
     * LogProperties[logTexts.size()]),script); }
     */
    // TODO
	/*
     * public String getLogText() { return null; }
     */
}
