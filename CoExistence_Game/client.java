import java.io.*;
import java.net.*;
import java.util.*;

public class client {
    private static final int DEFAULT_PORT_NUMBER = 35754;
    private static final String DEFAULT_SERVER_IP = "localhost";

    public static void main(String[] args) {
        String serverIP = DEFAULT_SERVER_IP;
        int port = DEFAULT_PORT_NUMBER;

        if (args.length >= 2) {
            serverIP = args[0];
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port number. Using " + DEFAULT_PORT_NUMBER);
            }
        } else if (args.length == 1) {
            serverIP = args[0];
            System.out.println("No port specified. Using " + DEFAULT_PORT_NUMBER);
        } else {
            System.out.println("No IP or port specified. Using localhost:" + DEFAULT_PORT_NUMBER);
        }

        new client(serverIP, port);
    }

    private Socket gameSocket;
    private BufferedReader inputReader;
    private PrintWriter outputWriter;
    private BufferedReader consoleInputReader;
    private int currentPlayerNumber;
    private boolean isGameActive = true;

    // Game state
    private String[][] gameBoard = new String[6][6];
    private int playerScore = 0;
    private int opponentPlayerScore = 0;
    private int currentRoundNumber = 1;
    private boolean isCurrentPlayerTurn = false;
    private String currentGameMessage = "NEW GAME";

    public client(String serverIP, int port) {
        try {
            // Initialize the board with empty spaces
            for (int i = 0; i < gameBoard.length; i++) {
                for (int j = 0; j < gameBoard[i].length; j++) {
                    gameBoard[i][j] = "";
                }
            }

            // Connect to server
            System.out.println("Trying to connect to server... ");
            gameSocket = new Socket(serverIP, port);
            System.out.println("Connected to server at " + serverIP + ":" + port);

            // Setup input/output streams
            inputReader = new BufferedReader(new InputStreamReader(gameSocket.getInputStream()));
            outputWriter = new PrintWriter(gameSocket.getOutputStream(), true);
            consoleInputReader = new BufferedReader(new InputStreamReader(System.in));

            // Start game thread
            startGame();

        } catch (IOException e) {
            System.err.println("Error connecting to server: " + e.getMessage());
            System.exit(1);
        }
    }

    private void startGame() {
        try {
            // Get player number
            String line = inputReader.readLine();
            if (line.startsWith("PLAYER:")) {
                currentPlayerNumber = Integer.parseInt(line.substring(7));
                System.out.println("You are player " + currentPlayerNumber);
                outputWriter.println("READY");
            }

            // Start listening for server messages in a separate thread
            Thread serverListener = new Thread(this::listenForServerMessages);
            serverListener.start();

            // Start user input thread
            Thread userInputThread = new Thread(this::handleUserInput);
            userInputThread.start();

            // Wait for game to end
            serverListener.join();

        } catch (Exception e) {
            System.err.println("Game error: " + e.getMessage());
        } finally {
            try {
                gameSocket.close();
            } catch (IOException e) {
                // Ignore close errors
            }
            System.exit(0);
        }
    }

    private void listenForServerMessages() {
        try {
            String line;
            while (isGameActive() && (line = inputReader.readLine()) != null) {
                if (line.startsWith("STATE:")) {
                    parseGameState(line.substring(6));
                    printGameState();
                }
            }
        } catch (IOException e) {
            if (isGameActive()) {
                System.err.println("Connection to server lost: " + e.getMessage());
            }
        }
    }

    private void parseGameState(String state) {
        Map<String, String> stateMap = new HashMap<>();
        for (String pair : state.split(";")) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                stateMap.put(keyValue[0], keyValue[1]);
            }
        }

        // Update game state
        String message = stateMap.get("message");
        if (message != null && !message.isEmpty()) {
            currentGameMessage = message;
        }

        currentRoundNumber = Integer.parseInt(stateMap.get("roundNumber"));
        boolean isGameOver = Boolean.parseBoolean(stateMap.get("isGameOver"));
        if (isGameOver) {
            setGameActive(false);
            currentGameMessage = "GAME OVER";
        }

        playerScore = Integer.parseInt(stateMap.get("score"));
        opponentPlayerScore = Integer.parseInt(stateMap.get("opponentScore"));

        // Update hands
        String[] myHandCards = stateMap.get("hand").split(",");
        String[] opponentHandCards = stateMap.get("opponentHand").split(",");

        // Clear the board
        for (int i = 0; i < gameBoard.length; i++) {
            for (int j = 0; j < gameBoard[i].length; j++) {
                gameBoard[i][j] = "";
            }
        }

        // Update the board with cards
        for (int i = 0; i < myHandCards.length; i++) {
            if (!myHandCards[i].isEmpty()) {
                gameBoard[1][i] = myHandCards[i];
            }
        }

        for (int i = 0; i < opponentHandCards.length; i++) {
            if (!opponentHandCards[i].isEmpty()) {
                gameBoard[0][i] = opponentHandCards[i];
            }
        }

        // Update turn status
        int currentPlayer = Integer.parseInt(stateMap.get("currentPlayer"));
        isCurrentPlayerTurn = (currentPlayerNumber == 1 && currentPlayer == 0)
                || (currentPlayerNumber == 2 && currentPlayer == 1);
    }

    private void printGameState() {
        System.out.println("\\" + repeatString("-", 38) + "/");

        // Print column headers for top row - perfectly centered over each card
        System.out.print("  ");
        for (int i = 0; i < 6; i++) {
            System.out.print((char) ('A' + i) + "    ");
        }
        System.out.println();

        // Top border for opponent cards
        System.out.print("/");
        for (int i = 0; i < 6; i++) {
            System.out.print("---\\");
            if (i < 5)
                System.out.print("/");
        }
        System.out.println("");

        // First line inside opponent cards
        System.out.print("|");
        for (int j = 0; j < 6; j++) {
            String card = gameBoard[0][j];
            String[] cardDesign = getCardDesign(card);
            System.out.print(cardDesign[0] + "|");
            if (j < 5)
                System.out.print("|");
        }
        System.out.println("      " + (!isCurrentPlayerTurn ? "^" : "|"));

        // Second line inside opponent cards
        System.out.print("|");
        for (int j = 0; j < 6; j++) {
            String card = gameBoard[0][j];
            String[] cardDesign = getCardDesign(card);
            System.out.print(cardDesign[1] + "|");
            if (j < 5)
                System.out.print("|");
        }
        System.out.println("      " + (!isCurrentPlayerTurn ? "|" : "|"));

        // Third line inside opponent cards
        System.out.print("|");
        for (int j = 0; j < 6; j++) {
            String card = gameBoard[0][j];
            String[] cardDesign = getCardDesign(card);
            System.out.print(cardDesign[2] + "|");
            if (j < 5)
                System.out.print("|");
        }
        System.out.println("      " + (!isCurrentPlayerTurn ? "|" : "v"));

        // Bottom border of opponent cards
        System.out.print("\\");
        for (int i = 0; i < 6; i++) {
            System.out.print("---/");
            if (i < 5)
                System.out.print("\\");
        }
        System.out.println("");

        // Score and round info section
        // Display opponent score directly above R
        System.out.println("                                     [" + opponentPlayerScore + "]");

        // Separator between opponent and player cards with round info
        System.out.print("<");
        for (int i = 0; i < 35; i++) {
            System.out.print("=");
        }
        System.out.println(">R" + currentRoundNumber + "<");

        // Display player score directly below R
        System.out.println("                                     [" + playerScore + "]");

        // Top border for player cards
        System.out.print("/");
        for (int i = 0; i < 6; i++) {
            System.out.print("---\\");
            if (i < 5)
                System.out.print("/");
        }
        System.out.println("");

        // First line inside player cards
        System.out.print("|");
        for (int j = 0; j < 6; j++) {
            String card = gameBoard[1][j];
            String[] cardDesign = getCardDesign(card);
            System.out.print(cardDesign[0] + "|");
            if (j < 5)
                System.out.print("|");
        }
        System.out.println("      " + (isCurrentPlayerTurn ? "|" : "^"));

        // Second line inside player cards
        System.out.print("|");
        for (int j = 0; j < 6; j++) {
            String card = gameBoard[1][j];
            String[] cardDesign = getCardDesign(card);
            System.out.print(cardDesign[1] + "|");
            if (j < 5)
                System.out.print("|");
        }
        System.out.println("      " + (isCurrentPlayerTurn ? "|" : "|"));

        // Third line inside player cards
        System.out.print("|");
        for (int j = 0; j < 6; j++) {
            String card = gameBoard[1][j];
            String[] cardDesign = getCardDesign(card);
            System.out.print(cardDesign[2] + "|");
            if (j < 5)
                System.out.print("|");
        }
        System.out.println("      " + (isCurrentPlayerTurn ? "v" : "|"));

        // Bottom border of player cards
        System.out.print("\\");
        for (int i = 0; i < 6; i++) {
            System.out.print("---/");
            if (i < 5)
                System.out.print("\\");
        }
        System.out.println("");

        // Print column headers for bottom row - perfectly centered over each card
        System.out.print("  ");
        for (int i = 0; i < 6; i++) {
            System.out.print((char) ('A' + i) + "    ");
        }
        System.out.println();

        // Message log area
        System.out.println("|" + repeatString("-", 38) + "|");

        // Display the current game message
        System.out.println("|" + String.format("%-38s", currentGameMessage) + "|");
        System.out.println("\\" + repeatString("-", 38) + "/");

        // Highlight which player's turn it is
        if (isCurrentPlayerTurn) {
            System.out.println("Your turn! Enter your move (e.g., 'AB' to attack from A to B, or 'PS' to pass or 'Q' to quit)");
        } else {
            System.out.println("Waiting for opponent move...");
        }
    }

    private String[] getCardDesign(String card) {
        String[] cardType = new String[3];

        if (card.isEmpty()) {
            cardType[0] = "   ";
            cardType[1] = "   ";
            cardType[2] = "   ";
        } else if (card.equals("Axe")) {
            // Design for Axe - double-headed axe
            cardType[0] = "[=]";
            cardType[1] = " | ";
            cardType[2] = " | ";
        } else if (card.equals("Arrow")) {
            // Design for Arrow - arrow pointing upward
            cardType[0] = " ^ ";
            cardType[1] = " | ";
            cardType[2] = "/^\\";
        } else if (card.equals("Sword")) {
            // Design for Sword - diagonal blade with crossguard
            cardType[0] = "  /";
            cardType[1] = " / ";
            cardType[2] = "X  ";
        } else if (card.equals("Hammer")) {
            // Design for Hammer - hammer with handle
            cardType[0] = "<7>";
            cardType[1] = " I ";
            cardType[2] = " L ";
        } else {
            // Unknown card
            cardType[0] = " ? ";
            cardType[1] = " ? ";
            cardType[2] = " ? ";
        }

        return cardType;
    }

    private void handleUserInput() {
        try {
            while (isGameActive()) {
                String input = consoleInputReader.readLine();
                if (input == null)
                    break;

                input = input.toUpperCase().trim();

                if (!isCurrentPlayerTurn) {
                    currentGameMessage = "Not your turn yet!";
                    printGameState();
                    continue;
                }

                if (input.equals("Q") || input.equals("QUIT")) {
                    currentGameMessage = "You have quit the game.";
                    printGameState();
                    setGameActive(false);
                    gameSocket.close();
                    break;
                }

                if (input.equals("PS") || isValidMoveFormat(input)) {
                    if (input.equals("PS")) {
                        currentGameMessage = "You passed your turn.";
                    } else {
                        currentGameMessage = "Move: " + input.charAt(0) + " → " + input.charAt(1);
                    }
                    outputWriter.println(input);
                } else {
                    currentGameMessage = "Invalid move format. Try 'AB' or 'PS' or 'Q' to quit";
                    printGameState();
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading input: " + e.getMessage());
        }
    }

    private boolean isValidMoveFormat(String move) {
        return move.length() == 2 &&
                move.charAt(0) >= 'A' && move.charAt(0) <= 'F' &&
                move.charAt(1) >= 'A' && move.charAt(1) <= 'F';
    }

    private synchronized void setGameActive(boolean active) {
        isGameActive = active;
    }

    private synchronized boolean isGameActive() {
        return isGameActive;
    }

    // To repeat a string, mainly used for creating the borders for terminal
    private String repeatString(String str, int times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
}