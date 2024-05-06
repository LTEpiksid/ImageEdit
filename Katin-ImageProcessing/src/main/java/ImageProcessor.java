import java.awt.*;
import java.awt.image.BufferedImage;

public class ImageProcessor {
    private BufferedImage originalImage;
    private BufferedImage currentImage;

    public ImageProcessor(BufferedImage originalImage) {
        this.originalImage = originalImage;
        this.currentImage = originalImage;
    }

    public BufferedImage getCurrentImage() {
        return currentImage;
    }

    public void updateHSB(int hue, int saturation, int brightness) {
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        currentImage = processImage(originalImage, hue, saturation, brightness, width, height);
    }

    private BufferedImage processImage(BufferedImage originalImage, int hue, int saturation, int brightness, int width, int height) {
        BufferedImage processedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // Multithreading for faster processing
        int processors = Runtime.getRuntime().availableProcessors();
        int tasksPerProcessor = height / processors;

        Thread[] threads = new Thread[processors];
        for (int i = 0; i < processors; i++) {
            int startRow = i * tasksPerProcessor;
            int endRow = (i == processors - 1) ? height : (i + 1) * tasksPerProcessor;

            threads[i] = new Thread(() -> {
                for (int y = startRow; y < endRow; y++) {
                    for (int x = 0; x < width; x++) {
                        int rgb = originalImage.getRGB(x, y);
                        float[] hsb = Color.RGBtoHSB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, null);

                        // Adjust HSB values
                        hsb[0] = (hsb[0] + (hue / 360.0f)) % 1.0f;
                        hsb[1] = Math.min(hsb[1] * (saturation / 100.0f), 1.0f);
                        hsb[2] = Math.min(hsb[2] * (brightness / 100.0f), 1.0f);

                        rgb = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
                        processedImage.setRGB(x, y, rgb);
                    }
                }
            });
            threads[i].start();
        }

        // Wait for all threads to finish
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return processedImage;
    }
}
