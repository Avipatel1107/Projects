import java.io.*;
import java.net.*;
import java.util.*;

public class server {
    private static final int DEFAULT_PORT = 35754;

    public static void main(String[] args) {
        String ipAddress = "0.0.0.0";
        int port = DEFAULT_PORT;

        // Use command line arguments if provided
        if (args.length >= 2) {
            ipAddress = args[0];
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port number. Using default port " + DEFAULT_PORT);
                port = DEFAULT_PORT;
            }
        } else if (args.length == 1) {
            ipAddress = args[0];
            System.out.println("No port specified. Using default port " + DEFAULT_PORT);
        } else {
            System.out.println("No IP or port specified. Using 0.0.0.0:" + DEFAULT_PORT);
        }

        System.out.println("Server is running on " + ipAddress + ":" + port);
        try (ServerSocket serverSocket = new ServerSocket(port, 50, InetAddress.getByName(ipAddress))) {
            System.out.println("Waiting for players to connect...");
            while (true) {
                Socket p1 = serverSocket.accept();
                System.out.println("Player 1 connected from " + p1.getInetAddress().getHostAddress());
                Socket p2 = serverSocket.accept();
                System.out.println("Player 2 connected from " + p2.getInetAddress().getHostAddress());
                System.out.println("Starting game thread.");
                new GameThread(p1, p2).start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

class GameThread extends Thread {
    private Socket player1, player2;
    private BufferedReader input1, input2;
    private PrintWriter output1, output2;
    private List<String> hand1, hand2;
    private int score1 = 0, score2 = 0;
    private int currentPlayer;
    private int roundNumber = 1;
    private boolean p1Pass = false;
    private boolean p2Pass = false;
    private long startTime;
    private static final String[] CARD_TYPES = { "Axe", "Hammer", "Sword", "Arrow" };
    private static final int MAX_SCORE = 9;
    private static final int MAX_ROUNDS = 5;
    private static final int HAND_SIZE = 6;

    public GameThread(Socket p1, Socket p2) {
        this.player1 = p1;
        this.player2 = p2;
        this.startTime = System.currentTimeMillis();
        try {
            input1 = new BufferedReader(new InputStreamReader(p1.getInputStream()));
            output1 = new PrintWriter(p1.getOutputStream(), true);
            input2 = new BufferedReader(new InputStreamReader(p2.getInputStream()));
            output2 = new PrintWriter(p2.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            System.out.println("New game started between " + player1.getInetAddress().getHostAddress()
                    + " and " + player2.getInetAddress().getHostAddress());
            System.out.println("Waiting for players to be ready...");
            output1.println("PLAYER:1");
            output2.println("PLAYER:2");

            if (input1.readLine().equals("READY") && input2.readLine().equals("READY")) {
                System.out.println("Both players ready, initializing game");
                initializeGame();
            }

            while (score1 < MAX_SCORE && score2 < MAX_SCORE && roundNumber <= MAX_ROUNDS) {
                BufferedReader activeReader = (currentPlayer == 0) ? input1 : input2;

                String move = activeReader.readLine();
                if (move == null) {
                    sendGameState("Player disconnected");
                    break;
                }

                move = move.toUpperCase();
                if (move.equals("PS")) {
                    handlePass();
                } else if (isValidMoveFormat(move)) {
                    handleAttack(move);
                } else {
                    sendGameState("Invalid move: " + move);
                }

                // Check if game is over after each move
                if (roundNumber > MAX_ROUNDS || score1 >= MAX_SCORE || score2 >= MAX_SCORE) {
                    break;
                }
            }

            gameEnd();
            player1.close();
            player2.close();

        } catch (Exception e) {
            System.err.println("Game error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initializeGame() {
        startNewRound(startTime);
        sendGameState("Game started - Round 1");
    }

    private void startNewRound(long seed) {
        // Create and shuffle a new deck
        List<String> fullDeck = createShuffledDeck(seed);

        // Split the deck between players - only need hands, not reserve
        hand1 = new ArrayList<>(fullDeck.subList(0, HAND_SIZE));
        hand2 = new ArrayList<>(fullDeck.subList(HAND_SIZE, HAND_SIZE * 2));

        // Reset pass flags
        p1Pass = false;
        p2Pass = false;

        // Determine starting player
        if (roundNumber == 1) {
            currentPlayer = Math.abs((int) (seed % 2));
        } else {
            currentPlayer = 1 - currentPlayer;
        }
    }

    private List<String> createShuffledDeck(long seed) {
        List<String> deck = new ArrayList<>();

        // We need 12 cards total (6 for each player)
        // Distribute evenly among the four card types
        int cardsNeeded = HAND_SIZE * 2; // 12 cards
        int cardsPerType = cardsNeeded / CARD_TYPES.length;

        for (String cardType : CARD_TYPES) {
            for (int i = 0; i < cardsPerType; i++) {
                deck.add(cardType);
            }
        }

        // Shuffle the deck
        Collections.shuffle(deck, new Random(seed));
        return deck;
    }

    private void sendGameState(String message) {
        // Log game state to terminal
        System.out.println(" " + message + " | P1: " + score1 + " - P2: " + score2 + " | Round: " + roundNumber);

        boolean isGameOver = score1 >= MAX_SCORE || score2 >= MAX_SCORE || roundNumber > MAX_ROUNDS;

        // Create game state for player 1
        Map<String, Object> map1 = new HashMap<>();
        map1.put("currentPlayer", currentPlayer);
        map1.put("playerNumber", 1);
        map1.put("hand", hand1);
        map1.put("opponentHand", hand2);
        map1.put("score", score1);
        map1.put("opponentScore", score2);
        map1.put("roundNumber", roundNumber);
        map1.put("message", message);
        map1.put("isGameOver", isGameOver);

        // Create game state for player 2
        Map<String, Object> map2 = new HashMap<>();
        map2.put("currentPlayer", currentPlayer);
        map2.put("playerNumber", 2);
        map2.put("hand", hand2);
        map2.put("opponentHand", hand1);
        map2.put("score", score2);
        map2.put("opponentScore", score1);
        map2.put("roundNumber", roundNumber);
        map2.put("message", message);
        map2.put("isGameOver", isGameOver);

        output1.println(initializeGameState(map1));
        output2.println(initializeGameState(map2));
    }

    private String initializeGameState(Map<String, Object> state) {
        StringBuilder sb = new StringBuilder("STATE:");
        for (Map.Entry<String, Object> entry : state.entrySet()) {
            sb.append(entry.getKey()).append("=");
            if (entry.getValue() instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> hand = (List<String>) entry.getValue();
                // Create a list of HAND_SIZE positions, with empty strings for empty positions
                List<String> fullHand = new ArrayList<>(Arrays.asList("", "", "", "", "", ""));
                for (int i = 0; i < hand.size() && i < HAND_SIZE; i++) {
                    String card = hand.get(i);
                    fullHand.set(i, card != null && !card.isEmpty() ? card : "");
                }
                sb.append(String.join(",", fullHand));
            } else {
                sb.append(entry.getValue());
            }
            sb.append(";");
        }
        return sb.toString();
    }

    private boolean isValidMoveFormat(String move) {
        return move.length() == 2 &&
                move.charAt(0) >= 'A' && move.charAt(0) <= 'F' &&
                move.charAt(1) >= 'A' && move.charAt(1) <= 'F';
    }

    private void gameEnd() {
        String endMessage = "Game Over - ";
        if (roundNumber > MAX_ROUNDS) {
            endMessage += (score1 > score2) ? "Player 1 Wins with " + score1 + " points!"
                    : (score2 > score1) ? "Player 2 Wins with " + score2 + " points!"
                            : "It's a Tie! Both players scored " + score1 + " points.";
        } else if (score1 >= MAX_SCORE) {
            endMessage += "Player 1 Wins with " + score1 + " points!";
        } else if (score2 >= MAX_SCORE) {
            endMessage += "Player 2 Wins with " + score2 + " points!";
        }

        System.out.println(" " + endMessage + " | Game session complete.");
        sendGameState(endMessage);
    }

    private void handlePass() {
        String message;
        if (currentPlayer == 0) {
            p1Pass = true;
            message = "Player 1 passed";
            System.out.println(" Player 1 passed their turn");
        } else {
            p2Pass = true;
            message = "Player 2 passed";
            System.out.println(" Player 2 passed their turn");
        }

        if (p1Pass && p2Pass) {
            roundNumber++;
            if (roundNumber <= MAX_ROUNDS) {
                message = "Round " + (roundNumber - 1) + " ended - Both players passed. Starting Round " + roundNumber;
                System.out.println(" Round " + (roundNumber - 1) + " ended - Both players passed");
                System.out.println(" Starting Round " + roundNumber);
                startNewRound(System.currentTimeMillis());
            } else {
                message = "Game Over - Round limit reached. ";
                message += (score1 > score2) ? "Player 1 Wins!" : (score2 > score1) ? "Player 2 Wins!" : "It's a Tie!";
                System.out.println(" Round limit reached. Game ending.");
            }
        } else {
            currentPlayer = 1 - currentPlayer;
        }

        sendGameState(message);
    }

    private void handleAttack(String move) {
        int fromIdx = move.charAt(0) - 'A';
        int toIdx = move.charAt(1) - 'A';

        List<String> attackerHand = (currentPlayer == 0) ? hand1 : hand2;
        List<String> defenderHand = (currentPlayer == 0) ? hand2 : hand1;

        if (fromIdx < 0 || fromIdx >= HAND_SIZE || toIdx < 0 || toIdx >= HAND_SIZE) {
            System.out.println(" Player " + (currentPlayer + 1) + " attempted invalid card positions: " + move);
            sendGameState("Invalid card positions");
            return;
        }

        // Check if positions have valid cards
        if (isEmptyPosition(attackerHand, fromIdx)) {
            System.out
                    .println(" Player " + (currentPlayer + 1) + " attempted attack from empty position: " + move);
            sendGameState("No card at attacking position");
            return;
        }

        if (isEmptyPosition(defenderHand, toIdx)) {
            System.out.println(" Player " + (currentPlayer + 1) + " attempted attack on empty position: " + move);
            sendGameState("No card at target position");
            return;
        }

        String attackUnit = attackerHand.get(fromIdx);
        String targetUnit = defenderHand.get(toIdx);

        if (!isValidAttack(attackUnit, targetUnit)) {
            System.out.println(" Player " + (currentPlayer + 1) + " attempted invalid attack: "
                    + attackUnit + " cannot attack " + targetUnit);
            sendGameState("Invalid attack: " + attackUnit + " cannot attack " + targetUnit);
            return;
        }

        // Process the attack - set target position to empty
        defenderHand.set(toIdx, "");

        System.out.println(" Player " + (currentPlayer + 1) + " attacked with " + attackUnit
                + " at position " + (char) ('A' + fromIdx) + " against " + targetUnit
                + " at position " + (char) ('A' + toIdx));

        // Build message and update score
        StringBuilder message = logAttackMessage(attackUnit, targetUnit, fromIdx, toIdx);

        // Switch turns and reset pass flags
        currentPlayer = 1 - currentPlayer;
        p1Pass = false;
        p2Pass = false;

        sendGameState(message.toString());
    }

    private boolean isEmptyPosition(List<String> hand, int position) {
        return position >= hand.size() || hand.get(position) == null || hand.get(position).isEmpty();
    }

    private StringBuilder logAttackMessage(String attackUnit, String targetUnit, int fromIdx, int toIdx) {
        StringBuilder message = new StringBuilder();
        message.append("Player ").append(currentPlayer + 1)
                .append("'s ").append(attackUnit)
                .append(" (").append((char) ('A' + fromIdx)).append(")")
                .append(" → ").append(targetUnit)
                .append(" (").append((char) ('A' + toIdx)).append(")");

        // Update score if both cards are not arrows
        if (!attackUnit.equals("Arrow") && !targetUnit.equals("Arrow")) {
            if (currentPlayer == 0) {
                score1++;
                System.out.println(" Player 1 scored a point! Score is now P1: " + score1 + " - P2: " + score2);
            } else {
                score2++;
                System.out.println(" Player 2 scored a point! Score is now P1: " + score1 + " - P2: " + score2);
            }
            message.append(" (+1pt)");
        }

        return message;
    }

    private boolean isValidAttack(String attacker, String defender) {
        if (attacker.equals("Axe") && defender.equals("Hammer"))
            return true;
        if (attacker.equals("Hammer") && defender.equals("Sword"))
            return true;
        if (attacker.equals("Sword") && defender.equals("Axe"))
            return true;
        if (attacker.equals("Arrow") || defender.equals("Arrow"))
            return true;
        return false;
    }
}