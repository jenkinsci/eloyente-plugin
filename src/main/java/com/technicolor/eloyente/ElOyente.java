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
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.model.Project;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.io.ObjectStreamException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
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
        synchronized (listeners) {      // added by StesB
            if (listeners == null) {    // added by StesB
                listeners = new HashMap<String, ItemEventCoordinator>();
            }                           //added by StesB                   
        }                               //added by StesB
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
     * plug-in is activated. 3) It is also called when restarting a connection
     * because of changes in the main configuration, after the Stop()
     *
     * It checks if there is all the information required for an XMPP connection 
     * in the main configuration, logs in, adds listeners and subscribes when required.
     *
     * @param project The project currently being started
     * @param newInstance
     */
    @Override
    public void start(Project project, boolean newInstance) {
        DescriptorImpl desc = this.getDescriptor();
        String server = desc.server;
        String user = desc.user;
        String password = desc.password;
        XMPPConnection con = desc.xmppCon;
        this.project = project;

        try {
            if (!checkAnyParameterEmpty(server, user, password)) {
                synchronized (this.getDescriptor().xmppCon) {
                    if (this.getDescriptor().xmppCon.isConnected()) {
                        
                        if (!this.getDescriptor().xmppCon.isAuthenticated()) {
                            String pepe = Jenkins.getInstance().getRootUrl();
                            try {
                                this.getDescriptor().xmppCon.login(user, password, pepe);
                            } catch (XMPPException ex) {
                                this.getDescriptor().xmppCon.disconnect();
                                System.err.println("Autentication failure");
                            }
                        }

                        if(this.getDescriptor().xmppCon!=null && this.getDescriptor().xmppCon.isAuthenticated()) {
                            addListeners(this.getDescriptor().xmppCon, user);
                            try {
                                subscribeIfNecessary(project);
                            } catch (InterruptedException ex) {
                                Logger.getLogger(ElOyente.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                }
            }
        } catch (XMPPException ex) {
            System.err.println();
        }
    }

    /**
     * Checks if all the parameters from the main configuration are filled.
     *
     * @param server Server from the main configuration
     * @param user User from the main configuration
     * @param password Password from the main configuration
     *
     * @return true if the one of the parameters is empty, false otherwise.
     */
    private static boolean checkAnyParameterEmpty(String server, String user, String password) {
        if (server != null && !server.isEmpty() && user != null && !user.isEmpty() && password != null && !password.isEmpty()) {
            return false;
        }
        return true;
    }

    /**
     * Method for subscribing a job to the nodes in the XMPP server.
     *
     * This method will subscribe the connection to the nodes specified in the
     * subscriptions(job configuration GUI) if it's not already subscribed in
     * the XMPP database.
     *
     * @param project The project being started.
     * @throws XMPPException
     *
     */
    private synchronized void subscribeIfNecessary(Project project) throws XMPPException, InterruptedException {
        String nodeName;
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
     * Adds listener to a node.
     *
     * It creates and attaches a listener per each different node specified in
     * its subscriptions(in the job configuration GUI). It does this in case the
     * listener for that node doesn't exist yet, otherwise it just adds the
     * trigger into a local variable "listeners" where we track the listeners
     * already added, to be more concrete it is added into the array of triggers
     * of that local listener, because it is removed from the array each time we
     * pass by the stop methode. When you loose the connection to the XMPP
     * server the listeners disappear, So each time to start Jenkins a new
     * connection is established and the listeners added.
     *
     *
     * @param con - The connection for which the listener will be created.
     * @param project - The project being configured.
     * @throws XMPPException
     */
    private synchronized void addListeners(Connection con, String user) throws XMPPException {

        PubSubManager mgr = this.getDescriptor().psm;

        if (subscriptions != null && subscriptions.length != 0) {
            for (int i = 0; i < subscriptions.length; i++) {

                //Checking if the node exist before creating the listener
                if (existsNode(subscriptions[i].node)) {
                    LeafNode node = (LeafNode) mgr.getNode(subscriptions[i].node);
                    synchronized (listeners) {
                        //If it is already in the local variable listeners -> create a new one
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
            runWithEnvironment(null, null, null);
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
    protected synchronized void runWithEnvironment(String event, String filter, EnvVars vars) throws InterruptedException {
        Boolean done;
        int n = 0;
        int m = 0;
        if (!project.getAllJobs().isEmpty()) {
            Iterator iterator = this.project.getAllJobs().iterator();
            while (iterator.hasNext()) {
                Project p = ((Project) iterator.next());
                System.out.println("Build scheduled for project: " + p.getName());
                //When the job is already in the queue (penging to be built) we introduce a delay before scheduling the next job.
                //For some reason this second job is not allways triggered, so we check it and if this was the case we schedule it again adding a bit mor of dealy (max 15 sec).
                if (p.isInQueue()) {
                    while (m <= 12) {
                        m = (int) Math.pow(2, n);
                        Thread.currentThread().sleep(m * 1000);
                        System.out.println(p.getName() + " is in the queue! wait " + m + " segundos");
                        done = p.scheduleBuild(0, new ElOyenteTriggerCause(event, filter, vars));
                        if (!done) {
                            n++;
                        } else {
                            break;
                        }
                    }
                } else {
                    done = p.scheduleBuild(0, new ElOyenteTriggerCause(event, filter, vars));
                    System.out.println(p.getName() + " executed: " + done);
                }

            }
        }
    }

    /**
     * Stop the XMPP elements of the job (Subscriptions, Listeners).
     *
     * This method is called when: 1) When the Save or Apply button are pressed
     * in a job configuration GUI 2) It is also called when restarting a
     * connection because of changes in the main configuration.
     *
     * It will check the subscriptions for that job, delete the trigger from the
     * listeners of the nodes it's listening to (if no more triggers in that
     * listener it will remove the listener too a). In case the trigger is
     * removed it will unsubscribe from that node. The start(Project, boolean)
     * method will be called after it.
     *
     */
    @Override
    public void stop() {

        if (this.getDescriptor().xmppCon!=null && this.getDescriptor().xmppCon.isConnected() && this.getDescriptor().xmppCon.isAuthenticated()) {
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
                            System.out.println("The old node doesn't exist!!");
                        }
                    } catch (XMPPException ex) {
                        Logger.getLogger(ElOyente.class.getName()).log(Level.SEVERE, null, ex);
                    }
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
         * Brings the persisted configuration in the main configuration and
         * create the XMPP connection.
         *
         * Brings the persisted configuration in the main configuration and
         * create the XMPP connection..
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

            synchronized (this) {
                stopJobs();

                server = formData.getString("server");
                user = formData.getString("user");
                password = formData.getString("password");

                save();
                startJobs();
            }

            return super.configure(req, formData);
        }

        /**
         * This method checks which jobs are using ElOyente and stop them.
         *
         * It calls the method stop() of all the jobs that are using the trigger
         * in order to remove the subscriptions, listeners, etc.
         */
        private void stopJobs() {
            if (xmppCon != null && xmppCon.isConnected() && xmppCon.isAuthenticated()) {
                Iterator it2 = Jenkins.getInstance().getItems().iterator();
                while (it2.hasNext()) {
                    AbstractProject job = (AbstractProject) it2.next();
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
        private void startJobs() {
            if (connectXMPP()) {
                Iterator it2 = Jenkins.getInstance().getItems().iterator();
                while (it2.hasNext()) {
                    AbstractProject job = (AbstractProject) it2.next();
                    ElOyente instance = (ElOyente) job.getTriggers().get(this);
                    if (instance != null) {
                        System.out.println("Starting job: " + job.getName());
                        instance.start((Project)job, true);
                    }
                }
            }
        }

        /**
         * Used to create the XMPP connection.
         *
         * This method is called when Jenkins is started or when there are
         * changes in the main configuration. It creates a new XMPP connection.
         *
         * @return Boolean indicating if it was possible to establish a
         * connection or not.
         */
        private synchronized boolean connectXMPP() {
            if (server != null && !server.isEmpty() && user != null && !user.isEmpty() && password != null && !password.isEmpty()) {
                config = new ConnectionConfiguration(server);
                xmppCon = new XMPPConnection(config);

                if (!xmppCon.isConnected()) {
                    try {
                        xmppCon.connect();
                        psm = new PubSubManager(xmppCon);
                        return true;
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

        /**
         * Check if the the connections has already a subscription for that node
         * and subscribes it if not.
         *
         * This method is called in subscribeIfNecessary
         *
         */
        protected synchronized void checkAndAddSubscription(String nodeName) throws XMPPException {
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
        public synchronized String getServer() {
            return server;
        }

        /**
         * This method returns the username for the XMPP connection.
         *
         *
         * global.jelly calls this method to obtain the value of field user.
         *
         */
        public synchronized String getUser() {
            return user;
        }

        /**
         * This method returns the password for the XMPP connection.
         *
         *
         * global.jelly calls this method to obtain the value of field password.
         *
         */
        public synchronized String getPassword() {
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
        public synchronized FormValidation doCheckServer(@QueryParameter String server) {

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
        public synchronized FormValidation doCheckPassword(@QueryParameter String user, @QueryParameter String password, @QueryParameter String server) {
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
        
                /**
         * Performs on-the-fly validation of the form field 'Filter'.
         *
         * This method checks if the filter is valid. 
         * It shows a notification describing the status.
         *
         * @param filter Filter of the subscription.
         *
         */
        public synchronized FormValidation doCheckFilter(@QueryParameter String filter) {
            if (!filter.isEmpty()) {
                XPath xpath = XPathFactory.newInstance().newXPath();
                try{
                    xpath.compile(filter);
                }
                catch(XPathExpressionException e){
                     return FormValidation.errorWithMarkup("Invalid filter");
                }
                  return FormValidation.ok();
            }
            return FormValidation.ok();
        }
        
         /**
         * Performs on-the-fly validation of the form field 'Value selection'.
         *
         * This method checks if the Xpath expression to get the value of the 
         * environment variable is valid. 
         * It shows a notification describing the status.
         *
         * @param xpathe Value of the environment variable.
         *
         */       
        public synchronized FormValidation doCheckEnvExpr(@QueryParameter String xpathe) {
            if (!xpathe.isEmpty()) {
                XPath xpath = XPathFactory.newInstance().newXPath();
                try{
                    xpath.compile(xpathe);
                }
                catch(XPathExpressionException e){
                     return FormValidation.errorWithMarkup("Invalid xpath expresion");
                }
                  return FormValidation.ok();
            }
            return FormValidation.ok();
        }
    }
}
