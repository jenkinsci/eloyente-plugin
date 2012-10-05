package com.technicolor;

import hudson.Extension;
import hudson.model.BuildableItem;
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
public class SocketTrigger extends Trigger<Project> {

    private final boolean activeJob;
    private static Socket socket;
    private static Item project;

    @DataBoundConstructor
    public SocketTrigger(boolean activeJob) {
        this.activeJob = activeJob;
    }

    public String getMyString() {
        return "Hola Milagros!";
    }

    public boolean getActiveJob() {
        return activeJob;
    }

    @Override
    public void start(Project project, boolean newInstance) {
        System.out.println("El principio de start");

        this.project = project;

        super.start(project, newInstance);

        if (this.getActiveJob()) {
            System.out.println("Activado");
            socket = new Socket(this);
            System.out.println("Socket creado");
        } else {
            System.out.println("No activado");
        }
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
//        try {
//            socket.closeSocket();
//        } catch (IOException ex) {
//            System.err.println("No se puede cerrar la conexion");
//        }
        super.stop();
    }

    /**
     *
     */
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
