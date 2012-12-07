package com.technicolor.eloyente;

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
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smackx.packet.DiscoverItems;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.pubsub.Node;
import org.jivesoftware.smackx.pubsub.PubSubManager;
import org.jivesoftware.smackx.pubsub.Subscription;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;

/**
 * @author Juan Luis Pardo Gonz&aacute;lez
 * @author Isabel Fern&aacute;ndez D&iacute;az
 */
public class ElOyente extends Trigger<Project> {

    private final static Integer USER_ID = 0;
    private final static Integer RESOURCE_ID = 1;
    private final static Map<String, Connection> connections = new HashMap<String, Connection>();
    protected SubscriptionProperties[] subscriptions;
    private transient Map<String, ItemEventCoordinator> listeners = new HashMap<String, ItemEventCoordinator>();
    protected transient Project project;

    @DataBoundConstructor
    public ElOyente(SubscriptionProperties[] s) {
        this.subscriptions = s;
    }
    
    @Override
    public Object readResolve() throws ObjectStreamException {
        super.readResolve();
        listeners = new HashMap<String, ItemEventCoordinator>();  
        System.out.println("readResolve created lesteners: "+ listeners);
        return this;
    }

    public List<SubscriptionProperties> getSubscriptions() {
        if (subscriptions == null) {
            return new ArrayList<SubscriptionProperties>();
        } else {
            return Arrays.asList(subscriptions);
        }
    }

    public List<SubscriptionProperties> getNodeSubscriptions(String node) {

        if (subscriptions == null) {
            return new ArrayList<SubscriptionProperties>();
        } else {
            int i;
            List<SubscriptionProperties> subsc = new ArrayList<SubscriptionProperties>();
            //return Arrays.asList(subscriptions);

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
     * configuration in case this plugin is activated.
     *
     * It checks if there is all the information required for an XMPP connection
     * in the main configuration and creates the connection.
     *
     * It is also called when restarting a connection because of changes in the
     * main configuration.
     *
     *
     * @param project
     * @param newInstance
     */
    @Override
    public void start(Project project, boolean newInstance) {

        String server = this.getDescriptor().server;
        String user = this.getDescriptor().user;
        String password = this.getDescriptor().password;
        this.project = project;

        try {
            if (getDescriptor().reloading) {
                if (!checkAnyParameterEmpty(server, user, password)) {
                    if (connectionOK(server, user, password)) {
                        if (!connections.isEmpty() && connections.containsKey(project.getName())) {
                            connections.get(project.getName()).disconnect();                                    //Reloading job because of parameter change, connection existing
                            connections.remove(project.getName());

                            Connection con = createConnection(project, server, user, password);
                            subscribeIfNecessary(project);
                            addListeners(con, user);
                        } else {
                            if (!checkAnyParameterEmpty(server, user, password)) {
                                Connection con = createConnection(project, server, user, password);              //Reloading job because of parameter change, no connection before
                                subscribeIfNecessary(project);
                                addListeners(con, user);

                            }
                        }
                    }
                }
            } else {
                if (!checkAnyParameterEmpty(server, user, password)) {
                    if (connectionOK(server, user, password)) {
                        Connection con = createConnection(project, server, user, password);                     // New job
                        subscribeIfNecessary(project);
                        addListeners(con, user);
                    }
                }
            }
        } catch (XMPPException ex) {
            ex.printStackTrace(System.err);
        }
    }

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

    private Map<Integer, String> parseJID(Subscription sub) {
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

    public void subscribeIfNecessary(Project project) throws XMPPException {
        boolean subscribed = false;
        String nodeName;
        Connection con = connections.get(project.getName());
        PubSubManager mgr = new PubSubManager(con);
        if (mgr.discoverNodes(null).getItems().hasNext()) {

            List<Subscription> subscriptionList;
            Iterator it2;

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
                            }
                        }
                        if (nodeExists) {
                            Node node = mgr.getNode(nodeName);
                            String JID = con.getUser();
                            mgr.getNode(nodeName).subscribe(JID);
                            System.out.println("Subscribe:--> Node: " + node.getId() + " pj: " + project.getName());
                        }
                    }
                    subscribed = false;
                }

            }
        }
    }

    public boolean checkAnyParameterEmpty(String server, String user, String password) {
        if (server != null && !server.isEmpty() && user != null && !user.isEmpty() && password != null && !password.isEmpty()) {
            return false;
        }
        return true;
    }

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
     * Add listeners to the connections.
     *
     * This method is in charge of adding listeners to a node to which a job is
     * subscribed to. This way it will receive events that will trigger it.
     *
     * @param con
     * @param project
     * @throws XMPPException
     */
    public void addListeners(Connection con, String user) throws XMPPException {
        PubSubManager mgr = new PubSubManager(con);

        for (int i = 0; i < subscriptions.length; i++) {

            LeafNode node = (LeafNode) mgr.getNode(subscriptions[i].node);
            System.out.println("NODO: " + subscriptions[i].node);
            System.out.println("NODO: " + node);
            System.out.println("NODO: " + node.getId());
            System.out.println("this: " + this + " - Listeners: " + listeners);

            if (!listeners.containsKey(node.getId())) {
                ItemEventCoordinator itemEventCoordinator = new ItemEventCoordinator(node.getId(), this);
                System.out.println("NEW ITEM EVENT COORDINATOR: " + itemEventCoordinator);
                // TODO: remove the next line
                itemEventCoordinator.print();
                node.addItemEventListener(itemEventCoordinator);
                listeners.put(node.getId(), itemEventCoordinator);
                System.out.println("itemEventCoordinator--> Node: " + node.getId() + " pj: " + project.getName());
            } else {
                System.err.println("NO se anade listener--> Node: " + node.getId() + " pj: " + project.getName());
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
        if (!project.getAllJobs().isEmpty()) {
            Iterator iterator = this.project.getAllJobs().iterator();
            while (iterator.hasNext()) {
                Project p = ((Project) iterator.next());
                System.out.println("ScheduleBuild: " + p.getName());
                p.scheduleBuild(null);
            }
        }
    }

    @Override
    public void stop() {
        System.out.println("Entering stop() of " + this);
        try {
            ConnectionConfiguration config = new ConnectionConfiguration(getDescriptor().server);
            Connection con = connections.get(project.getName());                
            PubSubManager mgr = new PubSubManager(con);
            System.out.println("config=" + config + " mgr=" + mgr + " con=" + con);
            for (String nodeName : listeners.keySet()) {
                System.out.println("nodeName: " + nodeName);
                LeafNode n = (LeafNode) mgr.getNode(nodeName);
                System.out.println("JID:"+con.getUser());
                n.unsubscribe(con.getUser());
//                ItemEventCoordinator listener = listeners.get(nodeName);
//                System.out.println("Removing ItemEventListener: " + listener + " for node " + n.getId());
//                // TODO: remove next line
//                listener.print();
//                n.removeItemEventListener(listener);
//                listener = null;
            }
//            listeners.clear();
            project=null;
            listeners=null;
            subscriptions=null;
            
            super.stop();

        } catch (XMPPException ex) {
            Logger.getLogger(ElOyente.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Leaving stop() of " + this);

    }

    /**
     * Retrieves the descriptor for the plugin elOyente.
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
        public boolean reloading = false;

        /**
         * Brings the persisted configuration in the main configuration.
         *
         * Brings the persisted configuration in the main configuration.
         */
        public DescriptorImpl() {
            load();
            reloading = false;
        }

//        @Override
//        public Trigger<?> newInstance(StaplerRequest req, JSONObject formData) throws FormException {
//            
//            List<SubscriptionProperties> tasksprops = req.bindParametersToList(SubscriptionProperties.class, "elOyente-suscription.suscriptionpropertes.");
//            return new ElOyente(tasksprops);
//            
//        }
        /**
         * Returns true if this task is applicable to the given project.
         *
         * True to allow user to configure this post-promotion task for the
         * given project.
         *
         * @return boolean
         * @param item
         */
        @Override
        public boolean isApplicable(Item item) {
            return true;
        }

        /**
         * Human readable name of this kind of configurable object.
         *
         *
         * @return String
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
         * @return boolean
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
         * This method reloads the jobs that are using ElOyente applying the new
         * main configuration.
         *
         * Checks if the parameters username, password and server of the main
         * configuration have changed, if so it calls the method start() of all
         * those jobs that are using ElOyente in order to connect to the server
         * with the new credentials and reset the subscriptions.
         */
        public void reloadJobs() {

            Iterator it2 = (Jenkins.getInstance().getItems()).iterator();
            while (it2.hasNext()) {
                AbstractProject job = (AbstractProject) it2.next();
                if (connections.containsKey(job.getName())) {
                    ((Connection) connections.get(job.getName())).disconnect();
                    connections.remove(job.getName());
                }
                Object instance = (ElOyente) job.getTriggers().get(this);
                if (instance != null) {
                    System.out.println(job.getName() + ": Yo tengo el plugin");
                    reloading = true;
                    File directoryConfigXml = job.getConfigFile().getFile().getParentFile();
                    try {
                        Items.load(job.getParent(), directoryConfigXml);

                    } catch (IOException ex) {
                        Logger.getLogger(ElOyente.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                    System.out.println(job.getName() + ": Yo no tengo el plugin");
                }

                reloading = false;
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
            //Logger loger =  Logger.getLogger(ElOyente.class.getName());

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
                            System.out.println("Node: " + i.toXML());
                            System.out.println("NodeName: " + i.getNode());
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
         * @return server
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
         * @return user
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
         * @return password
         */
        public String getPassword() {
            return password;
        }

        /**
         * Used for knowing if a job is using the start() method because it is
         * loading or the configuration has changed and it is being reloaded.
         *
         * @return reloading
         */
        public boolean getReloading() {
            return reloading;
        }

        /**
         * Performs on-the-fly validation of the form field 'server'.
         *
         * This method checks if the connection to the XMPP server specified is
         * available. It shows a notification describing the status of the
         * server connection.
         *
         * @param server
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
         * @param user
         * @param password
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
         * Fill the drop-down called "nodesAvailable" of the config.jelly
         *
         * @param name: name of the job
         * @return ListBoxModel: List of nodes available in the XMPP Server
         * @throws XMPPException
         * @throws InterruptedException
         */
        public ListBoxModel doFillNodeItems(@QueryParameter("name") String name) throws XMPPException, InterruptedException {

            ListBoxModel items = new ListBoxModel();
            ArrayList nodesSubsArray = new ArrayList();
//            String node;
//            String pjName = name;
//            Project pj;
//            pj = (Project) Jenkins.getInstance().getItem(name);
//            Object instance = (ElOyente) pj.getTriggers().get(this);

            items.add("Node1");
            items.add("Node2");
            items.add("Node3");
            return items;
//
//            System.out.println("pjName:" + pjName);
//
//            //System.out.println("nombre: " + Stapler.getCurrentResponse().getCurrentRequest().getParameter("com-technicolor-eloyente-ElOyente"));
//            System.out.println("nombre: " + Stapler.getCurrentRequest().getAttribute("com-technicolor-eloyente-ElOyente"));
//
//
//            if (instance != null) {
//
//               Connection con = connections.get(pjName);
//                PubSubManager mgr = new PubSubManager(con);
//
//                DiscoverItems it = mgr.discoverNodes(null);
//                Iterator<DiscoverItems.Item> iter = it.getItems();
//
//                HashMap<String, Subscription> prueba = new HashMap<String, Subscription>();
//                List<Subscription> listSubs = mgr.getSubscriptions();
//
//                for (int i = 0; i < listSubs.size(); i++) {
//                    if (listSubs.get(i).getJid().equals(con.getUser())) {
//                        System.out.println("User: " + listSubs.get(i).getJid() + " es igual a: " + con.getUser());
//                        System.out.println("Suscrito a: " + listSubs.get(i).getNode());
//                        node = listSubs.get(i).getNode();
//                        nodesSubsArray.add(node);
//                    }
//                }
//
//                if (con.isAuthenticated()) {
//                    while (iter.hasNext()) {
//                        DiscoverItems.Item i = iter.next();
//                        if (!nodesSubsArray.contains(i.getNode())) {
//                            items.add(i.getNode());
//                            System.out.println("Node shown: " + i.getNode());
//                        } else {
//                            System.out.println("Node no shown: " + i.getNode());
//                        }
//                    }
//                    //con.disconnect();
//                } else {
//                    items.add("No esta conectado el amigo");
//                    System.out.println("No Logeado");
//                }
//            }

        }

        public ListBoxModel doFillNodesSubItems() throws XMPPException, InterruptedException {
            ListBoxModel items = new ListBoxModel();
////            String node;
////            String pj;
////
////            if (semaforo == true) {
////                wait();
////            } else {
////                semaforo = true;
////                pj = ElOyente.DescriptorImpl.getCurNodoEstarentDescriptorByNameUrl();
////                System.out.println("pj " + pj);
////                Connection con = connections.get(pj);
////                //PubSubManager mgr=getPubSubManageNodoEstar();
////                PubSubManager mgr = new PubSubManager(con);
////
////                List<Subscription> listSubs = mgr.getSubscriptions();
////
////                for (int i = 0; i < listSubs.size(); i++) {
////                    if (listSubs.get(i).getJid().equals(con.getUser())) {
////                        node = listSubs.get(i).getNode();
////                        items.add(node);
////                    }
////                }
////                 //con.disconnect();
////                semaforo = false;
////            }

            return items;
        }
        /**
         * In charge of subscribing the job to a node.
         *
         * It subscribes the job being configured to the node selected, and also
         * creates the listeners required for triggering jobs when XMPP events
         * are received.
         *
         * @param nodesAvailable
         * @param name
         * @return
         */
//        public FormValidation doSubscribe(@QueryParameter("nodesAvailable") String nodesAvailable) {
//            String projectName = Stapler.getCurrentRequest().findAncestorObject(AbstractProject.class).getName();
//            Connection con = connections.get(projectName);
//            PubSubManager mgr = new PubSubManager(con);
//            Trigger trigger = null;
//
//            try {
//                Iterator it2 = (Jenkins.getInstance().getItems()).iterator();
//                while (it2.hasNext()) {
//                    AbstractProject job = (AbstractProject) it2.next();
//                    trigger = (ElOyente) job.getTriggers().get(this);
//                }
//                LeafNode node = (LeafNode) mgr.getNode(nodesAvailable);
//                ItemEventCoordinator itemEventCoordinator = new ItemEventCoordinator(nodesAvailable, trigger);
//                node.addItemEventListener(itemEventCoordinator);
//                String JID = con.getUser();
//                node.subscribe(JID);
//                return FormValidation.ok(con.getUser() + " Subscribed to " + nodesAvailable + " with resource " + projectName);
//            } catch (Exception e) {
//                return FormValidation.error("Couldn't subscribe to " + nodesAvailable);
//            }
//        }
    }
}
