import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class guiClient {
    private static final int DEFAULT_PORT = 35754;
    private static final String DEFAULT_IP = "localhost";

    public static void main(String[] args) {
        final String serverIP;
        final int port;

        if (args.length >= 2) {
            serverIP = args[0];
            int tempPort;
            try {
                tempPort = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port number. Using default port " + DEFAULT_PORT);
                tempPort = DEFAULT_PORT;
            }
            port = tempPort;
        } else if (args.length == 1) {
            serverIP = args[0];
            System.out.println("No port specified. Using default port " + DEFAULT_PORT);
            port = DEFAULT_PORT;
        } else {
            System.out.println("No IP or port specified. Using localhost:" + DEFAULT_PORT);
            serverIP = DEFAULT_IP;
            port = DEFAULT_PORT;
        }

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            SwingUtilities.invokeLater(() -> new guiClient(serverIP, port));
        } catch (Exception e) {
            System.err.println("Error starting client: " + e.getMessage());
            e.printStackTrace();
            SwingUtilities.invokeLater(() -> new guiClient(serverIP, port));
        }
    }

    private String serverAddressIP;
    private int serverPort;
    private Socket clientSocket;
    private BufferedReader inputReader;
    private PrintWriter outputWriter;
    private int currentPlayerNumber = 0;

    // GUI components
    private JFrame frame;
    private JPanel gamePanel;
    private JTextArea messageLog;
    private JPanel controlPanel;
    private JComboBox<String> fromSelector;
    private JComboBox<String> toSelector;
    private JButton attackButton;
    private JButton passButton;

    // Game state
    private boolean myTurn = false;
    private boolean gameEnded = false;
    private int playerScore = 0;
    private int enemyScore = 0;
    private int currentRound = 1;
    private String[] playerHand = new String[6];
    private String[] enemyHand = new String[6];
    private String latestMessage = "";

    public guiClient(String serverAddress, int serverPort) {
        this.serverAddressIP = serverAddress;
        this.serverPort = serverPort;
        setupGUI();
        connectToServer();
    }

    private void setupGUI() {
        frame = new JFrame("CoExistence - Connecting...");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 500);

        // Add connecting message
        JPanel connectingPanel = new JPanel(new BorderLayout());
        JLabel connectingLabel = new JLabel("Connecting to server...", SwingConstants.CENTER);
        connectingLabel.setFont(new Font("Arial", Font.BOLD, 16));
        connectingPanel.add(connectingLabel, BorderLayout.CENTER);
        frame.add(connectingPanel);

        frame.setVisible(true);

        // Setup the rest of the GUI in a separate thread
        SwingUtilities.invokeLater(() -> {
            frame.getContentPane().removeAll();
            frame.setLayout(new BorderLayout());

            // Game board panel
            gamePanel = new JPanel(new BorderLayout());
            gamePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            // Opponent area (top)
            JPanel opponentPanel = new JPanel(new BorderLayout());
            JPanel opponentCards = new JPanel(new GridLayout(1, 6, 5, 0));
            for (int i = 0; i < 6; i++) {
                JPanel cardPanel = createCardPanel(null);
                cardPanel.setBorder(BorderFactory.createTitledBorder(String.valueOf((char) ('A' + i))));
                opponentCards.add(cardPanel);
            }
            JLabel opponentScoreLabel = new JLabel("Opponent Score: 0");
            opponentPanel.add(opponentScoreLabel, BorderLayout.NORTH);
            opponentPanel.add(opponentCards, BorderLayout.CENTER);

            // Center area with message log and round info
            JPanel centerPanel = new JPanel(new BorderLayout());
            messageLog = new JTextArea(3, 30);
            messageLog.setEditable(false);
            centerPanel.add(new JScrollPane(messageLog), BorderLayout.CENTER);
            JLabel roundLabel = new JLabel("Round: 1", SwingConstants.CENTER);
            centerPanel.add(roundLabel, BorderLayout.NORTH);

            // Player area (bottom)
            JPanel playerPanel = new JPanel(new BorderLayout());
            JPanel playerCards = new JPanel(new GridLayout(1, 6, 5, 0));
            for (int i = 0; i < 6; i++) {
                JPanel cardPanel = createCardPanel(null);
                cardPanel.setBorder(BorderFactory.createTitledBorder(String.valueOf((char) ('A' + i))));
                playerCards.add(cardPanel);
            }
            JLabel playerScoreLabel = new JLabel("Your Score: 0");
            playerPanel.add(playerScoreLabel, BorderLayout.SOUTH);
            playerPanel.add(playerCards, BorderLayout.CENTER);

            // Add components to game panel
            gamePanel.add(opponentPanel, BorderLayout.NORTH);
            gamePanel.add(centerPanel, BorderLayout.CENTER);
            gamePanel.add(playerPanel, BorderLayout.SOUTH);

            // Control panel
            controlPanel = new JPanel();
            fromSelector = new JComboBox<>(new String[] { "A", "B", "C", "D", "E", "F" });
            toSelector = new JComboBox<>(new String[] { "A", "B", "C", "D", "E", "F" });
            attackButton = new JButton("Attack");
            passButton = new JButton("Pass");

            attackButton.addActionListener(e -> sendAttack());
            passButton.addActionListener(e -> sendPass());

            controlPanel.add(new JLabel("From:"));
            controlPanel.add(fromSelector);
            controlPanel.add(new JLabel("To:"));
            controlPanel.add(toSelector);
            controlPanel.add(attackButton);
            controlPanel.add(passButton);

            frame.add(gamePanel, BorderLayout.CENTER);
            frame.add(controlPanel, BorderLayout.SOUTH);

            setControlsEnabled(false);

            frame.revalidate();
            frame.repaint();
        });
    }

    private JPanel createCardPanel(String unitType) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(80, 100));
        panel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        panel.setBackground(Color.WHITE);

        if (unitType != null) {
            try {
                ImageIcon icon = guiImages.getImage(unitType);
                if (icon != null) {
                    JLabel imageLabel = new JLabel(icon);
                    imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
                    panel.add(imageLabel, BorderLayout.CENTER);
                    imageLabel.setToolTipText(unitType);
                }
            } catch (Exception e) {
                JLabel textLabel = new JLabel(unitType);
                textLabel.setHorizontalAlignment(SwingConstants.CENTER);
                panel.add(textLabel, BorderLayout.CENTER);
            }
        }

        return panel;
    }

    private void updateGameBoard() {
        // Update opponent cards
        JPanel opponentCards = (JPanel) ((JPanel) gamePanel.getComponent(0)).getComponent(1);
        for (int i = 0; i < 6; i++) {
            JPanel cardPanel = (JPanel) opponentCards.getComponent(i);
            cardPanel.removeAll();

            String cardType = null;
            // Check if there's a valid card in this position
            if (i < enemyHand.length && enemyHand[i] != null && !enemyHand[i].isEmpty()) {
                cardType = enemyHand[i];

                try {
                    ImageIcon icon = guiImages.getImage(cardType);
                    if (icon != null) {
                        JLabel imageLabel = new JLabel(icon);
                        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
                        cardPanel.add(imageLabel, BorderLayout.CENTER);
                        imageLabel.setToolTipText(cardType);
                    }
                } catch (Exception e) {
                    JLabel fallbackLabel = new JLabel(cardType);
                    fallbackLabel.setHorizontalAlignment(SwingConstants.CENTER);
                    cardPanel.add(fallbackLabel, BorderLayout.CENTER);
                }

                cardPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
            } else {
                // Empty slot
                try {
                    ImageIcon icon = guiImages.getImage(null); // Will return a blank card
                    JLabel imageLabel = new JLabel(icon);
                    imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
                    cardPanel.add(imageLabel, BorderLayout.CENTER);
                } catch (Exception e) {
                    // Fallback for empty slot - just leave it blank
                    cardPanel.setBackground(new Color(240, 240, 240));
                }
                cardPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
            }

            JLabel posLabel = new JLabel(String.valueOf((char) ('A' + i)));
            posLabel.setHorizontalAlignment(SwingConstants.CENTER);
            cardPanel.add(posLabel, BorderLayout.SOUTH);

            cardPanel.revalidate();
            cardPanel.repaint();
        }

        // Update player cards
        JPanel playerCards = (JPanel) ((JPanel) gamePanel.getComponent(2)).getComponent(1);
        for (int i = 0; i < 6; i++) {
            JPanel cardPanel = (JPanel) playerCards.getComponent(i);
            cardPanel.removeAll();

            String cardType = null;
            // Check if there's a valid card in this position
            if (i < playerHand.length && playerHand[i] != null && !playerHand[i].isEmpty()) {
                cardType = playerHand[i];

                try {
                    ImageIcon icon = guiImages.getImage(cardType);
                    if (icon != null) {
                        JLabel imageLabel = new JLabel(icon);
                        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
                        cardPanel.add(imageLabel, BorderLayout.CENTER);
                        imageLabel.setToolTipText(cardType);
                    }
                } catch (Exception e) {
                    JLabel fallbackLabel = new JLabel(cardType);
                    fallbackLabel.setHorizontalAlignment(SwingConstants.CENTER);
                    cardPanel.add(fallbackLabel, BorderLayout.CENTER);
                }

                cardPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
            } else {
                // Empty slot
                try {
                    ImageIcon icon = guiImages.getImage(null); // Will return a blank card
                    JLabel imageLabel = new JLabel(icon);
                    imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
                    cardPanel.add(imageLabel, BorderLayout.CENTER);
                } catch (Exception e) {
                    // Fallback for empty slot - just leave it blank
                    cardPanel.setBackground(new Color(240, 240, 240));
                }
                cardPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
            }

            JLabel posLabel = new JLabel(String.valueOf((char) ('A' + i)));
            posLabel.setHorizontalAlignment(SwingConstants.CENTER);
            cardPanel.add(posLabel, BorderLayout.SOUTH);

            cardPanel.revalidate();
            cardPanel.repaint();
        }

        // Update scores with colors
        JLabel opponentScoreLabel = (JLabel) ((JPanel) gamePanel.getComponent(0)).getComponent(0);
        opponentScoreLabel.setText("Opponent Score: " + enemyScore);
        if (latestMessage.contains("Player " + (currentPlayerNumber == 1 ? "2" : "1") + " scores")) {
            opponentScoreLabel.setForeground(Color.RED);
        } else {
            opponentScoreLabel.setForeground(Color.BLACK);
        }

        JLabel playerScoreLabel = (JLabel) ((JPanel) gamePanel.getComponent(2)).getComponent(0);
        playerScoreLabel.setText("Your Score: " + playerScore);
        if (latestMessage.contains("Player " + currentPlayerNumber + " scores")) {
            playerScoreLabel.setForeground(Color.GREEN);
        } else {
            playerScoreLabel.setForeground(Color.BLACK);
        }

        // Update round
        JPanel centerPanel = (JPanel) gamePanel.getComponent(1);
        JLabel roundLabel = (JLabel) centerPanel.getComponent(1);
        roundLabel.setText("Round: " + currentRound);

        // Update message log with formatted message
        if (!latestMessage.isEmpty()) {
            String formattedMessage = String.format("[Round %d] %s", currentRound, latestMessage);
            messageLog.append(formattedMessage + "\n");
            messageLog.setCaretPosition(messageLog.getDocument().getLength());
        }

        // Update controls based on turn
        setControlsEnabled(myTurn && !gameEnded);

        // Make turn indicator more visible
        String turnStatus = gameEnded ? "Game Over" : (myTurn ? "Your Turn" : "Opponent's Turn");
        frame.setTitle("CoExistence Game Client - Player " + currentPlayerNumber + " - " + turnStatus);

        // Apply background colors to make turns more visible
        playerCards.setBackground(myTurn ? new Color(230, 255, 230) : new Color(240, 240, 240));
        opponentCards.setBackground(!myTurn ? new Color(230, 255, 230) : new Color(240, 240, 240));
    }

    private void setControlsEnabled(boolean enabled) {
        fromSelector.setEnabled(enabled);
        toSelector.setEnabled(enabled);
        attackButton.setEnabled(enabled);
        passButton.setEnabled(enabled);

        attackButton.setBackground(enabled ? new Color(100, 200, 100) : null);
        passButton.setBackground(enabled ? new Color(200, 100, 100) : null);
    }

    private void connectToServer() {
        try {
            clientSocket = new Socket(serverAddressIP, serverPort);
            inputReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            outputWriter = new PrintWriter(clientSocket.getOutputStream(), true);

            new Thread(this::listenForServerMessages).start();
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Could not connect to server: " + e.getMessage());
            System.exit(1);
        }
    }

    private void listenForServerMessages() {
        try {
            String line;
            while ((line = inputReader.readLine()) != null) {
                if (line.startsWith("PLAYER:")) {
                    currentPlayerNumber = Integer.parseInt(line.substring(7));
                    frame.setTitle("CoExistence Game Client - Player " + currentPlayerNumber);
                    outputWriter.println("READY");
                    continue;
                }

                if (line.startsWith("STATE:")) {
                    parseGameState(line.substring(6));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Connection to server lost: " + e.getMessage());
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
        latestMessage = stateMap.get("message");
        currentRound = Integer.parseInt(stateMap.get("roundNumber"));
        gameEnded = Boolean.parseBoolean(stateMap.get("isGameOver"));
        playerScore = Integer.parseInt(stateMap.get("score"));
        enemyScore = Integer.parseInt(stateMap.get("opponentScore"));

        // Update hands
        String[] myHandCards = stateMap.get("hand").split(",");
        String[] opponentHandCards = stateMap.get("opponentHand").split(",");

        Arrays.fill(playerHand, null);
        Arrays.fill(enemyHand, null);

        for (int i = 0; i < myHandCards.length; i++) {
            String card = myHandCards[i];
            if (card != null && !card.isEmpty()) {
                playerHand[i] = card;
            } else {
                playerHand[i] = ""; // Explicitly set empty string for empty positions
            }
        }
        for (int i = 0; i < opponentHandCards.length; i++) {
            String card = opponentHandCards[i];
            if (card != null && !card.isEmpty()) {
                enemyHand[i] = card;
            } else {
                enemyHand[i] = ""; // Explicitly set empty string for empty positions
            }
        }

        // Update turn status
        int currentPlayer = Integer.parseInt(stateMap.get("currentPlayer"));
        myTurn = (currentPlayerNumber == 1 && currentPlayer == 0) || (currentPlayerNumber == 2 && currentPlayer == 1);

        // Update UI
        SwingUtilities.invokeLater(this::updateGameBoard);
    }

    private void sendAttack() {
        if (!myTurn || gameEnded)
            return;

        String from = (String) fromSelector.getSelectedItem();
        String to = (String) toSelector.getSelectedItem();

        int fromIdx = from.charAt(0) - 'A';
        if (fromIdx >= playerHand.length || playerHand[fromIdx] == null || playerHand[fromIdx].isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Invalid selection - no card in that position");
            return;
        }

        int toIdx = to.charAt(0) - 'A';
        if (toIdx >= enemyHand.length || enemyHand[toIdx] == null || enemyHand[toIdx].isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Invalid target - no card in that position");
            return;
        }

        String attackUnit = playerHand[fromIdx];
        String targetUnit = enemyHand[toIdx];

        if (!isValidAttack(attackUnit, targetUnit)) {
            JOptionPane.showMessageDialog(frame, "Invalid attack: " + attackUnit + " cannot attack " + targetUnit);
            return;
        }

        outputWriter.println(from + to);
        setControlsEnabled(false);
    }

    private boolean isValidAttack(String attacker, String defender) {
        if (attacker.equals("Axe") && defender.equals("Hammer"))
            return true;
        if (attacker.equals("Hammer") && defender.equals("Sword"))
            return true;
        if (attacker.equals("Sword") && defender.equals("Axe"))
            return true;
        if (attacker.equals("Arrow"))
            return true;
        if (defender.equals("Arrow"))
            return true;
        return false;
    }

    private void sendPass() {
        if (!myTurn || gameEnded)
            return;
        outputWriter.println("PS");
        setControlsEnabled(false);
    }
}