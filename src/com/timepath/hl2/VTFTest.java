package com.timepath.hl2;

import com.timepath.hl2.io.image.ImageFormat;
import com.timepath.hl2.io.image.VTF;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
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

    public static void main(String... args) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                test();
            }
        }).start();
    }

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

            private Color bg;

            private final JSpinner frame;

            private Image image;

            private final JSpinner lod;

            private VTF v;

            ImagePreviewPanel() {
                setPreferredSize(new Dimension(ACCSIZE, -1));
                bg = getBackground();
                bg = Color.PINK;
                lod = new JSpinner();
                lod.addChangeListener(new ChangeListener() {
                    @Override
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
                    @Override
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

            @Override
            public void paintComponent(Graphics g) {
                g.setColor(bg);
                g.fillRect(0, 0, ACCSIZE, getHeight());
                if(image != null) {
                    g.drawImage(image, getWidth() / 2 - image.getWidth(null) / 2,
                                getHeight() / 2 - image.getHeight(null) / 2, this);
                }
            }

            @Override
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

            private void load(File selection) throws IOException {
                if(selection == null) {
                    return;
                }
                v = VTF.load(new FileInputStream(selection));
                if(v != null) {
                    frame.setValue(v.getFrameFirst());
                }
                createImage(v);
            }

        }

        class VtfFileFilter extends FileFilter {

            private ImageFormat vtfFormat;

            VtfFileFilter(ImageFormat format) {
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
                if(vtfFormat == ImageFormat.IMAGE_FORMAT_UNKNOWN) {
                    return true;
                }
                return (v.getFormat() == vtfFormat);
            }

            @Override
            public String getDescription() {
                return "VTF (" + (vtfFormat != ImageFormat.IMAGE_FORMAT_UNKNOWN ? vtfFormat.name() : "All") + ")";
            }

        }

        class AntiVtfFileFilter extends FileFilter {

            private ImageFormat[] ignored;

            private String name;

            AntiVtfFileFilter(ImageFormat... formats) {
                this(null, formats);
            }

            AntiVtfFileFilter(String name, ImageFormat... formats) {
                this.name = name;
                this.ignored = formats;
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
                for(ImageFormat f : ignored) {
                    if(v.getFormat() == f) {
                        return false;
                    }
                }
                return true;
            }

            @Override
            public String getDescription() {
                if(name != null) {
                    return name;
                }
                return "VTF (Not " + Arrays.toString(ignored) + ")";
            }

        }

        JFileChooser chooser = new JFileChooser();
        FileFilter generic = new VtfFileFilter(ImageFormat.IMAGE_FORMAT_UNKNOWN);
        chooser.addChoosableFileFilter(generic);
        chooser.addChoosableFileFilter(new VtfFileFilter(ImageFormat.IMAGE_FORMAT_DXT1));
        chooser.addChoosableFileFilter(new VtfFileFilter(ImageFormat.IMAGE_FORMAT_DXT5));
        chooser.addChoosableFileFilter(new AntiVtfFileFilter(ImageFormat.IMAGE_FORMAT_DXT1,
                                                             ImageFormat.IMAGE_FORMAT_DXT3,
                                                             ImageFormat.IMAGE_FORMAT_DXT5
        ));
        chooser.addChoosableFileFilter(new AntiVtfFileFilter("RGB", ImageFormat.IMAGE_FORMAT_DXT1,
                                                             ImageFormat.IMAGE_FORMAT_DXT3,
                                                             ImageFormat.IMAGE_FORMAT_DXT5,
                                                             ImageFormat.IMAGE_FORMAT_ABGR8888,
                                                             ImageFormat.IMAGE_FORMAT_ARGB8888,
                                                             ImageFormat.IMAGE_FORMAT_BGRA8888,
                                                             ImageFormat.IMAGE_FORMAT_BGRA4444,
                                                             ImageFormat.IMAGE_FORMAT_BGRA5551,
                                                             ImageFormat.IMAGE_FORMAT_RGBA16161616,
                                                             ImageFormat.IMAGE_FORMAT_RGBA16161616F,
                                                             ImageFormat.IMAGE_FORMAT_RGBA32323232F,
                                                             ImageFormat.IMAGE_FORMAT_RGBA8888
        ));
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

}
