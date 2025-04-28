import java.io.*;
import java.net.*;

public class server {
    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(8888);
            System.out.println("Server started. Waiting for Players to Connect...");

            int totPlayer = 1;

            while (true) {
                // Accept a client connection
                Socket clientSocket = serverSocket.accept();
                System.out.println("Player " + totPlayer + " connected.");

                // Create a new thread to handle the client connection
                Thread clientThread = new Thread(new ClientHandler(clientSocket, totPlayer));
                clientThread.start();

                totPlayer++;

                if (totPlayer > 2) {
                    break; // Stop accepting more connections after two players
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket cSocket;
        private int player;

        public ClientHandler(Socket cSocket, int player) {
            this.cSocket = cSocket;
            this.player = player;
        }

        @Override
        public void run() {
            try {
                BufferedReader input = new BufferedReader(new InputStreamReader(cSocket.getInputStream()));
                PrintWriter output = new PrintWriter(cSocket.getOutputStream(), true);

                output.println("Connect 4 Game, You are Player " + player + ".");


                input.close();
                output.close();
                cSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
