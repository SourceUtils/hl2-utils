package com.timepath.steam.io;

import com.timepath.hl2.io.image.VTF;
import com.timepath.steam.io.storage.ACF;
import com.timepath.vfs.SimpleVFile;
import com.timepath.vfs.ftp.FTPFS;
import com.timepath.vfs.fuse.FUSEFS;
import com.timepath.vfs.http.HTTPFS;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.RenderedImage;
import java.io.*;
import java.lang.ref.SoftReference;
import java.util.Collection;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author TimePath
 */
class ArchiveHost {

    private static final Logger LOG = Logger.getLogger(ArchiveHost.class.getName());

    private ArchiveHost() {}

    public static void main(String... args) {
        try {
            int appID = 440;
            ACF acf = ACF.fromManifest(appID);
            Collection<? extends SimpleVFile> files = acf.list();
            final Semaphore available = new Semaphore(Runtime.getRuntime().availableProcessors() * 2, true);
            for(SimpleVFile file : files) {
                for(final SimpleVFile found : file.find(".vtf")) {
                    SimpleVFile png = new SimpleVFile() {
                        private SoftReference<byte[]> data = new SoftReference<>(null);

                        @Override
                        public String getName() {
                            return found.getName().replace(".vtf", ".png");
                        }

                        @Override
                        public boolean isDirectory() {
                            return false;
                        }

                        @Override
                        public InputStream openStream() {
                            byte[] arr = data.get();
                            if(arr == null) {
                                boolean release = false;
                                try {
                                    available.acquire();
                                    release = true;
                                    LOG.log(Level.INFO, "Converting {0}...", found);
                                    VTF v = VTF.load(found.openStream());
                                    if(v == null) {
                                        return null;
                                    }
                                    Image image = v.getImage(Math.min(1, v.getMipCount() - 1));
                                    if(image == null) {
                                        return null;
                                    }
                                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                    ImageIO.write((RenderedImage) image, "png", baos);
                                    arr = baos.toByteArray();
                                    data = new SoftReference<>(arr);
                                } catch(IOException | InterruptedException ex) {
                                    LOG.log(Level.SEVERE, null, ex);
                                } finally {
                                    LOG.log(Level.INFO, "Converted {0}", found);
                                    if(release) {
                                        available.release();
                                    }
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
                http.addAll(files);
                new Thread(http).start();
            } catch(IOException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
            try {
                FTPFS ftp = new FTPFS();
                ftp.addAll(files);
                new Thread(ftp).start();
            } catch(IOException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
            FUSEFS fuse = new FUSEFS("test");
            fuse.addAll(files);
            new Thread(fuse).start();
        } catch(IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JOptionPane.showMessageDialog(null,
                                              "Navigate to ftp://localhost:2121. The files will stop being hosted " +
                                              "when you close all running instances",
                                              "Files hosted",
                                              JOptionPane.INFORMATION_MESSAGE,
                                              null
                                             );
            }
        });
    }
}
