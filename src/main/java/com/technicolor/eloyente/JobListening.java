/*
 * Copyright 2012 Technicolor.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.technicolor.eloyente;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.Project;
import hudson.model.listeners.ItemListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.pubsub.PubSubManager;
import org.jivesoftware.smackx.pubsub.Subscription;

@Extension
public final class JobListening extends ItemListener {

    private final static Integer USER_ID = 0;
    private final static Integer RESOURCE_ID = 1;

    @Override
    public void onRenamed(Item item, String oldName, String newName) {

        List<Subscription> subscriptionList;
        ElOyente instance = (ElOyente) ((AbstractProject) item).getTrigger(ElOyente.class);

        if (instance != null) {
            try {
                Connection con = instance.connections.get(oldName);             //Saco la conexion
                PubSubManager mgr = new PubSubManager(con);

                if (instance.subscriptions != null) {
                    if (instance.subscriptions.length != 0) {
                        for (SubscriptionProperties sp : instance.subscriptions) {
                            String nodeName = sp.getNode();
                            subscriptionList = mgr.getSubscriptions();                      //Saco la lista de suscripciones de la base de datos
                            for (Subscription s : subscriptionList) {

                                Map<Integer, String> jid = instance.parseJID(s);
                                if (null == jid || jid.size() < 2) {
                                    continue;
                                }
                                if (jid.get(RESOURCE_ID).equals(oldName) && s.getNode().equals(nodeName) && jid.get(USER_ID).equals(instance.getDescriptor().getUser())) {
                                    LeafNode n = (LeafNode) mgr.getNode(sp.node);
                                    n.unsubscribe(con.getUser());
                                    ItemEventCoordinator listener = instance.listeners.get(sp.node) ;
                                    instance.listeners.remove(sp.node);
                                    listener = null;
                                }
                            }
                        }
                        con.disconnect();
                        instance.connections.remove(oldName);

                        instance.start((Project) item, true);
                    }
                }
            } catch (XMPPException ex) {
                Logger.getLogger(JobListening.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public void onDeleted(Item item) {
        System.out.println("Job " + item.getName() + " borrado\n");

        ElOyente instance = (ElOyente) ((AbstractProject) item).getTrigger(ElOyente.class);
        if (instance != null) {
            Connection con = instance.connections.get(item.getName());
            PubSubManager mgr = new PubSubManager(con);

            for (SubscriptionProperties s : instance.subscriptions) {
                try {
                    LeafNode n = (LeafNode) mgr.getNode(s.node);
                    n.unsubscribe(con.getUser());
                    
                } catch (XMPPException ex) {
                    System.err.println("The node didn't exist yet");
                }
            }
            con.disconnect();
            instance.connections.remove(item.getName());
        }
    }

    @Override
    public void onLoaded() {
/////////////////////////////////////////////////////////
////////////////////////////////////////////////////////
// WARNING DO NOT TOUCH 
/////////////////////////////////////////////////////////
////////////////////////////////////////////////////////
//        System.out.println("Jobs iniciados\n");
//
//        String server;
//        String user;
//        String password;
//
//        HandleXMPP handler = new HandleXMPP();
//
//        for (Item job : Jenkins.getInstance().getAllItems()) {
//            ElOyente instance = (ElOyente) ((AbstractProject) job).getTrigger(ElOyente.class);
//            if (instance != null) {
//                server = instance.getDescriptor().getServer();
//                user = instance.getDescriptor().getUser();
//                password = instance.getDescriptor().getPassword();
//                if (!handler.checkAnyParameterEmpty(server, user, password)) {
//                    if (handler.connectionOK(server, user, password)) {
//                        handler.createConnection((Project) job, server, user, password);
//                        instance.subscribeIfNecessary((Project) job);
//                    }
//                }
//            }
//        }
    }
}
