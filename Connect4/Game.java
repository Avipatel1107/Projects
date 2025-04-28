
import java.awt.*;
import javax.swing.*;

public class Game extends JPanel {
    private int player;

    /**
     * Creates a game board with blue background and set the player to 0
     */
    public Game() {
        this.player = 0;
        this.setBackground(new Color(0, 0, 255));
    }
    
    public void setPlayer(int player) {
        if (player != 0 && player != 1 && player !=2) {
            throw new IllegalArgumentException(" player must be either 0, 1 or 2");
        }
        this.player = player;
        repaint();
    }

    public int getPlayer() {
        return player;
    }


/**
 The inbuilt JComponent container called paintComponent
 was overridden and used to paint the game board, Player 1 is Red and Player 2 is Yellow.
 */
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (player == 0) {
            g.setColor(Color.WHITE);
        } else if (player == 1) {
            g.setColor(Color.RED);
        } else {
            g.setColor(Color.YELLOW);
        }
        g.fillOval(4, 3, this.getWidth() - 10, this.getHeight() - 8);

    }
}