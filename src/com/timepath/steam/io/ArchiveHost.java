package com.timepath.steam.io;

import com.timepath.hl2.io.VTF;
import com.timepath.steam.io.storage.ACF;
import com.timepath.vfs.SimpleVFile;
import com.timepath.vfs.ftp.FTPFS;
import com.timepath.vfs.http.HTTPFS;
import java.awt.Image;
import java.awt.image.RenderedImage;
import java.io.*;
import java.lang.ref.SoftReference;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 *
 * @author TimePath
 */
public class ArchiveHost {

    private static final Logger LOG = Logger.getLogger(ArchiveHost.class.getName());

    public static void main(String... args) {
        int appID = 440;
        try {
            final ACF acf = ACF.fromManifest(appID);
            Collection<? extends SimpleVFile> files = acf.list();
            for(SimpleVFile f : files) {
                for(final SimpleVFile found : f.find(".vtf")) {
                    SimpleVFile png = new SimpleVFile() {

                        private SoftReference<byte[]> data = new SoftReference<byte[]>(null);

                        @Override
                        public String getName() {
                            return found.getName().replace(".vtf", ".png");
                        }

                        @Override
                        public boolean isDirectory() {
                            return false;
                        }

                        @Override
                        public InputStream stream() {
                            byte[] arr = data.get();
                            if(arr == null) {
                                try {
                                    LOG.log(Level.INFO, "Streaming {0}", found);
                                    final VTF v = VTF.load(found.stream());
                                    if(v == null) {
                                        return null;
                                    }
                                    Image i = v.getImage(0); // v.mipCount - 1
                                    if(i == null) {
                                        return null;
                                    }
                                    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                    ImageIO.write((RenderedImage) i, "png", baos);
                                    arr = baos.toByteArray();
                                    data = new SoftReference<byte[]>(arr);
                                } catch(IOException ex) {
                                    LOG.log(Level.SEVERE, null, ex);
                                }
                            }
                            return new ByteArrayInputStream(arr);
                        }
                    };
                    found.getParent().add(png);
                }
            }

            try {
                HTTPFS http = new HTTPFS();
                http.copyFrom(files);
                new Thread(http).start();
            } catch(IOException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
            try {
                FTPFS ftp = new FTPFS();
                ftp.copyFrom(files);
                new Thread(ftp).start();
            } catch(IOException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        } catch(FileNotFoundException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }

        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                JOptionPane.showMessageDialog(null,
                                              "Navigate to ftp://localhost:2121. The files will stop being hosted when you close all running instances",
                                              "Files hosted",
                                              JOptionPane.INFORMATION_MESSAGE,
                                              null);
            }

        });

    }

}
