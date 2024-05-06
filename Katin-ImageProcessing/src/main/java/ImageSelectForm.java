import javax.swing.*;
import java.io.File;

public class ImageSelectForm extends JFrame {

    private final ImageDisplayForm imageDisplayForm = new ImageDisplayForm();

    public ImageSelectForm() {
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Image Select Form");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JButton selectImageButton = new JButton("Select Image");
        selectImageButton.addActionListener(e -> selectImage());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(selectImageButton);

        JPanel dropPanel = new DropPanel();
        dropPanel.setBorder(BorderFactory.createTitledBorder("Drag and Drop Image Here"));

        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        add(buttonPanel);
        add(dropPanel);

        pack();
        setLocationRelativeTo(null);
    }

    private void selectImage() {
        ImageFileSelectionDialog fileSelectionDialog = new ImageFileSelectionDialog();
        File selectedFile = fileSelectionDialog.getSelectedFile();
        if (selectedFile != null) {
            displayImage(selectedFile);
        }
    }

    void displayImage(File imageFile) {
        imageDisplayForm.displayImage(imageFile);
        imageDisplayForm.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ImageSelectForm form = new ImageSelectForm();
            form.setVisible(true);
        });
    }
}
