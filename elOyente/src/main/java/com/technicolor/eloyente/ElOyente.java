package com.technicolor.eloyente;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.model.Project;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import hudson.util.FormValidation;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.json.JSONObject;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.packet.DiscoverItems;
import org.jivesoftware.smackx.pubsub.PubSubManager;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * @author Juan Luis Pardo Gonzalez
 * @author Isabel Fernandez Diaz
 */
public class ElOyente extends Trigger<Project> {

    private String server;
    private String user;
    private String password;
    private static Item project;

    /**
     *Constructor for elOyente.
     * 
     * It uses the Descriptor to set the fields required later on. The Descriptor will bring the
     * information set in the main configuration to the particular job.
     */
    @DataBoundConstructor
    public ElOyente(Item project) {
        super();
        server = this.getDescriptor().server;
        user = this.getDescriptor().user;
        password = this.getDescriptor().password;
        ElOyente.project = project;
    }

    /**
     * Method used for starting a job.
     * 
     * This method is called when the Save button is pressed in a job configuration in case this
     * plugin is activated.
     * 
     * It checks if there is all the information required for an XMPP connection in the main configuration
     * and creates the connection.
     * 
     * @param Project
     * @param boolean
     */
    @Override
    public void start(Project project, boolean newInstance) {

        //if (this.getDescriptor().doCheckActiveJob(activeJob).kind.toString().equals("OK")) {
        if (server != null && !server.isEmpty() && user != null && !user.isEmpty() && password != null && !password.isEmpty()) {
            try {
                ConnectionConfiguration config = new ConnectionConfiguration(server);
                Connection con = new XMPPConnection(config);
                con.connect();
                if (con.isConnected()) {
                    try {
                        con.login(user, password, project.getName());
                    } catch (XMPPException ex) {
                        System.err.println("Login error");
                        ex.printStackTrace(System.err);
                    }
                }
            } catch (XMPPException ex) {
                System.err.println("Couldn't establish the connection, or already connected");
                ex.printStackTrace(System.err);
            }
        }
    }

    @Override
    public void run() {
        if (!project.getAllJobs().isEmpty()) {
            Iterator iterator = project.getAllJobs().iterator();
            while (iterator.hasNext()) {
                ((Project) iterator.next()).scheduleBuild(null);
            }
        }
        //super.run();      
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
     * Descriptor for {@link elOyente}. Used as a singleton. The class is marked
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
         * Used to persist the global configuration.
         *
         *
         * This method is used to set the field for the checkbox that activates
         * the plugin in the job configuration.
         *
         * @return boolean
         * @param Item
         */
        @Override
        public boolean isApplicable(Item item) {
            return true;
        }

        /**
         * Used to persist the global configuration.
         *
         *
         * This method is used to set the field for the checkbox that activates
         * the plugin in the job configuration.
         *
         * @return String
         */
        @Override
        public String getDisplayName() {
            return "XMPP triggered plugin";
        }

        /**
         * Used to persist the global configuration.
         *
         *
         * global.jelly calls this method to obtain the value of field server.
         *
         * @param req
         * @param formData
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
            // ^Can also use req.bindJSON(this, formData);
            //  (easier when there are many fields; need set* methods for this)

            report();

            save();
            return super.configure(req, formData);
        }

        /**
         * Used for logging to the log file.
         *
         * This method reports information related to XMPP events like
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
                    ConnectionConfiguration config = new ConnectionConfiguration(server);
                    PubSubManager mgr;
                    Connection con = new XMPPConnection(config);
                    con.connect();
                    logger.log(Level.INFO, "Connection established");
                    if (con.isConnected()) {
                        try {
                            con.login(user, password, "Global");
                            logger.log(Level.INFO, "JID: {0}", con.getUser());
                            logger.log(Level.INFO, "{0} has been logged to openfire!", user);

                            try {
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

                                con.disconnect();

                            } catch (XMPPException ex) {
                                System.out.println("List of nodes no available");
                                ex.printStackTrace(System.err);
                            }

                        } catch (XMPPException ex) {
                            System.err.println("Login error");
                            ex.printStackTrace(System.err);
                        }
                    }
                } catch (XMPPException ex) {
                    System.err.println("Couldn't establish the connection, or already connected");
                    ex.printStackTrace(System.err);
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
    }
}
