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
public class SuscriptionProperties {

    /**
     * The text string which should be searched in the build log.
     */
    // public LogProperties[] logTexts;
    public String nodeName;

    @DataBoundConstructor
    public SuscriptionProperties(String nodeName) {
        this.nodeName=nodeName;
    }

    public String getnodeName() {
        return nodeName;
    }
    public void setnodeName(String nodeName) {
         this.nodeName=nodeName;
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
