package com.timepath.steam.io;

import com.timepath.hl2.io.bsp.BSP;
import com.timepath.hl2.io.image.VTF;
import com.timepath.steam.io.storage.ACF;
import com.timepath.steam.io.storage.Files;
import com.timepath.vfs.SimpleVFile;
import com.timepath.vfs.SimpleVFile.MissingFileHandler;
import com.timepath.vfs.ftp.FTPFS;
import com.timepath.vfs.fuse.FUSEFS;
import com.timepath.vfs.http.HTTPFS;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.Collection;
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
            Class.forName(BSP.class.getName());
            Files.registerMissingFileHandler(new MissingFileHandler() {
                @Override
                public SimpleVFile handle(final SimpleVFile parent, final String name) {
                    if(!name.endsWith(".png")) return null;
                    final SimpleVFile vtf = parent.get(name.replace(".png", ".vtf"));
                    if(vtf == null) return null;
                    return new SimpleVFile() {
                        private SoftReference<byte[]> data = new SoftReference<>(null);

                        @Override
                        public String getName() {
                            return name;
                        }

                        @Override
                        public boolean isDirectory() {
                            return false;
                        }

                        @Override
                        public InputStream openStream() {
                            byte[] arr = data.get();
                            if(arr == null) {
                                try {
                                    LOG.log(Level.INFO, "Converting {0}...", vtf);
                                    VTF v = VTF.load(vtf.openStream());
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
                                } catch(IOException e) {
                                    LOG.log(Level.SEVERE, null, e);
                                } finally {
                                    LOG.log(Level.INFO, "Converted {0}", vtf);
                                }
                            }
                            return new ByteArrayInputStream(arr);
                        }
                    };
                }
            });
            int appID = 440;
            ACF acf = ACF.fromManifest(appID);
            Collection<? extends SimpleVFile> files = acf.list();
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
        } catch(ClassNotFoundException | IOException ex) {
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
