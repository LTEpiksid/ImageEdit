import javax.swing.*;
import java.awt.*;
import javax.swing.event.ChangeListener;

// Interface for classes that can update images based on HSB settings
interface ImageUpdater {
    void updateImage(int hue, int saturation, int brightness);
}

public class HSBControlForm extends JFrame {
    private JSlider hueSlider, saturationSlider, brightnessSlider;
    private ImageUpdater imageUpdater;

    public HSBControlForm(ImageUpdater imageUpdater) {
        this.imageUpdater = imageUpdater;
        setTitle("Adjust HSB Settings");
        setSize(400, 200);
        setLocationRelativeTo(null); // Set location relative to null to center the form on the screen
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        initUI();
    }


    private void initUI() {
        setLayout(new GridLayout(3, 2));  // Use GridLayout for better organization of labels and sliders

        // Hue slider
        hueSlider = createSlider(0, 360, 0, 90, "Hue:");
        add(new JLabel("Hue:"));
        add(hueSlider);

        // Saturation slider, range extended to 200%
        saturationSlider = createSlider(0, 200, 100, 50, "Saturation:");
        add(new JLabel("Saturation:"));
        add(saturationSlider);

        // Brightness slider, range extended to 200%
        brightnessSlider = createSlider(0, 200, 100, 50, "Brightness:");
        add(new JLabel("Brightness:"));
        add(brightnessSlider);

        // Add change listeners to sliders
        addChangeListener(hueSlider);
        addChangeListener(saturationSlider);
        addChangeListener(brightnessSlider);
    }

    // Helper method to create a slider
    private JSlider createSlider(int min, int max, int initialValue, int tickSpacing, String label) {
        JSlider slider = new JSlider(min, max, initialValue);
        slider.setMajorTickSpacing(tickSpacing);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        return slider;
    }

    // Helper method to add a change listener to a slider
    private void addChangeListener(JSlider slider) {
        slider.addChangeListener(e -> {
            updateImage();
        });
    }

    private void updateImage() {
        imageUpdater.updateImage(hueSlider.getValue(), saturationSlider.getValue(), brightnessSlider.getValue());
    }
}