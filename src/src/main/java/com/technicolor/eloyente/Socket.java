package com.technicolor.eloyente;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Socket {

    static ServerSocket echoServer = null;
    static clientThread clientSocket = null;

    public Socket() {
        try {
            echoServer = new ServerSocket(2222);
            echoServer.setSoTimeout(1);
        } catch (IOException e) {
            System.out.println(e);
        }

        System.out.println("The server started in port 2222. To stop it press <CTRL><C>.");

        while (true) {
            try {
                clientSocket = new clientThread(echoServer);
            } catch (IOException ex) {
                Logger.getLogger(Socket.class.getName()).log(Level.SEVERE, null, ex);
            }
            clientSocket.start();
        }
    }

    public void closeSocket() throws IOException{
          echoServer.close();
    }
}
