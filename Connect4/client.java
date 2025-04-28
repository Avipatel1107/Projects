import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class client {
    public static void main(String[] args) {
        try {
            Socket serverSocket1 = new Socket("localhost", 8888);
            System.out.println("Successful");

            BufferedReader input1 = new BufferedReader(new InputStreamReader(serverSocket1.getInputStream()));
            PrintWriter output1 = new PrintWriter(serverSocket1.getOutputStream(), true);

            String serverMessage1 = input1.readLine();
            System.out.println("Connected: " + serverMessage1);

            input1.close();
            output1.close();
            serverSocket1.close();

            Socket serverSocket2 = new Socket("localhost", 8888);
            System.out.println("Successful");

            BufferedReader input2 = new BufferedReader(new InputStreamReader(serverSocket2.getInputStream()));
            PrintWriter output2 = new PrintWriter(serverSocket2.getOutputStream(), true);

            String serverMessage2 = input2.readLine();
            System.out.println("Connected: " + serverMessage2);

            Connect4 gameBoard = new Connect4(6,7);
            input2.close();
            output2.close();
            serverSocket2.close();


        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
