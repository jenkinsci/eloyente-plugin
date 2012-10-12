package com.technicolor.eloyente;

import hudson.Extension;
import hudson.model.Item;
import hudson.model.Project;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import java.util.Iterator;
import net.sf.json.JSONObject;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import java.util.logging.*;

/**
 *
 * @author pardogonzalezj
 */
public class ElOyente extends Trigger<Project> {

    private final boolean activeJob;
    private static Item project;
//    private static Logger logger;

    @DataBoundConstructor
    public ElOyente(boolean activeJob) {
        this.activeJob = activeJob;
    }

    public boolean getActiveJob() {
        return activeJob;
    }

    @Override
    public void start(Project project, boolean newInstance) {
        System.out.println("Class: "+this.getClass().getName());
        Logger logger = Logger.getLogger("com.technicolor.eloyente");
        System.out.println("Logger: "+logger);

        System.out.println("El principio de start");
        logger.log(Level.ALL,       "ElOyento start - ALL");
        logger.log(Level.CONFIG,    "ElOyento - CONFIG");
        logger.log(Level.FINE,      "ElOyento - FINE");
        logger.log(Level.FINER,     "ElOyento - FINER");
        logger.log(Level.FINEST,    "ElOyento - FINEST");
        logger.log(Level.INFO,      "ElOyento - INFO");
        logger.log(Level.OFF,       "ElOyento - OFF");
        logger.log(Level.SEVERE,    "ElOyento - SEVERE");
        logger.log(Level.WARNING,   "ElOyento - WARNING");

        this.project = project;

        super.start(project, newInstance);

    }

    @Override
    public void run() {
        System.out.println("El principio de run");
//        logger.log(Level.ALL,       "ElOyento run - ALL");
//        logger.log(Level.CONFIG,    "ElOyento - CONFIG");
//        logger.log(Level.FINE,      "ElOyento - FINE");
//        logger.log(Level.FINER,     "ElOyento - FINER");
//        logger.log(Level.FINEST,    "ElOyento - FINEST");
//        logger.log(Level.INFO,      "ElOyento - INFO");
//        logger.log(Level.OFF,       "ElOyento - OFF");
//        logger.log(Level.SEVERE,    "ElOyento - SEVERE");
//        logger.log(Level.WARNING,   "ElOyento - WARNING");
        super.run();

        Iterator iterator = project.getAllJobs().iterator();

        while (iterator.hasNext()) {
            System.out.println(iterator.next());
            job.scheduleBuild(null);
        }
    }

    @Override
    public void stop() {
        super.stop();
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends TriggerDescriptor {

        private String server;
        private String user;
        private String password;
        private Connection con;

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

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
            Logger logger = Logger.getLogger("com.technicolor.eloyente");
            server = formData.getString("server");
            user = formData.getString("user");
            password = formData.getString("password");
            System.out.println("Servidor: " + server + " Usuario: " + user + " Password: " + password);

            System.out.println("Conneting to " + server);
            ConnectionConfiguration config = new ConnectionConfiguration(server);
System.out.println("CHECKPOINT 1");

            con = new XMPPConnection(config);
System.out.println("CHECKPOINT 2");
            logger.info("Before connecting");
//            logger.log(Level.INFO, "Before connecting");
System.out.println("CHECKPOINT 3");
            try {
System.out.println("CHECKPOINT 4");
                con.connect();
System.out.println("CHECKPOINT 5");
            } catch (XMPPException ex) {
                System.err.println("Couldn't stablish the connection, or already connected");
            }
System.out.println("CHECKPOINT 6");
            logger.log(Level.INFO, "After connecting");
System.out.println("CHECKPOINT 7");
            System.out.println("Login as " + user);
//            try {
//                con.login(user, password);
//                System.err.println("ESTE ES EL MESAJITO__________________________________________________");
//            } catch (XMPPException ex) {
//                System.err.println("User or password doesn't exist");
//            }

            // ^Can also use req.bindJSON(this, formData);
            //  (easier when there are many fields; need set* methods for this, like setUseFrench)
            save();

            return super.configure(req, formData);
        }
    }
}
