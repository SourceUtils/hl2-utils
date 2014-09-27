package com.timepath.hl2;

import com.timepath.hl2.io.image.ImageFormat;
import com.timepath.hl2.io.image.VTF;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author TimePath
 */
class VTFTest {

    private static final Logger LOG = Logger.getLogger(VTFTest.class.getName());

    private VTFTest() {
    }

    public static void main(String... args) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                test();
            }
        }).start();
    }

    public static void test() {
        @NotNull JFrame frame = new JFrame("VTF Loader");
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        @NotNull JScrollPane jsp = new JScrollPane();
        jsp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        jsp.getVerticalScrollBar().setUnitIncrement(64);
        frame.add(jsp);
        @NotNull JPanel pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.PAGE_AXIS));
        jsp.setViewportView(pane);
        @NotNull JFileChooser chooser = new JFileChooser();
        @NotNull FileFilter generic = new VtfFileFilter(ImageFormat.IMAGE_FORMAT_UNKNOWN);
        chooser.addChoosableFileFilter(generic);
        chooser.addChoosableFileFilter(new VtfFileFilter(ImageFormat.IMAGE_FORMAT_DXT1));
        chooser.addChoosableFileFilter(new VtfFileFilter(ImageFormat.IMAGE_FORMAT_DXT5));
        chooser.addChoosableFileFilter(new AntiVtfFileFilter(ImageFormat.IMAGE_FORMAT_DXT1,
                ImageFormat.IMAGE_FORMAT_DXT3,
                ImageFormat.IMAGE_FORMAT_DXT5));
        chooser.addChoosableFileFilter(new AntiVtfFileFilter("RGB",
                ImageFormat.IMAGE_FORMAT_DXT1,
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
                ImageFormat.IMAGE_FORMAT_RGBA8888));
        chooser.setFileFilter(generic);
        @NotNull ImagePreviewPanel preview = new ImagePreviewPanel(frame);
        chooser.setAccessory(preview);
        chooser.addPropertyChangeListener(preview);
        chooser.setControlButtonsAreShown(false);
        pane.add(chooser);
        frame.setVisible(true);
        frame.pack();
        frame.setLocationRelativeTo(null);
    }

    @SuppressWarnings("serial")
    static class ImagePreviewPanel extends JPanel implements PropertyChangeListener {

        private static final int ACCSIZE = 256;
        private static final Logger LOG = Logger.getLogger(ImagePreviewPanel.class.getName());
        @NotNull
        private final JSpinner frame;
        @NotNull
        private final JSpinner lod;
        private Color bgColor;
        @Nullable
        private Image image;
        @Nullable
        private VTF vtf;
        private JFrame parentFrame;

        ImagePreviewPanel(JFrame frame) {
            parentFrame = frame;
            setPreferredSize(new Dimension(ACCSIZE, -1));
            bgColor = Color.PINK;
            lod = new JSpinner();
            lod.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    try {
                        createImage(vtf);
                        repaint();
                    } catch (IOException ex) {
                        LOG.log(Level.SEVERE, null, ex);
                    }
                }
            });
            add(lod, BorderLayout.WEST);
            this.frame = new JSpinner();
            this.frame.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    try {
                        createImage(vtf);
                        repaint();
                    } catch (IOException ex) {
                        LOG.log(Level.SEVERE, null, ex);
                    }
                }
            });
            add(this.frame, BorderLayout.EAST);
        }

        private void createImage(@Nullable VTF v) throws IOException {
            if (v != null) {
                @Nullable Image img = v.getImage((Integer) lod.getValue(), (Integer) frame.getValue());
                if (img != null) {
                    parentFrame.setIconImage(v.getThumbImage());
                    image = img;
                    return;
                }
            }
            image = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        }

        @Override
        public void paintComponent(@NotNull Graphics g) {
            g.setColor(bgColor);
            g.fillRect(0, 0, ACCSIZE, getHeight());
            if (image != null) {
                g.drawImage(image,
                        (getWidth() / 2) - (image.getWidth(null) / 2),
                        (getHeight() / 2) - (image.getHeight(null) / 2),
                        this);
            }
        }

        @Override
        public void propertyChange(@NotNull PropertyChangeEvent evt) {
            String propertyName = evt.getPropertyName();
            if (propertyName.equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)) {
                try {
                    load((File) evt.getNewValue());
                    repaint();
                } catch (IOException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                }
            }
        }

        private void load(@Nullable File selection) throws IOException, FileNotFoundException {
            if (selection == null) {
                return;
            }
            vtf = VTF.load(new FileInputStream(selection));
            if (vtf != null) {
                frame.setValue(vtf.getFrameFirst());
            }
            createImage(vtf);
        }
    }

    static class VtfFileFilter extends FileFilter {

        private static final Logger LOG = Logger.getLogger(VtfFileFilter.class.getName());
        private ImageFormat vtfFormat;

        VtfFileFilter(ImageFormat format) {
            vtfFormat = format;
        }

        @Override
        public boolean accept(@NotNull File f) {
            if (f.isDirectory()) {
                return true;
            }
            @Nullable VTF v = null;
            try {
                v = VTF.load(new FileInputStream(f));
            } catch (IOException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
            if (v == null) {
                return false;
            }
            if (vtfFormat == ImageFormat.IMAGE_FORMAT_UNKNOWN) {
                return true;
            }
            return v.getFormat() == vtfFormat;
        }

        @NotNull
        @Override
        public String getDescription() {
            return "VTF (" + ((vtfFormat == ImageFormat.IMAGE_FORMAT_UNKNOWN) ? "All" : vtfFormat.name()) + ')';
        }
    }

    static class AntiVtfFileFilter extends FileFilter {

        private static final Logger LOG = Logger.getLogger(AntiVtfFileFilter.class.getName());
        private ImageFormat[] ignored;
        private String name;

        AntiVtfFileFilter(ImageFormat... formats) {
            this(null, formats);
        }

        AntiVtfFileFilter(String name, ImageFormat... formats) {
            this.name = name;
            ignored = formats;
        }

        @Override
        public boolean accept(@NotNull File f) {
            if (f.isDirectory()) {
                return true;
            }
            @Nullable VTF v = null;
            try {
                v = VTF.load(new FileInputStream(f));
            } catch (IOException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
            if (v == null) {
                return false;
            }
            for (ImageFormat format : ignored) {
                if (v.getFormat() == format) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public String getDescription() {
            if (name != null) {
                return name;
            }
            return "VTF (Not " + Arrays.toString(ignored) + ')';
        }
    }
}
