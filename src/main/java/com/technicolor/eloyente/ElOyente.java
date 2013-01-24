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

import com.google.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.model.Project;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.io.ObjectStreamException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.packet.DiscoverItems;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.pubsub.Node;
import org.jivesoftware.smackx.pubsub.PubSubManager;
import org.jivesoftware.smackx.pubsub.Subscription;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * This plug-in adds job triggering based on XMPP Pub/Sub events.
 *
 * @author Juan Luis Pardo Gonz&aacute;lez
 * @author Isabel Fern&aacute;ndez D&iacute;az
 */
public class ElOyente extends Trigger<Project> {

    /**
     * Array of subscriptions for a job.
     */
    protected SubscriptionProperties[] subscriptions;
    /**
     * Map of active listeners.
     *
     * (Key: Node Name ; Value: Listener).
     *
     * A node has only one listener (it is inside the listener where we control
     * the subscriptions and schedule the builds).
     */
    protected static transient Map<String, ItemEventCoordinator> listeners = new HashMap<String, ItemEventCoordinator>();
    /**
     * The project associated to the instance of the trigger.
     */
    protected transient Project project;

    /**
     * Constructor for the trigger.
     *
     * This constructor needs an Array of SubscriptionProperties related to the
     * job the trigger is attached to.
     *
     * @param s Array of SubscriptionProperties, it will contain the nodes,
     * filters and environment variables related to a job.
     */
    @DataBoundConstructor
    public ElOyente(SubscriptionProperties[] s) {
        this.subscriptions = s;
    }

    /**
     * Used for deserialization of the trigger.
     *
     * @throws ObjectStreamException
     */
    @Override
    public Object readResolve() throws ObjectStreamException {
        super.readResolve();
        listeners = new HashMap<String, ItemEventCoordinator>();
        return this;
    }

    /**
     * Used for getting the subscriptions.
     *
     * This method retrieves the information about the different subscriptions
     * filled by the user in the job configuration.
     *
     */
    public List<SubscriptionProperties> getSubscriptions() {
        if (subscriptions == null) {
            return new ArrayList<SubscriptionProperties>();
        } else {
            return Arrays.asList(subscriptions);
        }
    }

    /**
     *
     * Retrieves the subscription properties for a node.
     *
     * This method returns a List with the different subscriptions for a node
     * specified as parameter.
     *
     * @param node The name of the node.
     *
     * @return subsc The list of subscriptions that a job has in its config.xml.
     */
    public List<SubscriptionProperties> getNodeSubscriptions(String node) {

        if (subscriptions == null) {
            return new ArrayList<SubscriptionProperties>();
        } else {
            int i;
            List<SubscriptionProperties> subsc = new ArrayList<SubscriptionProperties>();
            for (i = 0; i < subscriptions.length; i++) {
                if (subscriptions[i].node.equals(node)) {
                    subsc.add(subscriptions[i]);
                }
            }
            return subsc;
        }
    }

    /**
     * Method used for starting a job.
     *
     * This method is called when: 1) Jenkins starts. 2) When the Save or Apply
     * button are pressed in a job, after the Stop() configuration in case this
     * plugin is activated. 3) It is also called when restarting a connection
     * because of changes in the main configuration, after the Stop()
     *
     * It checks if there is all the information required for an XMPP connection
     * in the main configuration, subscribes when required and adds listeners.
     *
     * @param project The project currently being started
     * @param newInstance
     */
    @Override
    public void start(Project project, boolean newInstance) {
        String server = this.getDescriptor().server;
        String user = this.getDescriptor().user;
        String password = this.getDescriptor().password;
        this.project = project;

        try {
            if (!checkAnyParameterEmpty(server, user, password)) {
                if (this.getDescriptor().xmppCon.isConnected() && this.getDescriptor().xmppCon.isAuthenticated()) {
                    addListeners(this.getDescriptor().xmppCon, user);
                    try {
                        subscribeIfNecessary(project);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(ElOyente.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        } catch (XMPPException ex) {
            ex.printStackTrace(System.err);
        }
    }

    /**
     * Checks if all the parameters from the main configuration are filled.
     *
     * @param server Server from the main configuration
     * @param user User from the main configuration
     * @param password Password from the main configuration
     *
     */
    public static boolean checkAnyParameterEmpty(String server, String user, String password) {
        if (server != null && !server.isEmpty() && user != null && !user.isEmpty() && password != null && !password.isEmpty()) {
            return false;
        }
        return true;
    }

    /**
     *
     * Method for subscribing a job to the nodes in the XMPP server.
     *
     * This method will subscribe a job to the nodes specified in the
     * subscriptions if it's not already subscribed or there are not any other
     * nodes already subscribed to that node.
     *
     * @param project The project being started.
     * @throws XMPPException
     *
     */
    public synchronized void subscribeIfNecessary(Project project) throws XMPPException, InterruptedException {

        String nodeName;
        Connection con = this.getDescriptor().xmppCon;
        PubSubManager mgr = this.getDescriptor().psm;
        if (mgr.discoverNodes(null).getItems().hasNext()) {

            if (subscriptions != null && subscriptions.length != 0) {
                for (int i = 0; i < subscriptions.length; i++) {
                    nodeName = subscriptions[i].getNode();
                    if (existsNode(nodeName)) {
                        this.getDescriptor().checkAndAddSubscription(nodeName);
                    }
                }
            }
        }
    }

    /**
     * Adds listeners to the nodes.
     *
     * It creates listeners for a job to the nodes specified in its
     * subscriptions. It does this in case the listener for that node doesn't
     * exist yet, otherwise it just adds the trigger to the array of triggers of
     * that listener.
     *
     *
     * @param con - The connection for which the listener will be created.
     * @param project - The project being configured.
     * @throws XMPPException
     */
    public synchronized void addListeners(Connection con, String user) throws XMPPException {

        PubSubManager mgr = this.getDescriptor().psm;

        if (subscriptions != null && subscriptions.length != 0) {
            for (int i = 0; i < subscriptions.length; i++) {

                //Checking if the node exist before creating the listener
                if (existsNode(subscriptions[i].node)) {
                    LeafNode node = (LeafNode) mgr.getNode(subscriptions[i].node);
                    synchronized (listeners) {
                        if (!listeners.containsKey(node.getId())) {
                            ItemEventCoordinator itemEventCoordinator = new ItemEventCoordinator(node.getId());
                            itemEventCoordinator.addTrigger(this);
                            node.addItemEventListener(itemEventCoordinator);
                            listeners.put(node.getId(), itemEventCoordinator);
                            System.out.println("Listener added for node: " + node.getId() + " for project " + project.getName());
                        } else {
                            listeners.get(node.getId()).addTrigger(this);
                            System.err.println("No need to add new listener to node " + node.getId() + " for project " + project.getName());
                        }
                    }
                }
            }
        }
    }

    /**
     * Run a job.
     *
     * This method will be called by the ItemEventCoordinator when an XMPP event
     * is received and a job must be triggered.
     *
     */
    @Override
    public void run() {
        try {
            runWithEnvironment(null);
        } catch (InterruptedException ex) {
            Logger.getLogger(ElOyente.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Schedules a build.
     *
     * Called by the run() method it schedules a build using the environment
     * variables of the subscriptions and setting the Quiet Period to 0.
     *
     * @param vars The environment variables to be set based on those passed by
     * the user.
     */
    public synchronized void runWithEnvironment(EnvVars vars) throws InterruptedException {
        Boolean done;
        int n = 0;
        int m = 0;
        if (!project.getAllJobs().isEmpty()) {
            Iterator iterator = this.project.getAllJobs().iterator();
            while (iterator.hasNext()) {
                Project p = ((Project) iterator.next());
                System.out.println("Build scheduled for project: " + p.getName());
                if (p.isInQueue()) {
                    while (m <= 12) {
                        m = (int) Math.pow(2, n);
                        Thread.currentThread().sleep(m * 1000);
                        System.out.println(p.getName() + " is in the queue! wait " + m + " segundos");
                        done = p.scheduleBuild(0, new ElOyenteTriggerCause(vars));
                        if (!done) {
                            n++;
                        } else {
                            break;
                        }
                    }
                } else {
                    done = p.scheduleBuild(0, new ElOyenteTriggerCause(vars));
                    System.out.println(p.getName() + " executed: " + done);
                }

            }
        }
    }

    /**
     * Called before a Trigger is removed.
     *
     * This method will be called when a job configuration is saved. It will
     * check the subscriptions for that job, delete the trigger from the
     * listeners of the nodes it's listening to (if no more triggers in that
     * listener it will remove the listener too). In case the trigger is removed
     * it will unsubscribe from that node. The start(Project, boolean) method
     * will be called after it.
     *
     */
    @Override
    public void stop() {

        if (subscriptions != null && subscriptions.length != 0) {
            String nodeName;
            Connection con = this.getDescriptor().xmppCon;
            PubSubManager mgr = this.getDescriptor().psm;
            List<Subscription> subscriptionList;
            Iterator it2;

            for (int i = 0; i < subscriptions.length; i++) {
                try {
                    nodeName = subscriptions[i].node;
                    if (existsNode(nodeName)) {

                        LeafNode n = (LeafNode) mgr.getNode(nodeName);

                        //Remove listener
                        synchronized (listeners) {
                            if (listeners.containsKey(nodeName)) {
                                ArrayList<ElOyente> a = listeners.get(nodeName).Triggers;
                                if (a.contains(this)) {
                                    a.remove(this);
                                }

                                if (a.isEmpty()) {
                                    try {
                                        mgr.getNode(nodeName).removeItemEventListener(listeners.get(nodeName));
                                        listeners.put(nodeName, null);
                                        listeners.remove(nodeName);
                                        System.out.println("The Listener of the node has been removed");

                                        //Unsubscribe
                                        subscriptionList = mgr.getSubscriptions();

                                        it2 = subscriptionList.iterator();

                                        while (it2.hasNext()) {
                                            Subscription sub = (Subscription) it2.next();

                                            if (sub.getNode().equals(nodeName) && sub.getJid().equals(con.getUser())) {
                                                n.unsubscribe(con.getUser(), sub.getId());
                                                break;
                                            }
                                        }

                                    } catch (XMPPException ex) {
                                        Logger.getLogger(ElOyente.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                }
                            }
                        }

                    } else {
                        System.out.println("The old node doesn't exixt!!");
                    }
                } catch (XMPPException ex) {
                    Logger.getLogger(ElOyente.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    /**
     *
     * Used to determine if a node exists.
     *
     * It retrieves the nodes from the XMPP server's database and determines if
     * the node wanted exists in it.
     *
     * @param key The name of the node to look for.
     * @return nodeExists Boolean value indicating if the node exists or not.
     * @throws XMPPException
     */
    private boolean existsNode(String key) throws XMPPException {

        boolean nodeExists = false;
        DiscoverItems discoverNodes = this.getDescriptor().psm.discoverNodes(null);
        Iterator<DiscoverItems.Item> items = discoverNodes.getItems();

        while (items.hasNext()) {
            if (((DiscoverItems.Item) items.next()).getNode().equals(key)) {
                nodeExists = true;
                break;
            }
        }
        return nodeExists;
    }

    /**
     * Retrieves the descriptor for the plugin.
     *
     * Used by the plugin to set and work with the main configuration.
     *
     * @return DescriptorImpl
     */
    @Override
    public final DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    /**
     * Descriptor for {@link ElOyente}. Used as a singleton. The class is marked
     * as public so that it can be accessed from views.
     *
     * <p> See
     * <tt>src/main/resources/hudson/plugins/eloyente/elOyente/*.jelly</tt> for
     * the actual HTML fragment for the configuration screen.
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends TriggerDescriptor {

        /**
         * To persist global configuration information, simply store it in a
         * field and call save().
         *
         * <p> If you don't want fields to be persisted, use <tt>transient</tt>.
         */
        private String server;
        private String user;
        private String password;
        protected transient ConnectionConfiguration config;
        protected transient XMPPConnection xmppCon;
        protected transient PubSubManager psm;

        /**
         * Brings the persisted configuration in the main configuration.
         *
         * Brings the persisted configuration in the main configuration.
         */
        public DescriptorImpl() {

            load();
            connectXMPP();
        }

        /**
         * Returns true if this task is applicable to the given project.
         *
         * True to allow user to configure this post-promotion task for the
         * given project.
         *
         * @param item
         */
        @Override
        public boolean isApplicable(Item item) {
            return true;
        }

        /**
         * Human readable name of this kind of configurable object.
         */
        @Override
        public String getDisplayName() {
            return "XMPP triggered plugin";
        }

        /**
         * Invoked when the global configuration page is submitted.
         *
         * When click on the "Save" button of the main configuration this method
         * will be called. It will then stop all the jobs that are using
         * ElOyente, get the new credentials, make them persistent and start all
         * those jobs back with the new credentials specified.
         *
         * @param req
         * @param formData
         * @throws Descriptor.FormException
         */
        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws Descriptor.FormException {
            // To persist global configuration information,
            // set that to properties and call save().

            stopJobs();

            server = formData.getString("server");
            user = formData.getString("user");
            password = formData.getString("password");

            save();
            startJobs();

            return super.configure(req, formData);
        }

        /**
         * This method checks which jobs are using ElOyente and stop them.
         *
         * It calls the method stop() of all the jobs that are using the trigger
         * in order to remove the subscriptions, listeners, etc.
         */
        public void stopJobs() {
            if (xmppCon != null && xmppCon.isConnected() && xmppCon.isAuthenticated()) {
                Iterator it2 = (Jenkins.getInstance().getItems()).iterator();
                while (it2.hasNext()) {
                    Project job = (Project) it2.next();
                    ElOyente instance = (ElOyente) job.getTriggers().get(this);
                    if (instance != null) {
                        System.out.println("Stopping job: " + job.getName());
                        instance.stop();
                    }
                }
                xmppCon.disconnect();
            }
        }

        /**
         * This method checks which jobs are using ElOyente and start them back.
         *
         * It calls the method start() of all the jobs that are using the
         * trigger in order to connect to the server with the new credentials
         * and reset the subscriptions, listeners, etc.
         */
        public void startJobs() {
            if (connectXMPP()) {
                Iterator it2 = (Jenkins.getInstance().getItems()).iterator();
                while (it2.hasNext()) {
                    Project job = (Project) it2.next();
                    ElOyente instance = (ElOyente) job.getTriggers().get(this);
                    if (instance != null) {
                        System.out.println("Starting job: " + job.getName());
                        instance.start(job, true);
                    }
                }
            }
        }

        /**
         * Used to create the XMPP connection.
         *
         * This method is called when Jenkins is started or when there are
         * changes in the main configuration
         *
         * @return Boolean indicating if it was possible to stablish a
         * connection or not.
         */
        private synchronized boolean connectXMPP() {
            if (server != null && !server.isEmpty() && user != null && !user.isEmpty() && password != null && !password.isEmpty()) {
                config = new ConnectionConfiguration(server);
                xmppCon = new XMPPConnection(config);

                if (!xmppCon.isConnected()) {
                    try {
                        xmppCon.connect();
                        if (!xmppCon.isAuthenticated()) {
                            String resource = "" + xmppCon;
                            String pepe = Jenkins.SESSION_HASH;
                            try {
                                pepe = Jenkins.getInstance().getRootUrl();
                            } catch (Exception ex) {
                                System.out.println("FOLLONERO");
                            }
                            xmppCon.login(user, password, pepe);
                            psm = new PubSubManager(xmppCon);
                            return true;
                        } else {
                            System.out.println("Not need to login!");
                            return false;
                        }
                    } catch (XMPPException ex) {
                        System.out.println("Failed to connect");
                        return false;
                    }
                } else {
                    System.out.println("Already connected");
                    return true;
                }
            } else {
                System.out.println("Empty fields in main configuration!");
                return false;
            }
        }

        public synchronized void checkAndAddSubscription(String nodeName) throws XMPPException {
            if (!isSubscribed(nodeName) && !nodeName.equals("")) {
                Node node = psm.getNode(nodeName);
                String JID = xmppCon.getUser();
                psm.getNode(nodeName).subscribe(JID);
                System.out.println("Project subscribed to node " + node.getId());
            }
        }

        /**
         * Checks if there exists already a subscription to the node specified.
         *
         * @param nodeName Name of the node.
         * @return Boolean that indicates if subscribed.
         * @throws XMPPException
         */
        private boolean isSubscribed(String nodeName) throws XMPPException {

            List<Subscription> subscriptionList;
            Iterator it2;

            subscriptionList = psm.getSubscriptions();
            it2 = subscriptionList.iterator();

            while (it2.hasNext()) {
                Subscription sub = (Subscription) it2.next();

                if (sub.getNode().equals(nodeName) && sub.getJid().equals(xmppCon.getUser())) {
                    return true;
                }
            }

            return false;
        }

        /**
         * This method returns the URL of the XMPP server.
         *
         *
         * global.jelly calls this method to obtain the value of field server.
         *
         */
        public String getServer() {
            return server;
        }

        /**
         * This method returns the username for the XMPP connection.
         *
         *
         * global.jelly calls this method to obtain the value of field user.
         *
         */
        public String getUser() {
            return user;
        }

        /**
         * This method returns the password for the XMPP connection.
         *
         *
         * global.jelly calls this method to obtain the value of field password.
         *
         */
        public String getPassword() {
            return password;
        }

        /**
         * Performs on-the-fly validation of the form field 'server'.
         *
         * This method checks if the connection to the XMPP server specified is
         * available. It shows a notification describing the status of the
         * server connection.
         *
         * @param server Server from the the main configuration.
         */
        public FormValidation doCheckServer(@QueryParameter String server) {

            try {
                config = new ConnectionConfiguration(server);
                Connection con = new XMPPConnection(config);

                if (server.isEmpty()) {
                    return FormValidation.warningWithMarkup("No server specified");
                }
                con.connect();
                if (con.isConnected()) {
                    con.disconnect();
                    return FormValidation.okWithMarkup("Connection available");
                }
                return FormValidation.errorWithMarkup("Couldn't connect");
            } catch (XMPPException ex) {
                return FormValidation.errorWithMarkup("Couldn't connect");
            }
        }

        /**
         * Performs on-the-fly validation of the form fields 'user' and
         * 'password'.
         *
         * This method checks if the user and password of the XMPP server
         * specified are correct and valid. It shows a notification describing
         * the status of the login.
         *
         * @param user User from the the main configuration.
         * @param password Password from the the main configuration.
         *
         */
        public FormValidation doCheckPassword(@QueryParameter String user, @QueryParameter String password, @QueryParameter String server) {
            config = new ConnectionConfiguration(server);
            Connection con = new XMPPConnection(config);
            if ((user.isEmpty() || password.isEmpty()) || server.isEmpty()) {
                return FormValidation.warningWithMarkup("Not authenticated");
            }
            try {
                con.connect();
                con.login(user, password);
                if (con.isAuthenticated()) {
                    con.disconnect();
                    return FormValidation.okWithMarkup("Authentication succed");
                }
                return FormValidation.warningWithMarkup("Not authenticated");
            } catch (XMPPException ex) {
                return FormValidation.errorWithMarkup("Authentication failed");
            }
        }

        /**
         * Fill the drop-down called "node" of the config.jelly
         *
         * @throws XMPPException
         * @throws InterruptedException
         */
        public ListBoxModel doFillNodeItems() throws XMPPException, InterruptedException {

            ListBoxModel items = new ListBoxModel();

            config = new ConnectionConfiguration(server);
            Connection con = new XMPPConnection(config);
            PubSubManager mgr = new PubSubManager(con);

            DiscoverItems discoverNodes = mgr.discoverNodes(null);
            Iterator<DiscoverItems.Item> it = discoverNodes.getItems();
            while (it.hasNext()) {
                items.add(it.next().getNode());
            }

            return items;
        }
    }
}