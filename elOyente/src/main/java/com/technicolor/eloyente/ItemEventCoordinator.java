package com.technicolor.eloyente;

import hudson.triggers.Trigger;
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
    private final ElOyente trigger;

    ItemEventCoordinator(String nodename, Trigger trigger) {
        this.nodename = nodename;
        this.trigger = (ElOyente) trigger;
    }

    @Override
    public void handlePublishedItems(ItemPublishEvent<PayloadItem<SimplePayload>> items) {
        print(nodename, items);
	// TODO: why only consider the first entry of items, and why use an iterator in that case?
        String xml = items.getItems().iterator().next().toXML();
        List<SubscriptionProperties> subscriptionList = trigger.getNodeSubscriptions(nodename);
        //System.out.println("IMPRIME size: " + items.getItems().size());
        System.out.println("IMPRIME xml: " + xml);

        for (SubscriptionProperties subs : subscriptionList) {
            System.out.println("IMPRIME nodo: " + subs.getNode());
            System.out.println("IMPRIME filtro: " + subs.getFilter() + " / " + subs.getFilterXPath());

            try {
                XPathExpressionHandler filter = subs.getFilterXPath();
                //System.out.println("IMPRIME evaluate: " + filter.evaluate(xml));

                if (filter.test(xml)) {
                    System.out.println("IMPRIME run init");
                    for (Variable v : subs.getVariables()) {
                        System.out.println("$" + v.getEnvName());
                        System.out.println("expression=" + v.getEnvExpr());
                        System.out.println("resolve=" + v.resolve(xml));
                    //    System.out.println("$" + v.getEnvName() + " = '" + v.resolve(xml) + "' [" + v.getEnvExpr() + "] " + xml);
                    }
                    System.out.println("IMPRIME run");
                    trigger.run();
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
