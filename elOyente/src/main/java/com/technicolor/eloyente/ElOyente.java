package com.technicolor.eloyente;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.model.Project;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import hudson.util.FormValidation;
import java.util.Iterator;
import net.sf.json.JSONObject;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * @author Juan Luis Pardo Gonzalez
 * @author Isabel Fernandez Diaz
 */
public class ElOyente extends Trigger<Project> {

    private final boolean activeJob;
    private String server;
    private String user;
    private String password;
    private static Item project;
 
    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public ElOyente(boolean activeJob) {
        super();
        this.activeJob = activeJob;
        server = this.getDescriptor().server;
        user = this.getDescriptor().user;
        password = this.getDescriptor().password;
    }

    /**
     * We'll use this from the <tt>config.jelly</tt>.
     */
    public boolean getActiveJob() {
        return activeJob;
    }

   
    
    @Override
    public void start(Project project, boolean newInstance) {

        //super.start(project, newInstance);
        //if (this.getDescriptor().doCheckActiveJob(activeJob).kind.toString().equals("OK")) {
        if (server != null && !server.isEmpty() && user != null && !user.isEmpty() && password != null && !password.isEmpty()) {
//            if (newInstance) {
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
//            } else {
//                this.getDescriptor().save();
//            }
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
        //super.run(); TODO: intentar si funciona activandolo !!!!!!!        
    }

    @Override
    public final DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    /**
     * Descriptor for {@link HelloWorldBuilder}. Used as a singleton. The class
     * is marked as public so that it can be accessed from views.
     *
     * <p> See
     * <tt>src/main/resources/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
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

        public DescriptorImpl() {
            load();
        }

        /**
         * Performs on-the-fly validation of the form field 'name'.
         *
         * @param value This parameter receives the value that the user has
         * typed.
         * @return Indicates the outcome of the validation. This is sent to the
         * browser.
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

        public FormValidation doCheckActiveJob(@QueryParameter boolean activeJob) {
            server = this.getServer();
            user = this.getUser();
            password = this.getPassword();

            if (server == null || server.isEmpty() || user == null || user.isEmpty() || password == null || password.isEmpty()) {
                return FormValidation.errorWithMarkup("No information about the server in the main configuration");
            }
            return FormValidation.okWithMarkup("Server: " + server);
        }

        @Override
        public boolean isApplicable(Item item) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "XMPP triggered plugin";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws Descriptor.FormException {
            // To persist global configuration information,
            // set that to properties and call save().
            server = formData.getString("server");
            user = formData.getString("user");
            password = formData.getString("password");
            // ^Can also use req.bindJSON(this, formData);
            //  (easier when there are many fields; need set* methods for this, like setUseFrench)

            save();
            return super.configure(req, formData);
        }

        @Override
        public synchronized void save() {
            super.save();
        }

        /**
         * This method returns true if the global configuration says we should
         * speak French.
         *
         * The method name is bit awkward because global.jelly calls this method
         * to determine the initial state of the checkbox by the naming
         * convention.
         */
        public String getServer() {
            return server;
        }

        public String getUser() {
            return user;
        }

        public String getPassword() {
            return password;
        }
    }
}