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

import hudson.EnvVars;
import hudson.triggers.Trigger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.xpath.XPathExpressionException;
import org.jivesoftware.smackx.pubsub.ItemPublishEvent;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.SimplePayload;
import org.jivesoftware.smackx.pubsub.listener.ItemEventListener;

/**
 * @author Juan Luis Pardo Gonz&aacute;lez
 * @author Isabel Fern&aacute;ndez D&iacute;az
 */
public class ItemEventCoordinator implements ItemEventListener<PayloadItem<SimplePayload>> {

    /**
     * Name of the node that will be listened .
     */
    private final String nodename;
    /**
     * The list of triggers that this ItemEventCoordinator will have to trigger
     * .
     *
     * This list of triggers is set depending on the subscriptions of the
     * different jobs. When an event is received, this ItemEventCoordinator will
     * schedule builds for the jobs related to this triggers.
     */
    protected final ArrayList<ElOyente> Triggers;

    /**
     * Constructor for the ItemEventCoordinator.
     *
     * It creates an ItemEventCoordinator for a concrete node.
     * 
     * @param s Name of the node to be listened to.
     */
    ItemEventCoordinator(String nodename) {
        this.nodename = nodename;
        this.Triggers = new ArrayList();
    }
    
    /**
     * Adds a trigger to the list.
     * 
     * It adds a trigger to the list every time a job requires it, it makes sure
     * the triggers are not repeated.
     * 
     * @param trigger Trigger to be added to the list of triggers.
     */
    public void addTrigger(ElOyente trigger) {
        boolean add = false;
        for (ElOyente triggerAdded : this.Triggers) {
            if (trigger.equals(triggerAdded)) {
                add = true;
                break;
            }
        }
        if (add == false) {
            this.Triggers.add(trigger);
        }
    }

    /**
     * Applying the filter decides whether to trigger the job or not and passes
     * the environment variables if they exist.
     * 
     * It is called for each XMPP event received on a node. It will trigger all the triggers
     * of the field Triggers, for this, it will get all the subscriptions of each particular job
     * of each particular trigger in the field Triggers and it will filter based on the filter specified. 
     * If it passes it will use the method runWithEnvironment(EnvVars) to schedule a build.
     * 
     * @param items The XMPP event received.
     * 
     */   
    public void handlePublishedItems(ItemPublishEvent<PayloadItem<SimplePayload>> items) {
        print(items);
        for (ElOyente trigger : this.Triggers) {         
            System.out.println(trigger.listeners.size());
            
            Iterator it2 = trigger.listeners.entrySet().iterator();
            while (it2.hasNext()) {
                Map.Entry e = (Map.Entry) it2.next();
                System.out.println("LISTENER: " + e.getKey() + " " + e.getValue());
            }
            // TODO: why only consider the first entry of items, and why use an iterator in that case?
            String xml = items.getItems().iterator().next().toXML();
            List<SubscriptionProperties> subscriptionList = trigger.getNodeSubscriptions(nodename);
            Iterator it = subscriptionList.iterator();
            
            for (SubscriptionProperties subs : subscriptionList) {
                try {
                    XPathExpressionHandler filter = subs.getFilterXPath();
                    if (filter.test(xml)) {
                        EnvVars vars = new EnvVars();
                        for (Variable v : subs.getVariables()) {
                            vars.put(v.getEnvName(), v.resolve(xml));
                        }
                        try {
                            trigger.runWithEnvironment(vars);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(ItemEventCoordinator.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                } catch (XPathExpressionException ex) {
                    System.out.println("Exception: " + ex);
                    Logger.getLogger(ItemEventCoordinator.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    /**
     * Prints the message of the XMPP event received.
     */
    private synchronized void print(ItemPublishEvent<PayloadItem<SimplePayload>> items) {
        System.out.println("-----------------------------");
        System.out.println("El item event coordinator es: " + this);
        System.out.println(nodename + ": Item count: " + items.getItems().size());
        for (PayloadItem<SimplePayload> item : items.getItems()) {
            System.out.println(nodename + ": XML: " + item.toXML());
            System.out.println("Mas cosas:" + item);
        }
        System.out.println("-----------------------------");
    }
}
