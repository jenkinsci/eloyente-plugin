package com.technicolor.eloyente;

import hudson.triggers.Trigger;
import hudson.EnvVars;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.xpath.XPathExpressionException;
import org.jivesoftware.smackx.pubsub.ItemPublishEvent;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.SimplePayload;
import org.jivesoftware.smackx.pubsub.listener.ItemEventListener;

class ItemEventCoordinator implements ItemEventListener<PayloadItem<SimplePayload>> {

    private final String nodename;
    private final ElOyente eloyente;

    ItemEventCoordinator(String nodename, Trigger trigger) {
        this.nodename = nodename;
        this.eloyente = (ElOyente) trigger;
    }

    @Override
    public void handlePublishedItems(ItemPublishEvent<PayloadItem<SimplePayload>> items) {
        print(nodename, items);
	// TODO: why only consider the first entry of items, and why use an iterator in that case?
        String xml = items.getItems().iterator().next().toXML();
        List<SubscriptionProperties> subscriptionList = eloyente.getNodeSubscriptions(nodename);
        //System.out.println("IMPRIME size: " + items.getItems().size());
        System.out.println("IMPRIME xml: " + xml);

        for (SubscriptionProperties subs : subscriptionList) {
            System.out.println("IMPRIME nodo: " + subs.getNode());
            System.out.println("IMPRIME filtro: " + subs.getFilter() + " / " + subs.getFilterXPath());

            try {
                XPathExpressionHandler filter = subs.getFilterXPath();
                //System.out.println("IMPRIME evaluate: " + filter.evaluate(xml));

                if (filter.test(xml)) {
                    EnvVars vars = new EnvVars();
                    System.out.println("IMPRIME run init");
                    for (Variable v : subs.getVariables()) {
                        vars.put(v.getEnvName(), v.resolve(xml));
                    }
                    System.out.println("IMPRIME run");
                    eloyente.runWithEnvironment(vars);
                } else {
                    System.out.println("filter="+filter+" test() returned false");
		}
            } catch (XPathExpressionException ex) {
		System.out.println("Exception: "+ex);
                Logger.getLogger(ItemEventCoordinator.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private synchronized void print(String nodename, ItemPublishEvent<PayloadItem<SimplePayload>> items) {
        System.out.println("-----------------------------");
        System.out.println(nodename + ": Item count: " + items.getItems().size());
        for (PayloadItem<SimplePayload> item : items.getItems()) {
            System.out.println(nodename + ": XML: " + item.toXML());
            System.out.println("Mas cosas:" + item);
        }
        System.out.println("-----------------------------");
    }
}
