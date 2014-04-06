package com.timepath.hl2;

import com.timepath.DataUtils;
import com.timepath.plaf.OS;
import com.timepath.plaf.x.filechooser.NativeFileChooser;
import com.timepath.steam.SteamUtils;
import com.timepath.steam.io.BVDF;
import com.timepath.steam.io.Blob;
import com.timepath.steam.io.VDF1;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

/**
 *
 * @author TimePath
 */
@SuppressWarnings("serial")
public class DataTest extends javax.swing.JFrame {

    private static final Logger LOG = Logger.getLogger(DataTest.class.getName());

    /**
     * Creates new form VDFTest
     */
    public DataTest() {
        initComponents();

        //<editor-fold defaultstate="collapsed" desc="Drag+drop">
        this.setDropTarget(new DropTarget() {
            @Override
            public void drop(DropTargetDropEvent e) {
                try {
                    DropTargetContext context = e.getDropTargetContext();
                    e.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                    Transferable t = e.getTransferable();
                    File file = null;
                    if(OS.isLinux()) {
                        DataFlavor nixFileDataFlavor = new DataFlavor(
                            "text/uri-list;class=java.lang.String");
                        String data = (String) t.getTransferData(nixFileDataFlavor);
                        for(StringTokenizer st = new StringTokenizer(data, "\r\n"); st.hasMoreTokens();) {
                            String token = st.nextToken().trim();
                            if(token.startsWith("#") || token.length() == 0) {
                                // comment line, by RFC 2483
                                continue;
                            }
                            try {
                                file = new File(new URI(token));
                            } catch(Exception ex) {
                            }
                        }
                    } else {
                        Object data = t.getTransferData(DataFlavor.javaFileListFlavor);
                        if(data instanceof List) {
                            for(Object o : ((Iterable<? extends Object>) data)) {
                                if(o instanceof File) {
                                    file = (File) o;
                                }
                            }
                        }
                    }
                    if(file != null) {
                        open(file);
                    }
                } catch(ClassNotFoundException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                } catch(InvalidDnDOperationException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                } catch(UnsupportedFlavorException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                } catch(IOException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                } finally {
                    e.dropComplete(true);
                    repaint();
                }
            }
        });
        //</editor-fold>
    }

    private void open(final File f) {
        if(f == null) {
            LOG.info("File is null");
            return;
        } else {
            LOG.log(Level.INFO, "File is {0}", f);
        }
        final DefaultTreeModel model = ((DefaultTreeModel) jTree1.getModel());
        final DefaultMutableTreeNode pseudo = new DefaultMutableTreeNode(f.getPath());
        model.setRoot(pseudo);

        new SwingWorker<DefaultMutableTreeNode, Void>() {
            @Override
            protected DefaultMutableTreeNode doInBackground() throws Exception {
                DefaultMutableTreeNode n = null;
                try {
                    if(f.getName().toLowerCase().endsWith(".blob")) {
                        Blob bin = new Blob();
                        bin.readExternal(DataUtils.mapFile(f));
                        n = bin.getRoot();
                    } else if(f.getName().toLowerCase().matches("^.*(vdf|res)$")) {
                        if(!VDF1.isBinary(f)) {
                            VDF1 res = new VDF1();
                            res.readExternal(new FileInputStream(f));
                            n = res.getRoot();
                        } else {
                            BVDF bin = new BVDF();
                            bin.readExternal(DataUtils.mapFile(f));
                            n = bin.getRoot();
                        }
                    } else if(f.getName().toLowerCase().endsWith(".bin")) {
                        BVDF bin = new BVDF();
                        bin.readExternal(DataUtils.mapFile(f));
                        n = bin.getRoot();
                    } else {
                        JOptionPane.showMessageDialog(DataTest.this,
                                                      MessageFormat.format("{0} is not supported", f.getAbsolutePath()),
                                                      "Invalid file", JOptionPane.ERROR_MESSAGE);
                    }
                } catch(StackOverflowError e) {
                    LOG.warning("Stack Overflow");
                } catch(Exception e) {
                    e.printStackTrace();
                }
                return n;
            }

            @Override
            protected void done() {
                try {
                    DefaultMutableTreeNode n = get();
                    if(n != null) {
                        pseudo.add(n);
                    }
                    model.reload();
//                    TreeUtils.expand(DataTest.this.jTree1);
                } catch(InterruptedException ex) {
                    Logger.getLogger(DataTest.class.getName()).log(Level.SEVERE, null, ex);
                } catch(ExecutionException ex) {
                    Logger.getLogger(DataTest.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }.execute();

    }

    /**
     * @param args the command line arguments
     */
    public static void main(String... args) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new DataTest().setVisible(true);
            }
        });
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jTree1 = new javax.swing.JTree();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();
        jMenuItem3 = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Data viewer");
        setMinimumSize(new java.awt.Dimension(300, 300));

        javax.swing.tree.DefaultMutableTreeNode treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("root");
        jTree1.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        jTree1.setEditable(true);
        jTree1.setLargeModel(true);
        jTree1.setRootVisible(false);
        jScrollPane1.setViewportView(jTree1);

        getContentPane().add(jScrollPane1, java.awt.BorderLayout.CENTER);

        jMenu1.setText("File");

        jMenuItem1.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItem1.setMnemonic('O');
        jMenuItem1.setText("Open");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openVDF(evt);
            }
        });
        jMenu1.add(jMenuItem1);

        jMenuItem2.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_I, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItem2.setMnemonic('A');
        jMenuItem2.setText("AppInfo");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                appInfo(evt);
            }
        });
        jMenu1.add(jMenuItem2);

        jMenuItem3.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItem3.setMnemonic('P');
        jMenuItem3.setText("PackageInfo");
        jMenuItem3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                packageInfo(evt);
            }
        });
        jMenu1.add(jMenuItem3);

        jMenuBar1.add(jMenu1);

        setJMenuBar(jMenuBar1);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void openVDF(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openVDF
        try {
            File[] fs = new NativeFileChooser().setParent(this).setTitle("Open VDF").choose();
            if(fs == null) {
                return;
            }
            open(fs[0]);
        } catch(IOException ex) {
            Logger.getLogger(DataTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_openVDF

    private void appInfo(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_appInfo
        open(new File(SteamUtils.getSteam() + "/appcache/appinfo.vdf"));
    }//GEN-LAST:event_appInfo

    private void packageInfo(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_packageInfo
        open(new File(SteamUtils.getSteam() + "/appcache/packageinfo.vdf"));
    }//GEN-LAST:event_packageInfo

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTree jTree1;
    // End of variables declaration//GEN-END:variables

}
