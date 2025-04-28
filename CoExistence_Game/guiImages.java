import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.io.File;
import java.net.URL;

public class guiImages {
    private static final Map<String, ImageIcon> unitImages = new HashMap<>();
    private static final int CARD_WIDTH = 80;
    private static final int CARD_HEIGHT = 100;

    static {
        System.out.println("Initializing UnitImages...");
        try {
            // Try to load images from classpath or relative paths
            loadImage("Axe", "images/Axe.png");
            loadImage("Hammer", "images/Hammer.png");
            loadImage("Sword", "images/Sword.png");
            loadImage("Arrow", "images/Arrow.png");

            System.out.println("Successfully loaded images: " + unitImages.keySet());
        } catch (IOException e) {
            System.err.println("Failed to load unit images: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void loadImage(String unitType, String path) throws IOException {
        try {
            // Try to load from classloader
            URL imageUrl = guiImages.class.getClassLoader().getResource(path);
            if (imageUrl != null) {
                BufferedImage original = ImageIO.read(imageUrl);
                Image scaled = getScaledImage(original, CARD_WIDTH, CARD_HEIGHT);
                unitImages.put(unitType, new ImageIcon(scaled));
                return;
            }

            // Load from the src/images folder
            File file = new File("src/images/" + unitType + ".png");
            if (file.exists()) {
                BufferedImage original = ImageIO.read(file);
                Image scaled = getScaledImage(original, CARD_WIDTH, CARD_HEIGHT);
                unitImages.put(unitType, new ImageIcon(scaled));
                return;
            }

            throw new IOException("Image not found: " + path);
        } catch (Exception e) {
            System.err.println("Error loading image " + unitType + ": " + e.getMessage());
            throw new IOException("Failed to load image for " + unitType, e);
        }
    }

    private static Image getScaledImage(Image srcImg, int width, int height) {
        // Create a new high-quality scaled image
        BufferedImage resizedImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = resizedImg.createGraphics();

        // Draw the image preserving aspect ratio
        int srcWidth = srcImg.getWidth(null);
        int srcHeight = srcImg.getHeight(null);

        // Calculate dimensions to maintain aspect ratio
        double ratio = Math.min(
                (double) width / srcWidth,
                (double) height / srcHeight);

        int newWidth = (int) (srcWidth * ratio);
        int newHeight = (int) (srcHeight * ratio);

        // Center the image
        int x = (width - newWidth) / 2;
        int y = (height - newHeight) / 2;

        g2.drawImage(srcImg, x, y, newWidth, newHeight, null);
        g2.dispose();
        

        return resizedImg;
    }

    // Create an empty card for slots with no card
    private static ImageIcon createEmptyCardImage() {
        BufferedImage image = new BufferedImage(CARD_WIDTH, CARD_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // Enable anti-aliasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw a light gray background
        g2d.setColor(new Color(240, 240, 240));
        g2d.fillRect(0, 0, CARD_WIDTH, CARD_HEIGHT);

        // Draw a light border
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.drawRect(2, 2, CARD_WIDTH - 4, CARD_HEIGHT - 4);

        g2d.dispose();
        return new ImageIcon(image);
    }

    public static ImageIcon getImage(String unitType) {
        if (unitType == null || unitType.isEmpty()) {
            // Return an empty card image
            return createEmptyCardImage();
        }

        ImageIcon icon = unitImages.get(unitType);
        if (icon == null) {
            throw new IllegalArgumentException("No image found for unit type: " + unitType);
        }
        return icon;
    }
}