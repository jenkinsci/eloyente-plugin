package com.technicolor.eloyente;

import hudson.Extension;
import hudson.Util;
import hudson.model.Item;
import hudson.model.Project;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.json.JSONObject;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;



/**
 *
 * @author pardogonzalezj
 */
public class ElOyente extends Trigger<Project> {

    private final boolean activeJob;
    private static Item project;
    private final static Logger logger = Logger.getLogger(ElOyente.class .getName()); 

    @DataBoundConstructor
    public ElOyente(boolean activeJob) {
        this.activeJob = activeJob;

    }

    public boolean getActiveJob() {
           return activeJob;
    }

    @Override
    public void start(Project project, boolean newInstance) {
        System.out.println("El principio de start");

        this.project = project;

        super.start(project, newInstance);

    }

    @Override
    public void run() {
        System.out.println("El principio de run");
        super.run();

        System.out.println("El principio de run");
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
            server = formData.getString("server");
            user = formData.getString("user");
            password = formData.getString("password");
            System.out.println("Servidor: " + server + " Usuario: " + user + " Password: " + password);
            // ^Can also use req.bindJSON(this, formData);
            //  (easier when there are many fields; need set* methods for this, like setUseFrench)
            save();

            System.out.println("Conneting to " + server);
            ConnectionConfiguration config = new ConnectionConfiguration(server);
            con = new XMPPConnection(config);
            try {
                con.connect();
            } catch (XMPPException ex) {
                System.err.println("Couldn't stablish the connection, or already connected");
            }
            System.out.println("Login as " + user);
            try {
                con.login(user, password);
                 logger.log(Level.OFF, "MENSAJES PARA DIOS");         
              
            } catch (XMPPException ex) {
                System.err.println("User or password doesn't exist");
            }
            return super.configure(req, formData);
        }
    }
}
