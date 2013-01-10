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
import hudson.model.Items;
import hudson.model.Project;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.io.File;
import java.io.IOException;
import java.io.ObjectStreamException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
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

    private final static Integer USER_ID = 0;
    private final static Integer RESOURCE_ID = 1;
    /**
     * Map of connections to the XMPP server.
     *
     * (key: Job Name; Value: Connection). 
     *
     * Each project/job has its own connection. The id of these connections have
     * the form: userName/jobName (where "userName" is the name of the user
     * defined in the main configuration panel.)
     */
    protected final static Map<String, Connection> connections = new HashMap<String, Connection>();
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
     * the subscriptions and schedule de builds).
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
     * job the trigger is attached.
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
     * @param node The name of the node. properties for that node.
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
     * This method is called when the Save or Apply button are pressed in a job
     * configuration in case this plugin is activated. It is also called (indirectly with the "load" function) when
     * restarting a connection because of changes in the main configuration.
     *
     * It checks if there is all the information required for an XMPP connection
     * in the main configuration and creates the connection, subscribes when
     * necessary and adds listeners.
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
                if (connectionOK(server, user, password)) {
                    Connection con = createConnection(project, server, user, password);
                    subscribeIfNecessary(project);
                    addListeners(con, user);
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
     */
    public boolean checkAnyParameterEmpty(String server, String user, String password) {
        if (server != null && !server.isEmpty() && user != null && !user.isEmpty() && password != null && !password.isEmpty()) {
            return false;
        }
        return true;
    }

    /**
     * Check if it is possible to connect to the XMPP server.
     *
     * Check if it is possible to connect to the XMPP server with the parameters
     * specified in the main configuration.
     *
     * @param server Server from the main configuration
     * @param user User from the main configuration
     * @param password Password from the main configuration
     */
    public static synchronized boolean connectionOK(String server, String user, String password) {

        try {
            ConnectionConfiguration config = new ConnectionConfiguration(server);
            Connection con = new XMPPConnection(config);
            con.connect();
            con.login(user, password);
            con.disconnect();
            return true;
        } catch (XMPPException ex) {
            return false;
        }
    }

    /**
     *
     * @param project Project being started
     * @param server Server to which the plugin is connected
     * @param user User for the server
     * @param password Password for the server
     * @throws XMPPException
     */
    public Connection createConnection(Project project, String server, String user, String password) throws XMPPException {
        if (!connections.containsKey(project.getName())) {
            ConnectionConfiguration config = new ConnectionConfiguration(server);
            Connection con = new XMPPConnection(config);
            con.connect();
            con.login(user, password, project.getName());
            connections.put(project.getName(), con);
            return con;
        } else {
            return connections.get(project.getName());
        }
    }

    /**
     * Checks if the JID has the form expected and returns a Map with the UserID
     * y el ResourceID
     */
    protected Map<Integer, String> parseJID(Subscription sub) {
        String JID = sub.getJid();

        int atPos = JID.indexOf('@');
        int slashPos = JID.indexOf('/');
        if (atPos == -1 || slashPos == -1) {
            return null;
        }
        HashMap<Integer, String> res = new HashMap<Integer, String>();
        res.put(USER_ID, JID.substring(0, atPos));
        res.put(RESOURCE_ID, JID.substring(slashPos + 1));
        return res;
    }

    /**
     *
     * Method for subscribing a job to the nodes in the XMPP server.
     *
     * This method will subscribe a job to the nodes specified in the
     * subscriptions if it's not already subscribed.
     *
     * @param project The project being started.
     * @throws XMPPException
     */
    public void subscribeIfNecessary(Project project) throws XMPPException {
        boolean subscribed = false;
        String nodeName;
        Connection con = connections.get(project.getName());
        PubSubManager mgr = new PubSubManager(con);
        if (mgr.discoverNodes(null).getItems().hasNext()) {

            List<Subscription> subscriptionList;
            Iterator it2;
            if (subscriptions != null) {
                if (subscriptions.length != 0) {
                    for (int i = 0; i < subscriptions.length; i++) {
                        nodeName = subscriptions[i].getNode();
                        subscriptionList = mgr.getSubscriptions();
                        it2 = subscriptionList.iterator();

                        while (it2.hasNext()) {
                            Subscription sub = (Subscription) it2.next();
                            Map<Integer, String> jid = parseJID(sub);
                            if (null == jid || jid.size() < 2) {
                                continue;
                            }

                            if (jid.get(RESOURCE_ID).equals(project.getName()) && sub.getNode().equals(nodeName) && jid.get(USER_ID).equals(getDescriptor().user)) {
                                subscribed = true;
                                break;
                            }
                        }

                        if (!subscribed && !nodeName.equals("")) {
                            DiscoverItems discoverNodes = mgr.discoverNodes(null);
                            Iterator<DiscoverItems.Item> items = discoverNodes.getItems();

                            boolean nodeExists = false;
                            while (items.hasNext()) {
                                if (((DiscoverItems.Item) items.next()).getNode().equals(nodeName)) {
                                    nodeExists = true;
                                    break;
                                }
                            }
                            if (nodeExists) {
                                Node node = mgr.getNode(nodeName);
                                String JID = con.getUser();
                                mgr.getNode(nodeName).subscribe(JID);
                                System.out.println("Project " + project.getName() + " subscribed to node " + node.getId());
                            }
                        }
                        subscribed = false;
                    }

                }
            }
        }
    }

    /**
     * Add listeners to the connections.
     *
     * This method is in charge of adding listeners to a node to which a job is
     * subscribed to. This way it will receive events that will trigger it.
     *
     * @param con - The connection for which the listener will be created.
     * @param project - The project being configured.
     * @throws XMPPException
     */
    public void addListeners(Connection con, String user) throws XMPPException {
        PubSubManager mgr = new PubSubManager(con);

        if (subscriptions != null) {
            for (int i = 0; i < subscriptions.length; i++) {

                //Checking if the node exist before creating the listener
                String key = subscriptions[i].node;

                DiscoverItems discoverNodes = mgr.discoverNodes(null);
                Iterator<DiscoverItems.Item> items = discoverNodes.getItems();

                boolean nodeExists = false;
                while (items.hasNext()) {
                    if (((DiscoverItems.Item) items.next()).getNode().equals(key)) {
                        nodeExists = true;
                        break;
                    }
                }
                if (nodeExists) {
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
        runWithEnvironment(null);
    }

    /**
     * Schedules a build.
     *
     */
    public void runWithEnvironment(EnvVars vars) {
        Boolean done;
        if (!project.getAllJobs().isEmpty()) {
            Iterator iterator = this.project.getAllJobs().iterator();
            while (iterator.hasNext()) {
                Project p = ((Project) iterator.next());
                System.out.print("Build scheduled for project: " + p.getName());
                done = p.scheduleBuild(0, new ElOyenteTriggerCause(vars));
                System.out.println("..." + done);
            }
        }
    }

    /**
     * Called before a Trigger is removed.
     *
     * This method will be called when the main configuration or a job
     * configuration are saved.
     *
     */
    @Override
    public void stop() {
        try {
            Connection con = connections.get(project.getName());
            PubSubManager mgr = new PubSubManager(con);
            synchronized (listeners) {
                for (String nodeName : listeners.keySet()) {
                    LeafNode n = (LeafNode) mgr.getNode(nodeName);
                    mgr.getNode(nodeName).removeItemEventListener(listeners.get(nodeName));
                    listeners.remove(nodeName);
                    n.unsubscribe(con.getUser());
                }
            }
            con.disconnect();
            connections.remove(project.getName());
        } catch (XMPPException ex) {
            System.err.println(ex);
        }
        project = null;
        //listeners = null;
        subscriptions = null;
        //super.stop();
    }

    public void deleteJob() {
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

        /**
         * Brings the persisted configuration in the main configuration.
         *
         * Brings the persisted configuration in the main configuration.
         */
        public DescriptorImpl() {
            load();
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
         * Invoked when the global configuration page is submitted..
         *
         * Overriden in order to persist the main configuration, reporting and
         * and call the method reloadJobs, that we'll decide if it's necessary
         * to reload or not.
         *
         * @param req
         * @param formData// public void listen(Node node, Trigger trigger) { //
         * ItemEventCoordinator itemEventCoordinator = new
         * ItemEventCoordinator(node.getId(), trigger); //
         * node.addItemEventListener(itemEventCoordinator); // }
         * @throws Descriptor.FormException
         */
        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws Descriptor.FormException {
            // To persist global configuration information,
            // set that to properties and call save().
            server = formData.getString("server");
            user = formData.getString("user");
            password = formData.getString("password");

            //report();
            save();
            reloadJobs();

            return super.configure(req, formData);
        }

        /**
         * This method reloads the jobs that are using the trigger applying the
         * new main configuration.
         *
         * Checks if the parameters username, password and server of the main
         * configuration have changed, if so it calls the method start() of all
         * those jobs that are using the trigger in order to connect to the
         * server with the new credentials and reset the subscriptions.
         */
        public void reloadJobs() {

            Iterator it2 = (Jenkins.getInstance().getItems()).iterator();
            while (it2.hasNext()) {
                AbstractProject job = (AbstractProject) it2.next();
//                if (connections.containsKey(job.getName())) {
//                    ((Connection) connections.get(job.getName())).disconnect();
//                    connections.remove(job.getName());
//                }
                ElOyente instance = (ElOyente) job.getTriggers().get(this);
                if (instance != null) {
                    System.out.println("Reloading job: " + job.getName());
                    //START()!!!!!!!!!!!!!!!!!1
                    File directoryConfigXml = job.getConfigFile().getFile().getParentFile();
                    try {
                        instance.stop();
                        Items.load(job.getParent(), directoryConfigXml);

                    } catch (IOException ex) {
                        Logger.getLogger(ElOyente.class.getName()).log(Level.SEVERE, null, ex);
                        System.out.println(ex);
                    }
                }
            }
        }

        /**
         * Used for logging to the log file.
         *
         * This method reports information related to XMPPadmin events like
         * "Available Nodes", connection information, etc. It creates a
         * connection to take the required data for reporting and it closes it
         * after. It is used in the main configuration every time the Save or
         * Apply buttons are pressed.
         *
         */
        public void report() {
            Logger logger = Logger.getLogger("com.technicolor.eloyente");

            if (server != null && !server.isEmpty() && user != null && !user.isEmpty() && password != null && !password.isEmpty()) {
                try {
                    //Connection
                    ConnectionConfiguration config = new ConnectionConfiguration(server);
                    PubSubManager mgr;
                    Connection con = new XMPPConnection(config);
                    con.connect();
                    logger.log(Level.INFO, "Connection established");
                    if (con.isConnected()) {

                        //Login
                        con.login(user, password, "Global");
                        logger.log(Level.INFO, "JID: {0}", con.getUser());
                        logger.log(Level.INFO, "{0} has been logged to openfire!", user);

                        //Log the availables nodes
                        mgr = new PubSubManager(con);
                        DiscoverItems items = mgr.discoverNodes(null);
                        Iterator<DiscoverItems.Item> iter = items.getItems();

                        logger.log(Level.INFO, "NODES: ---------------------------------");
                        while (iter.hasNext()) {
                            DiscoverItems.Item i = iter.next();
                            logger.log(Level.INFO, "Node: {0}", i.getNode());
                        }
                        logger.log(Level.INFO, "END NODES: -----------------------------");

                        //Disconnection
                        con.disconnect();
                    }
                } catch (XMPPException ex) {
                    System.err.println(ex.getXMPPError().getMessage());
                }
            }
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
                ConnectionConfiguration config = new ConnectionConfiguration(server);
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
         */
        public FormValidation doCheckPassword(@QueryParameter String user, @QueryParameter String password, @QueryParameter String server) {
            ConnectionConfiguration config = new ConnectionConfiguration(server);
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

            ConnectionConfiguration config = new ConnectionConfiguration(server);
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
