package com.timepath.hl2;

import com.timepath.hl2.io.bsp.BSP;
import com.timepath.hl2.io.image.VTF;
import com.timepath.steam.io.storage.ACF;
import com.timepath.steam.io.storage.Files;
import com.timepath.vfs.SimpleVFile;
import com.timepath.vfs.SimpleVFile.MissingFileHandler;
import com.timepath.vfs.ftp.FTPFS;
import com.timepath.vfs.fuse.FUSEFS;
import com.timepath.vfs.http.HTTPFS;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    private ArchiveHost() {
    }

    public static void main(String[] args) {
        try {
            Class.forName(BSP.class.getName());
            Files.registerMissingFileHandler(new MissingFileHandler() {
                @Nullable
                @Override
                public SimpleVFile handle(@NotNull final SimpleVFile parent, @NotNull final String name) {
                    if (!name.endsWith(".png")) return null;
                    @Nullable final SimpleVFile vtf = parent.get(name.replace(".png", ".vtf"));
                    if (vtf == null) return null;
                    return new SimpleVFile() {
                        @NotNull
                        private SoftReference<byte[]> data = new SoftReference<>(null);

                        @NotNull
                        @Override
                        public String getName() {
                            return name;
                        }

                        @Override
                        public boolean isDirectory() {
                            return false;
                        }

                        @Nullable
                        @Override
                        public InputStream openStream() {
                            @Nullable byte[] arr = data.get();
                            if (arr == null) {
                                try {
                                    LOG.log(Level.INFO, "Converting {0}...", vtf);
                                    @Nullable VTF v = VTF.load(vtf.openStream());
                                    if (v == null) {
                                        return null;
                                    }
                                    @Nullable Image image = v.getImage(Math.min(1, v.getMipCount() - 1));
                                    if (image == null) {
                                        return null;
                                    }
                                    @NotNull ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                    ImageIO.write((RenderedImage) image, "png", baos);
                                    arr = baos.toByteArray();
                                    data = new SoftReference<>(arr);
                                } catch (IOException e) {
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
            @Nullable ACF acf = ACF.fromManifest(appID);
            @Nullable Collection<? extends SimpleVFile> files = acf.list();
            try {
                @NotNull HTTPFS http = new HTTPFS();
                http.addAll(files);
                new Thread(http).start();
            } catch (IOException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
            try {
                @NotNull FTPFS ftp = new FTPFS();
                ftp.addAll(files);
                new Thread(ftp).start();
            } catch (IOException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
            @NotNull FUSEFS fuse = new FUSEFS("test");
            fuse.addAll(files);
            new Thread(fuse).start();
        } catch (@NotNull ClassNotFoundException | IOException ex) {
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
                        null);
            }
        });
    }
}
