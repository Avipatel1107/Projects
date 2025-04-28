import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.Timer;

public class Connect4 extends JFrame {
    private JPanel gPanel;
    private JPanel bPanel;
    private JLabel description;
    private Game[][] game;
    private JButton[] colButton;
    private final Integer[] nRow;
    private final int rows;
    private final int columns;
    private int currPlayer;
    public Connect4(int row, int col) {
        super("Connect 4 Game");
        this.rows = row;
        this.columns = col;

        currPlayer = 1;
        nRow = new Integer[columns];
        for (int i = 0; i < columns; i++) {
            nRow[i] = rows - 1;
        }

        frameSetup();
        buttonSetup();
        gameSetup();
        descriptionSetup();
        pack();        //The pack method sizes the frame so that all its contents are at preferred size
        this.setVisible(true);
    }
    private void frameSetup() {
        this.setLocation(50, 50);
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setResizable(true);
    }

    /**
     * This method aims to create buttons for each column to drop our game pieces,
     * a JButton array is created for columns, a panel is created for the buttons and added to the
     * game frame
     */
    private void buttonSetup() {
        colButton = new JButton[columns];
        bPanel = new JPanel();
        bPanel.setLayout(new GridLayout(1, 0));

        for (int i = 0; i < columns; i++) {
            colButton[i] = new JButton(i+"");
            ButtonListener btnListener = new ButtonListener();
            colButton[i].addActionListener(btnListener);
            bPanel.add(colButton[i]);
        }
        this.add(bPanel, BorderLayout.NORTH);
    }
    /**
     * This method creates and sets the layout for the game, it creates an array to store
     * the game pieces.
     */
    private void gameSetup() {
        gPanel = new JPanel();
        gPanel.setPreferredSize(new Dimension(90 * columns, 90 * rows));
        gPanel.setLayout(new GridLayout(rows, columns));
        game = new Game[rows][columns];

        // create the game and add it to the panel
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                game[i][j] = new Game();
                gPanel.add(game[i][j]);
            }
        }
        // add game panel to the frame
        this.add(gPanel, BorderLayout.CENTER);
    }

    /**
     This method aims to create a label for which players turn  it is and add it to the game frame
     */
    private void descriptionSetup() {
        description = new JLabel("Player " + currPlayer + "'s turn...", JLabel.CENTER);
        this.add(description, BorderLayout.SOUTH);
    }
    private void updateGameBoard(int row, int col) {
        game[row][col].setPlayer(currPlayer);
        if (currPlayer == 1) {
            currPlayer = 2;
        } else {
            currPlayer = 1;
        }
        nRow[col]--;
        description.setText("Player " + currPlayer + "'s turn...");
    }
    /**
     * This method allows for the current player to use the created column buttons to place the
     * game pieces down and will update the board accordingly.
     */
    private class ButtonListener implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent e) {
            JButton button = (JButton) e.getSource();
            int col = Integer.parseInt(button.getText());
            if (nRow[col] >= 0) {
                updateGameBoard(nRow[col], col);
            }
            if (nRow[col] < 0) {
                nRow[col] = 0;
                button.setEnabled(false);
            }
            checkWin();
        }

    }
    private void checkWin() {
        int row = Collections.min(Arrays.asList(nRow));
        if (row >= rows - 3) {
            row = rows - 4;
        }

        boolean exit = false;
        for (int i = row; i < rows - 3; i++) {
            for (int j = 0; j < columns - 3; j++) {
                if (allPossibilities(i, j)) {
                    exit = true;
                    break;
                }
            }
            if (exit){
                ifEndGame();
                break;
            }
        }

    }
    private void ifEndGame() {
        for (JButton btn : colButton) {
            btn.setEnabled(false);
        }
        Timer timer = new Timer(5000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    /**
    If the game has ended this method aims to set the background of the winning combination to green
     and displays the winner info at bottom of game screen or a Tie Game.
     */
    private boolean endDescription(int[] arr) {
        if (arr[0] != -1) {
            int row = arr[1];
            int col = arr[2];
            for (int i = 0; i < 4; i++) {
                if (arr[0] == 0) game[row][col + i].setBackground(Color.GREEN);
                else if (arr[0] == 1) game[row + i][col].setBackground(Color.GREEN);
                else if (arr[0] == 2) game[row+i][col+i].setBackground(Color.GREEN);
                else game[row + i][col - i].setBackground(Color.GREEN);
            }

            int winner = game[row][col].getPlayer();
            description.setText("Player " + winner + " wins!!!");
            return true;
        } else {
            if (!noSpace()) {
                description.setText("Tie Game");
                for (JButton btn : colButton) {
                    btn.setEnabled(false);
                }
                return true;
            }
            return false;
        }

    }

    /**
     * The methods below are used to check the game winning possibility scenarios,it will check
     * rows, columns, and diagonal up/down combinations for a winner if possible.It will also
     * check the possibility for no space on the board in case of a tie were to occur.
     */
    private boolean allPossibilities(int row, int col) {
        int Up = 0;
        int Bottom = 0;
        int[] piece = new int[] {-1};

        for (int i = 0; i < 4; i++) {
            int cRow = 0;
            int cCol = 0;

            for (int j = 0; j < 3; j++) {
                if (checkRow(row, col, i, j)) cRow++;
                if (checkColumn(row, col, i, j)) cCol++;
                if (i==j) {
                    if (diagonalCheckBottomUp(row, col, i)) Up++;
                    if (diagonalCheckUpBottom(row, col, i)) Bottom++;
                }
            }

            if (cRow == 0) piece = new int[] {0, row + i, col};
            else if (cCol == 0)  piece = new int[] {1, row, col + i};
            else if (Up == 3) piece = new int[] {2, row, col};
            else if (Bottom == 3) piece = new int[] {3, row, col + 3};

            if (endDescription(piece)) return true;
        }
        return false;
    }
    private boolean diagonalCheckBottomUp(int row, int col, int i) {
        return (game[row + i][col + i].getPlayer() != 0
                && game[row + i + 1][col + i + 1].getPlayer() != 0
                && game[row + i][col + i].getPlayer() ==
                game[row + i + 1][col + i + 1].getPlayer());
    }
    private boolean diagonalCheckUpBottom(int row, int col, int i) {
        return (game[row + 3 - i][col + i].getPlayer() != 0
                && game[row + 2 - i][col + i + 1].getPlayer() != 0
                && game[row + 3 - i][col + i].getPlayer() ==
                game[row + 2 - i][col + i + 1].getPlayer());
    }
    private boolean checkColumn(int row, int col, int i, int j) {
        return game[row + j][col + i].getPlayer() == 0
                || game[row + j +1][col + i].getPlayer() == 0
                || game[row + j][col + i].getPlayer() != game[row + j +1][col + i].getPlayer();
    }
    private boolean checkRow(int row, int col, int i, int j) {
        return game[row + i][col + j].getPlayer() == 0
                || game[row + i][col + j+1].getPlayer() == 0
                || game[row + i][col + j].getPlayer() != game[row + i][col + j+1].getPlayer();
    }
    private boolean noSpace() {
        for (int j = 0; j < columns; j++) {
            if (game[0][j].getPlayer() == 0) {
                return true;
            }
        }
        return false;
    }


}
