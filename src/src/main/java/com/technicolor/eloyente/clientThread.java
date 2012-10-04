package com.technicolor.eloyente;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;

class clientThread extends Thread {

    private DataInputStream is = null;
    private PrintStream os = null;
    private java.net.Socket clientSocket = null;

    public clientThread(ServerSocket echoServer) throws IOException {
        this.clientSocket = echoServer.accept();
    }

    @Override
    public void run() {

        try {

            while (true) {
                is = new DataInputStream(clientSocket.getInputStream());
                os = new PrintStream(clientSocket.getOutputStream());
                os.println("\nDime algo");
                String algo = is.readLine().trim();
                System.out.println("Dice que: " + algo);
                os.println("Dices que: " + algo);
            }
        } catch (IOException e) {
        }
    }
}