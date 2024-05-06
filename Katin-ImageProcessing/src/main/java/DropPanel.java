import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.util.List;

public class DropPanel extends JPanel {

    public DropPanel() {
        setPreferredSize(new Dimension(400, 200));
        setDropTarget(new DropTarget() {
            public synchronized void drop(DropTargetDropEvent evt) {
                try {
                    evt.acceptDrop(DnDConstants.ACTION_COPY);
                    Transferable transferable = evt.getTransferable();
                    if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        List<File> files = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                        if (!files.isEmpty()) {
                            File imageFile = files.get(0);
                            System.out.println("Dropped image file: " + imageFile.getAbsolutePath());
                            ImageSelectForm parentForm = (ImageSelectForm) SwingUtilities.getWindowAncestor(DropPanel.this);
                            parentForm.displayImage(imageFile);
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }
}
