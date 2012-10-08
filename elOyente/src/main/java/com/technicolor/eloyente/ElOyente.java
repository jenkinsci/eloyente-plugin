package com.technicolor.eloyente;

import hudson.Extension;
import hudson.model.Item;
import hudson.model.Project;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import java.util.Iterator;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 *
 * @author pardogonzalezj
 */
public class ElOyente extends Trigger<Project> {

    private final boolean activeJob;
    private static String server;
    private static String user;
    private static String password;
    private static Item project;

    @DataBoundConstructor
    public ElOyente(String server, String user, String password, boolean activeJob) {
        ElOyente.server = server;
        ElOyente.user = user;
        ElOyente.password = password;
        this.activeJob = activeJob;

    }

    public boolean getActiveJob() {
        return activeJob;
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
    public void start(Project project, boolean newInstance) {
        System.out.println("El principio de start");

        this.project = project;

        super.start(project, newInstance);


    }

    @Override
    public void run() {
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

    @Extension
    public static final class DescriptorImpl extends TriggerDescriptor {

        @Override
        public boolean isApplicable(Item item) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Trigger by socket event";
        }
    }
}
