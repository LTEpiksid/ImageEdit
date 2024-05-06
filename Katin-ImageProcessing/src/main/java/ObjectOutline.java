import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.Queue;
import java.util.LinkedList;


class ObjectOutline {
    public static BufferedImage detectObjects(BufferedImage originalImage) {
        BufferedImage grayscaleImage = ImageProcessorObject.convertToGrayscale(originalImage);
        BufferedImage edgeImage = ImageProcessorObject.applySobelEdgeDetection(grayscaleImage);
        BufferedImage binaryImage = ImageProcessorObject.binarizeImage(edgeImage, 128); // Example threshold
        BufferedImage segmentedImage = ImageProcessorObject.performColorBasedSegmentation(originalImage);
        BufferedImage outlinedImage = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = outlinedImage.createGraphics();
        graphics.drawImage(originalImage, 0, 0, null);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(Color.BLACK);
        graphics.setStroke(new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Create a thread pool executor with fixed number of threads
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        // List to hold detected objects
        List<Shape> objects = new ArrayList<>();

        // Calculate the height per thread for dividing the image
        int heightPerThread = originalImage.getHeight() / executor.getMaximumPoolSize();

        // Divide the image and assign portions to threads for processing
        for (int i = 0; i < executor.getMaximumPoolSize(); i++) {
            int startY = i * heightPerThread;
            int endY = (i == executor.getMaximumPoolSize() - 1) ? originalImage.getHeight() : (i + 1) * heightPerThread;
            final int threadStartY = startY;

            // Execute each portion of the image processing in a separate thread
            executor.execute(() -> {
                List<Shape> threadObjects = ImageProcessorObject.findObjects(binaryImage, segmentedImage, threadStartY, endY);
                synchronized (objects) {
                    objects.addAll(threadObjects);
                }
            });
        }

        // Shutdown the executor after all threads finish processing
        executor.shutdown();
        try {
            // Wait for all threads to finish
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (Shape object : objects) {
            graphics.draw(object); // Draw the detected object's shape as the outline
        }

        graphics.dispose();
        return outlinedImage;
    }
}

class ImageProcessorObject {
    public static BufferedImage convertToGrayscale(BufferedImage image) {
        BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics g = result.getGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return result;
    }

    public static BufferedImage applySobelEdgeDetection(BufferedImage grayscaleImage) {
        final int width = grayscaleImage.getWidth();
        final int height = grayscaleImage.getHeight();
        BufferedImage output = new BufferedImage(width, height, grayscaleImage.getType());

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int px = grayscaleImage.getRGB(x, y) & 0xFF;

                int px00 = grayscaleImage.getRGB(x - 1, y - 1) & 0xFF;
                int px20 = grayscaleImage.getRGB(x + 1, y - 1) & 0xFF;
                int px02 = grayscaleImage.getRGB(x - 1, y + 1) & 0xFF;
                int px22 = grayscaleImage.getRGB(x + 1, y + 1) & 0xFF;
                int px10 = grayscaleImage.getRGB(x, y - 1) & 0xFF;
                int px12 = grayscaleImage.getRGB(x, y + 1) & 0xFF;
                int px01 = grayscaleImage.getRGB(x - 1, y) & 0xFF;
                int px21 = grayscaleImage.getRGB(x + 1, y) & 0xFF;

                // Sobel X filter
                int gx = ((px20 + 2 * px21 + px22) - (px00 + 2 * px01 + px02));
                // Sobel Y filter
                int gy = ((px02 + 2 * px12 + px22) - (px00 + 2 * px10 + px20));

                int gval = (int) Math.sqrt(gx * gx + gy * gy);
                output.setRGB(x, y, gval > 255 ? 0xFFFFFFFF : (0xFF000000 | (gval << 16 | gval << 8 | gval)));
            }
        }
        return output;
    }

    public static BufferedImage binarizeImage(BufferedImage edgeImage, int threshold) {
        BufferedImage result = new BufferedImage(edgeImage.getWidth(), edgeImage.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        for (int y = 0; y < edgeImage.getHeight(); y++) {
            for (int x = 0; x < edgeImage.getWidth(); x++) {
                int rgb = edgeImage.getRGB(x, y) & 0xFF;
                result.setRGB(x, y, rgb > threshold ? 0xFFFFFFFF : 0x00000000);
            }
        }
        return result;
    }

    public static BufferedImage performColorBasedSegmentation(BufferedImage originalImage) {
        BufferedImage segmentedImage = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        // Example: Simple thresholding based on RGB intensity
        int thresholdRed = 100;
        int thresholdGreen = 100;
        int thresholdBlue = 100;

        for (int y = 0; y < originalImage.getHeight(); y++) {
            for (int x = 0; x < originalImage.getWidth(); x++) {
                Color color = new Color(originalImage.getRGB(x, y));
                int red = color.getRed();
                int green = color.getGreen();
                int blue = color.getBlue();
                // Check if color intensity exceeds thresholds
                if (red > thresholdRed && green > thresholdGreen && blue > thresholdBlue) {
                    segmentedImage.setRGB(x, y, Color.WHITE.getRGB()); // Set as foreground
                } else {
                    segmentedImage.setRGB(x, y, Color.BLACK.getRGB()); // Set as background
                }
            }
        }
        return segmentedImage;
    }

    public static List<Shape> findObjects(BufferedImage binaryImage, BufferedImage originalImage, int startY, int endY) {
        List<Shape> objects = new ArrayList<>();
        boolean[][] visited = new boolean[binaryImage.getWidth()][binaryImage.getHeight()];
        int outlineThickness = 1; // Define the outline thickness here (thinner)
        for (int y = startY; y < endY; y++) {
            for (int x = 0; x < binaryImage.getWidth(); x++) {
                if (!visited[x][y] && (binaryImage.getRGB(x, y) & 0xFF) == 255) {
                    Shape object = floodFill(binaryImage, visited, x, y, outlineThickness);
                    if (isObjectLargeEnough(object, originalImage)) {
                        objects.add(object);
                    }
                }
            }
        }
        return objects;
    }

    private static Shape floodFill(BufferedImage image, boolean[][] visited, int x, int y, int outlineThickness) {
        Graphics2D g2d = image.createGraphics();
        Path2D path = new Path2D.Double();
        Queue<Point> queue = new LinkedList<>();
        queue.add(new Point(x, y));
        int minX = x, maxX = x, minY = y, maxY = y;
        while (!queue.isEmpty()) {
            Point p = queue.poll();
            if (visited[p.x][p.y]) continue;
            visited[p.x][p.y] = true;
            boolean isBorderPixel = false;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    int nx = p.x + dx, ny = p.y + dy;
                    if (nx >= 0 && ny >= 0 && nx < image.getWidth() && ny < image.getHeight() && !visited[nx][ny]) {
                        if ((image.getRGB(nx, ny) & 0xFF) == 255) {
                            queue.add(new Point(nx, ny));
                        } else {
                            isBorderPixel = true;
                        }
                    }
                }
            }
            if (isBorderPixel) {
                int outlineOffset = outlineThickness / 2; // Offset to center the outline
                int outlineX = Math.max(0, p.x - outlineOffset);
                int outlineY = Math.max(0, p.y - outlineOffset);
                int outlineWidth = Math.min(image.getWidth() - outlineX, outlineThickness);
                int outlineHeight = Math.min(image.getHeight() - outlineY, outlineThickness);
                path.append(new Rectangle(outlineX, outlineY, outlineWidth, outlineHeight), false);
            }
        }
        return path;
    }


    private static boolean isObjectLargeEnough(Shape object, BufferedImage image) {
        Rectangle bounds = object.getBounds();
        double minSize = Math.min(image.getWidth(), image.getHeight()) * 0.1; // 1/5th of the smaller dimension
        return bounds.width >= minSize && bounds.height >= minSize;
    }
}
