package com.technicolor.eloyente;

import hudson.Extension;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.Project;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.inject.New;
import net.sf.json.JSONObject;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.packet.DiscoverItems;
import org.jivesoftware.smackx.pubsub.PubSubManager;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 *
 * @author pardogonzalezj
 * @author fernandezdiazi
 */
public class ElOyente extends Trigger<Project> {

    private final boolean activeJob;
    private static Item project;
//    private static Logger logger;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"    
    @DataBoundConstructor
    public ElOyente(boolean activeJob) {
        super();
        this.activeJob = activeJob;
    }

    /**
     * We'll use this from the <tt>config.jelly</tt>.
     */
    public boolean getActiveJob() {
        return activeJob;
    }

    @Override
    public void start(Project project, boolean newInstance) {
        System.out.println("Class: " + this.getClass().getName());
        Logger logger = Logger.getLogger("com.technicolor.eloyente");
        System.out.println("Logger: " + logger);

        this.project = project;
        super.start(project, newInstance);

    }

    @Override
    public void run() {
        System.out.println("El principio de run");

        if (!project.getAllJobs().isEmpty()) {
            Iterator iterator = project.getAllJobs().iterator();

            while (iterator.hasNext()) {
                //System.out.println(iterator.next());
                ((Project) iterator.next()).scheduleBuild(null);
                //job.scheduleBuild(null);
            }
        }
        //super.run(); TODO: intentar si funciona activandolo !!!!!!!        
    }

    @Override
    public void stop() {
        super.stop();
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends TriggerDescriptor {

        private String server;
        private String user;
        private String password;
        private Connection con;
        private PubSubManager mgr;
        private String jid;
        Logger logger = Logger.getLogger("com.technicolor.eloyente");

        public DescriptorImpl() {
            super(ElOyente.class);
            load();
        }

        @Override
        public boolean isApplicable(Item item) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Trigger jobs by XMPP";
        }

        public String getServer() {
            return server;
        }

        public String getUser() {
            return user;
        }

        public String getPassword() {
            return password;
        }

        public void setServer(String server) {
            this.server = server;
        }

        public void setUser(String user) {
            this.user = user;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().

            server = req.getParameter("server");
            user = req.getParameter("user");
            password = req.getParameter("password");

            System.out.println("Servidor: " + server + " Usuario: " + user + " Password: " + password);
            System.out.println("Conneting to " + server);

            ConnectionConfiguration config = new ConnectionConfiguration(server);
            con = new XMPPConnection(config);

            try {
                con.connect();
                logger.log(Level.INFO, "Connection established");
                System.out.println("Loging...");
                try {
                    con.login(user, password);
                    jid = con.getUser();
                    logger.log(Level.INFO, "JID: {0}", jid);
                    logger.log(Level.INFO, "{0} has been logged to openfire!", user);
                    System.out.println(user + " logged!");
                    save();

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
                    } catch (XMPPException ex) {
                        System.out.println("List of nodes no available");
                        ex.printStackTrace(System.err);
                    }

                } catch (XMPPException ex) {
                    System.err.println("User or password doesn't exist");
                    ex.printStackTrace(System.err);
                }
            } catch (XMPPException ex) {
                System.err.println("Couldn't stablish the connection, or already connected");
                ex.printStackTrace(System.err);
            }

            return super.configure(req, formData);
        }
    }
}
