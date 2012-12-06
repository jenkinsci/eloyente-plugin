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
        String xml = items.getItems().iterator().next().toXML();
        List<SubscriptionProperties> subscriptionList = trigger.getNodeSubscriptions(nodename);
        Iterator it = subscriptionList.iterator();
        System.out.println("IMPRIME handlePublishedItems() of object " + this + " called for items " + items.getItems());
        System.out.println("IMPRIME size: " + items.getItems().size());
        System.out.println("IMPRIME xml: " + xml);

        while (it.hasNext()) {
            SubscriptionProperties subs = (SubscriptionProperties) it.next();
            System.out.println("IMPRIME nodo: " + subs.getNode());
            System.out.println("IMPRIME filtro: " + subs.getFilter());

            try {
                if (subs != null) {
                    XPathExpressionHandler filter = subs.getFilterXPath();
                    //System.out.println("IMPRIME evaluate: " + filter.evaluate(xml));

                    if (filter.test(xml)) {
                        System.out.println("IMPRIME run");
                        trigger.run();
                    }
                }

            } catch (XPathExpressionException ex) {
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