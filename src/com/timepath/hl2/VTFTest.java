package com.timepath.hl2;

import com.timepath.hl2.io.VTF;
import com.timepath.hl2.io.VTF.Format;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author TimePath
 */
public class VTFTest {

    private static final Logger LOG = Logger.getLogger(VTFTest.class.getName());

    public static void test() {
        final JFrame f = new JFrame("VTF Loader");
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JScrollPane jsp = new JScrollPane();
        jsp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        jsp.getVerticalScrollBar().setUnitIncrement(64);
        f.add(jsp);
        JPanel pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.PAGE_AXIS));
        jsp.setViewportView(pane);

        @SuppressWarnings("serial")
        class ImagePreviewPanel extends JPanel implements PropertyChangeListener {

            private static final int ACCSIZE = 256;

            private Image image;

            private Color bg;

            private final JSpinner lod, frame;

            private VTF v;

            ImagePreviewPanel() {
                setPreferredSize(new Dimension(ACCSIZE, -1));
                bg = getBackground();
                bg = Color.PINK;
                lod = new JSpinner();
                lod.addChangeListener(new ChangeListener() {
                    public void stateChanged(ChangeEvent e) {
                        try {
                            createImage(v);
                            repaint();
                        } catch(IOException ex) {
                            Logger.getLogger(VTFTest.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                });
                this.add(lod, BorderLayout.WEST);
                frame = new JSpinner();
                frame.addChangeListener(new ChangeListener() {
                    public void stateChanged(ChangeEvent e) {
                        try {
                            createImage(v);
                            repaint();
                        } catch(IOException ex) {
                            Logger.getLogger(VTFTest.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                });
                this.add(frame, BorderLayout.EAST);
            }

            public void propertyChange(PropertyChangeEvent e) {
                String propertyName = e.getPropertyName();
                if(propertyName.equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)) {
                    try {
                        load((File) e.getNewValue());
                        repaint();
                    } catch(IOException ex) {
                        Logger.getLogger(VTFTest.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }

            @Override
            public void paintComponent(Graphics g) {
                g.setColor(bg);
                g.fillRect(0, 0, ACCSIZE, getHeight());
                if(image != null) {
                    g.drawImage(image, getWidth() / 2 - image.getWidth(null) / 2,
                                       getHeight() / 2 - image.getHeight(null) / 2, this);
                }
            }

            private void load(File selection) throws IOException {
                if(selection == null) {
                    return;
                }
                v = VTF.load(new FileInputStream(selection));
                if(v != null) {
                    frame.setValue(v.frameFirst);
                }
                createImage(v);
            }

                        private void createImage(VTF v) throws IOException {
                            if(v != null) {
                                Image i = v.getImage((Integer) lod.getValue(), (Integer) frame.getValue());
                                if(i != null) {
                                    f.setIconImage(v.getThumbImage());
                                    image = i;
                                    return;
                                }
                            }
                            image = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
                            
                        }

        }

        class VtfFileFilter extends FileFilter {

            Format vtfFormat;

            VtfFileFilter(Format format) {
                this.vtfFormat = format;
            }

            @Override
            public boolean accept(File file) {
                if(file.isDirectory()) {
                    return true;
                }
                VTF v = null;
                try {
                    v = VTF.load(new FileInputStream(file));
                } catch(IOException ex) {
                    Logger.getLogger(VTFTest.class.getName()).log(Level.SEVERE, null, ex);
                }
                if(v == null) {
                    return false;
                }
                if(vtfFormat == VTF.Format.IMAGE_FORMAT_NONE) {
                    return true;
                }
                return (v.format == vtfFormat);
            }

            @Override
            public String getDescription() {
                return "VTF (" + (vtfFormat != Format.IMAGE_FORMAT_NONE ? vtfFormat.name() : "All") + ")";
            }

        }

        JFileChooser chooser = new JFileChooser();
        FileFilter generic = new VtfFileFilter(Format.IMAGE_FORMAT_NONE);
        chooser.addChoosableFileFilter(generic);
        chooser.addChoosableFileFilter(new VtfFileFilter(Format.IMAGE_FORMAT_DXT1));
        chooser.addChoosableFileFilter(new VtfFileFilter(Format.IMAGE_FORMAT_DXT5));
        chooser.setFileFilter(generic);
        ImagePreviewPanel preview = new ImagePreviewPanel();
        chooser.setAccessory(preview);
        chooser.addPropertyChangeListener(preview);
        chooser.setControlButtonsAreShown(false);
        pane.add(chooser);
        f.setVisible(true);
        f.pack();
        f.setLocationRelativeTo(null);
    }

    public static void main(String... args) {
        new Thread(new Runnable() {
            public void run() {
                test();
            }
        }).start();
    }

}
