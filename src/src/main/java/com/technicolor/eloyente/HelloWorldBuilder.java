/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.technicolor.eloyente;

import hudson.model.Item;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 *
 * @author pardogonzalezj
 */
public class HelloWorldBuilder extends Trigger {

    private final boolean activeJob;
    private static Socket socket = null;

    @DataBoundConstructor
    public HelloWorldBuilder(boolean activeJob) {
        this.activeJob = activeJob;
    }

    public String getMyString() {
        return "Hello Milagros!";
    }

    public boolean getActiveJob() {
        return activeJob;
    }

    @Override
    public void start(Item project, boolean newInstance) {
        super.start(project, newInstance);
        if (this.getActiveJob()) {
            System.out.println("Activado");
        } else {
            System.out.println("No activado");
        }
        socket = new Socket();
    }

    @Override
    public void run() {
        super.run();

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

    public static final class DescriptorImpl extends TriggerDescriptor {

        @Override
        public boolean isApplicable(Item item) {
            return true;
        }

        @Override
        public String getDisplayName() {
            System.out.println("Entra en el getDisplayName");
            return "Trigger by socket event";
        }
    }
}
