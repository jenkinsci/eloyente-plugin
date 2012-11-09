package com.technicolor.eloyente;

import hudson.triggers.Trigger;
import org.jivesoftware.smackx.pubsub.ItemPublishEvent;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.SimplePayload;
import org.jivesoftware.smackx.pubsub.listener.ItemEventListener;

class ItemEventCoordinator implements ItemEventListener<PayloadItem<SimplePayload>> {

    private final String nodename;
    private final Trigger trigger;

    ItemEventCoordinator(String nodename,Trigger trigger) {
        this.nodename = nodename;
        this.trigger = trigger;
    }

    @Override
    public void handlePublishedItems(ItemPublishEvent<PayloadItem<SimplePayload>> items) {
        print(nodename, items);
        trigger.run();
    }

    private synchronized void print(String nodename, ItemPublishEvent<PayloadItem<SimplePayload>> items) {
        System.out.println("-----------------------------");
        System.out.println(nodename + ": Item count: " + items.getItems().size());
        for (PayloadItem<SimplePayload> item : items.getItems()) {
            System.out.println(nodename + ": XML: " + item.toXML());
            System.out.println("Item:" + item);
        }
        System.out.println("-----------------------------");
    }
}