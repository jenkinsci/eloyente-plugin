package com.technicolor;

import hudson.triggers.Trigger;
import java.io.IOException;
import java.net.ServerSocket;

public class Socket extends Thread {

    private ServerSocket echoServer = null;
    private Trigger trigger = null;

    public Socket(Trigger tr) {
        try {
            trigger = tr;
            echoServer = new ServerSocket(2222);
            this.start();
        } catch (IOException e) {
            System.out.println("Exception in constructor: " + e);
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                System.out.println("Waiting for a connection to port 2222");
                echoServer.accept();
                System.out.println("Connection established");
                trigger.run();
            }
        } catch (IOException e) {
            System.out.println("Exception in run loop: " + e);
        }
    }

}
