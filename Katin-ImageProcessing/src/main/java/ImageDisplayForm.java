import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class ImageDisplayForm extends JFrame implements ImageUpdater {
    private JLabel imageLabel;
    private BufferedImage originalImage;
    private BufferedImage hsbAdjustedImage;
    private BufferedImage processedImage;
    private ImageProcessor imageProcessor;

    public ImageDisplayForm() {
        setTitle("Image Display");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1200, 1200);
        setLocationRelativeTo(null);
        setResizable(false);

        imageLabel = new JLabel("", JLabel.CENTER);
        imageLabel.setVerticalAlignment(JLabel.CENTER);
        getContentPane().add(imageLabel, BorderLayout.CENTER);

        JPanel panelButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnAdjustSaturation = new JButton("Adjust HSB");
        btnAdjustSaturation.addActionListener(e -> new HSBControlForm(this).setVisible(true));

        JButton btnDetectSquares = new JButton("Outline");
        btnDetectSquares.addActionListener(e -> detectSquares());

        JButton btnSave = new JButton("Save");
        btnSave.addActionListener(e -> saveImage());

        panelButtons.add(btnAdjustSaturation);
        panelButtons.add(btnDetectSquares);
        panelButtons.add(btnSave);
        getContentPane().add(panelButtons, BorderLayout.SOUTH);
    }

    private void detectSquares() {
        if (originalImage != null) {
            processedImage = ObjectOutline.detectObjects(deepCopy(originalImage));
            updateImageDisplay(processedImage);
        }
    }

    private BufferedImage deepCopy(BufferedImage bi) {
        int width = bi.getWidth();
        int height = bi.getHeight();
        int type = bi.getType();
        BufferedImage newImage = new BufferedImage(width, height, type);
        Graphics2D g = newImage.createGraphics();
        g.drawImage(bi, 0, 0, null);
        g.dispose();
        return newImage;
    }

    public void displayImage(File imageFile) {
        try {
            originalImage = ImageIO.read(imageFile);
            hsbAdjustedImage = deepCopy(originalImage);
            imageProcessor = new ImageProcessor(hsbAdjustedImage);
            updateImageDisplay(originalImage);
        } catch (IOException e) {
            System.out.println("Error loading image: " + e.getMessage());
        }
    }

    private void updateImageDisplay(BufferedImage image) {
        Image scaledImage = scaleImageToFitForm(image);
        ImageIcon icon = new ImageIcon(scaledImage);
        imageLabel.setIcon(icon);
        imageLabel.revalidate();
        imageLabel.repaint();
    }

    private Image scaleImageToFitForm(BufferedImage image) {
        int formWidth = getWidth() - 20;
        int formHeight = getHeight() - 120;
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();

        double scale = Math.min((double) formWidth / imageWidth, (double) formHeight / imageHeight);
        int scaledWidth = (int) (imageWidth * scale);
        int scaledHeight = (int) (imageHeight * scale);

        return image.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
    }

    private void saveImage() {
        BufferedImage imageToSave = processedImage != null ? processedImage : hsbAdjustedImage;
        if (imageToSave != null) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save Image");
            fileChooser.setFileFilter(new FileNameExtensionFilter("PNG images", "png"));
            int userSelection = fileChooser.showSaveDialog(this);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();
                try {
                    ImageIO.write(imageToSave, "PNG", new File(fileToSave.getAbsolutePath() + ".png"));
                    JOptionPane.showMessageDialog(this, "Image saved as: " + fileToSave.getName(), "Save Image", JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(this, "Error saving image: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } else {
            JOptionPane.showMessageDialog(this, "No image to save", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void updateImage(int hue, int saturation, int brightness) {
        if (imageProcessor != null) {
            imageProcessor.updateHSB(hue, saturation, brightness);
            hsbAdjustedImage = imageProcessor.getCurrentImage();
            updateImageDisplay(hsbAdjustedImage);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ImageDisplayForm form = new ImageDisplayForm();
            form.displayImage(new File("example_image.png")); // Change "example_image.png" to your image file path
            form.setVisible(true);
        });
    }
}
