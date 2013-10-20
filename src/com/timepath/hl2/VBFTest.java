package com.timepath.hl2;

import com.timepath.hl2.io.VBF;
import com.timepath.hl2.io.VBF.BitmapGlyph;
import com.timepath.hl2.io.VTF;
import com.timepath.hl2.swing.VBFCanvas;
import com.timepath.plaf.x.filechooser.BaseFileChooser;
import com.timepath.plaf.x.filechooser.BaseFileChooser.ExtensionFilter;
import com.timepath.plaf.x.filechooser.NativeFileChooser;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.tree.*;

/**
 *
 * If one of the dimensions is 0, then do not use it to calculate the maxcharwidth/height
 *
 * @author TimePath
 */
public class VBFTest extends javax.swing.JFrame {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = Logger.getLogger(VBFTest.class.getName());

    private static class DisplayableCharacter {

        private final char c;

        DisplayableCharacter(int i) {
            this.c = (char) i;
        }

                public char getC() {
                    return c;
                }

        @Override
        public String toString() {
            Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
            boolean printable = ((!Character.isISOControl(c)) && c != KeyEvent.CHAR_UNDEFINED && block != null && block != Character.UnicodeBlock.SPECIALS);
            if(!printable) {
                return "0x" + (c < 16 ? "0" : "") + Integer.toHexString(c).toUpperCase();
            }
            return Character.toString(c);
        }

    }

    /**
     * Creates new form VBFTest
     */
    public VBFTest() {
        initComponents();

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {
                int choice = JOptionPane.showInternalConfirmDialog(VBFTest.this.getContentPane(),
                                                                   "Don't you want to save?");
                if(choice == JOptionPane.NO_OPTION) {
                    VBFTest.this.dispose();
                }
            }
        });

        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                JTree which = jTree1;
                if(jTree2.getSelectionRows() != null) {
                    which = jTree2;
                }
                jTree1.setSelectionRow(-1);
                jTree2.setSelectionRow(-1);
                BitmapGlyph seek = canvas.getSelected();
                if(seek == null) {
                    return;
                }
                for(int i = 0; i < which.getModel().getChildCount(which.getModel().getRoot()); i++) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) which.getModel().getChild(
                            which.getModel().getRoot(), i);
                    if(((BitmapGlyph) node.getUserObject()) == seek) {
                        which.setSelectionRow(node.getParent().getIndex(node) + 1);
                        break;
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if(currentGlyph == null) {
                    return;
                }
                Rectangle r = currentGlyph.getBounds();
                xSpinner.setValue(r.x);
                ySpinner.setValue(r.y);
                widthSpinner.setValue(r.width);
                heightSpinner.setValue(r.height);
            }
        });

        jTree2.setModel(jTree1.getModel());
        jTree1.setMinDragLevel(2);
        jTree1.setMinDropLevel(1);
        jTree1.setMaxDropLevel(1);
        jTree1.setDropMode(DropMode.ON);
        jTree2.setMinDragLevel(2);
        jTree2.setMinDropLevel(1);
        jTree2.setMaxDropLevel(1);
        jTree2.setDropMode(DropMode.ON);
        TreeCellRenderer renderer = new DefaultTreeCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
                                                          boolean expanded, boolean leaf, int row,
                                                          boolean hasFocus) {
                return super.getTreeCellRendererComponent(tree, value, sel, expanded,
                                                          ((TreeNode) value).getParent() != null && ((TreeNode) value).getParent() != tree.getModel().getRoot(),
                                                          row, hasFocus);
            }

            public TreeCellRenderer init() {
                this.setLeafIcon(null);
                return this;
            }
        }.init();
        jTree1.setCellRenderer(renderer);
        jTree2.setCellRenderer(renderer);
        final boolean spinners = false;
        //<editor-fold defaultstate="collapsed" desc="Spinners">

        xSpinner.addChangeListener(new ChangeListener() {
            private int old = 0;

            @Override
            public void stateChanged(ChangeEvent e) {
                if(data == null || currentGlyph == null) {
                    xSpinner.setValue(0);
                    return;
                }
                int current = ((Number) xSpinner.getValue()).intValue();
                currentGlyph.getBounds().x = current;
                doRepaint(Math.min(old, current), ((Number) ySpinner.getValue()).intValue(),
                          Math.max(old, current) + ((Number) widthSpinner.getValue()).intValue(),
                          ((Number) heightSpinner.getValue()).intValue());
                old = current;
                int wide = data.getWidth();
                if(image != null) {
                    wide = image.width;
                }
                if(spinners) {
                    ((SpinnerNumberModel) widthSpinner.getModel()).setMaximum(
                            wide - ((Number) xSpinner.getValue()).intValue());
                }
            }
        });
        widthSpinner.addChangeListener(new ChangeListener() {
            private int old = 0;

            @Override
            public void stateChanged(ChangeEvent e) {
                if(data == null || currentGlyph == null) {
                    widthSpinner.setValue(0);
                    return;
                }
                int current = ((Number) widthSpinner.getValue()).intValue();
                currentGlyph.getBounds().width = current;
                doRepaint(((Number) xSpinner.getValue()).intValue(),
                          ((Number) ySpinner.getValue()).intValue(), Math.max(old, current),
                          ((Number) heightSpinner.getValue()).intValue());
                old = current;
                int wide = data.getWidth();
                if(image != null) {
                    wide = image.width;
                }
                if(spinners) {
                    ((SpinnerNumberModel) xSpinner.getModel()).setMaximum(
                            wide - ((Number) widthSpinner.getValue()).intValue());
                }
            }
        });
        ySpinner.addChangeListener(new ChangeListener() {
            private int old = 0;

            @Override
            public void stateChanged(ChangeEvent e) {
                if(data == null || currentGlyph == null) {
                    ySpinner.setValue(0);
                    return;
                }
                int current = ((Number) ySpinner.getValue()).intValue();
                currentGlyph.getBounds().y = current;
                doRepaint(((Number) xSpinner.getValue()).intValue(), Math.min(old, current),
                          ((Number) widthSpinner.getValue()).intValue(),
                          Math.max(old, current) + ((Number) heightSpinner.getValue()).intValue());
                old = current;
                int high = data.getHeight();
                if(image != null) {
                    high = image.height;
                }
                if(spinners) {
                    ((SpinnerNumberModel) heightSpinner.getModel()).setMaximum(
                            high - ((Number) ySpinner.getValue()).intValue());
                }
            }
        });
        heightSpinner.addChangeListener(new ChangeListener() {
            private int old = 0;

            @Override
            public void stateChanged(ChangeEvent e) {
                if(data == null || currentGlyph == null) {
                    heightSpinner.setValue(0);
                    return;
                }
                int current = ((Number) heightSpinner.getValue()).intValue();
                currentGlyph.getBounds().height = current;
                doRepaint(((Number) xSpinner.getValue()).intValue(),
                          ((Number) ySpinner.getValue()).intValue(),
                          ((Number) widthSpinner.getValue()).intValue(), Math.max(old, current));
                old = current;
                int high = data.getHeight();
                if(image != null) {
                    high = image.height;
                }
                if(spinners) {
                    ((SpinnerNumberModel) ySpinner.getModel()).setMaximum(
                            high - ((Number) heightSpinner.getValue()).intValue());
                }
            }
        });
        //</editor-fold>
    }

    private void doRepaint(int x, int y, int w, int h) {
        this.canvas.repaint();//x, y, h, h);
    }

    private VBF data;

    private BitmapGlyph currentGlyph;

    private VTF image;

    private void load(String f) throws IOException {
        LOG.log(Level.INFO, "Loading {0}", f);
        VBFCanvas p = this.canvas;

        File vbf = new File(f + ".vbf");
        File vtf = new File(f + ".vtf");

        if(vbf.exists()) {
            data = VBF.load(new FileInputStream(vbf));
            p.setVBF(data);

            DefaultTreeModel model = (DefaultTreeModel) this.jTree1.getModel();
            DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
            root.removeAllChildren();
            for(BitmapGlyph g : data.getGlyphs()) {
                insertGlyph(model, g);
            }
        }

        if(vtf.exists()) {
            image = VTF.load(new FileInputStream(vtf));
            p.setVTF(image);
        }
        canvas.repaint();
    }

    private void insertGlyph(DefaultTreeModel model, BitmapGlyph glyph) {
        DefaultMutableTreeNode child = new DefaultMutableTreeNode(glyph);
        model.insertNodeInto(child, (MutableTreeNode) model.getRoot(), model.getChildCount(
                model.getRoot()));
        insertCharacters(model, child, glyph.getIndex());
        model.reload();
    }

    private void insertCharacters(DefaultTreeModel model, DefaultMutableTreeNode child, int g) {
        for(int i = 0; i < data.getTable().length; i++) {
            int glyphIndex = data.getTable()[i];
            if(glyphIndex != g) {
                continue;
            }
            DefaultMutableTreeNode sub = new DefaultMutableTreeNode(new DisplayableCharacter(i));
            model.insertNodeInto(sub, child, model.getChildCount(child));
        }
    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPopupMenu1 = new javax.swing.JPopupMenu();
        jMenuItem4 = new javax.swing.JMenuItem();
        jSplitPane1 = new javax.swing.JSplitPane();
        jPanel1 = new javax.swing.JPanel();
        jSplitPane2 = new javax.swing.JSplitPane();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTree1 = new com.timepath.swing.ReorderableJTree();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTree2 = new com.timepath.swing.ReorderableJTree();
        jPanel2 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        xSpinner = new javax.swing.JSpinner();
        ySpinner = new javax.swing.JSpinner();
        jPanel5 = new javax.swing.JPanel();
        widthSpinner = new javax.swing.JSpinner();
        heightSpinner = new javax.swing.JSpinner();
        jScrollPane1 = new javax.swing.JScrollPane();
        canvas = new com.timepath.hl2.swing.VBFCanvas();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        jMenuItem3 = new javax.swing.JMenuItem();

        jMenuItem4.setText("Copy");
        jMenuItem4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem4ActionPerformed(evt);
            }
        });
        jPopupMenu1.add(jMenuItem4);

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Bitmap Font Editor");
        setPreferredSize(new java.awt.Dimension(640, 480));

        jSplitPane1.setDividerLocation(250);
        jSplitPane1.setContinuousLayout(true);
        jSplitPane1.setPreferredSize(new java.awt.Dimension(360, 403));

        jSplitPane2.setDividerSize(2);
        jSplitPane2.setResizeWeight(0.5);
        jSplitPane2.setEnabled(false);

        javax.swing.tree.DefaultMutableTreeNode treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("Glyphs");
        jTree1.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        jTree1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTree1MouseClicked(evt);
            }
        });
        jTree1.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                treeInteraction(evt);
            }
        });
        jScrollPane2.setViewportView(jTree1);

        jSplitPane2.setLeftComponent(jScrollPane2);

        jTree2.setModel(jTree1.getModel());
        jTree2.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTree2MouseClicked(evt);
            }
        });
        jTree2.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                treeInteraction(evt);
            }
        });
        jScrollPane3.setViewportView(jTree2);

        jSplitPane2.setRightComponent(jScrollPane3);

        jPanel2.setPreferredSize(new java.awt.Dimension(200, 195));

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Position"));
        jPanel3.setLayout(new javax.swing.BoxLayout(jPanel3, javax.swing.BoxLayout.LINE_AXIS));

        xSpinner.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(0), Integer.valueOf(0), null, Integer.valueOf(1)));
        jPanel3.add(xSpinner);

        ySpinner.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(0), Integer.valueOf(0), null, Integer.valueOf(1)));
        jPanel3.add(ySpinner);

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder("Dimensions"));
        jPanel5.setLayout(new javax.swing.BoxLayout(jPanel5, javax.swing.BoxLayout.LINE_AXIS));

        widthSpinner.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(0), Integer.valueOf(0), null, Integer.valueOf(1)));
        jPanel5.add(widthSpinner);

        heightSpinner.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(0), Integer.valueOf(0), null, Integer.valueOf(1)));
        jPanel5.add(heightSpinner);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel5, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, 249, Short.MAX_VALUE)
            .addComponent(jSplitPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 249, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addComponent(jSplitPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 301, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, 148, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jSplitPane1.setLeftComponent(jPanel1);

        javax.swing.GroupLayout canvasLayout = new javax.swing.GroupLayout(canvas);
        canvas.setLayout(canvasLayout);
        canvasLayout.setHorizontalGroup(
            canvasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 373, Short.MAX_VALUE)
        );
        canvasLayout.setVerticalGroup(
            canvasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 449, Short.MAX_VALUE)
        );

        jScrollPane1.setViewportView(canvas);

        jSplitPane1.setRightComponent(jScrollPane1);

        getContentPane().add(jSplitPane1, java.awt.BorderLayout.CENTER);

        jMenu1.setText("File");

        jMenuItem1.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItem1.setMnemonic('O');
        jMenuItem1.setText("Open");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                open(evt);
            }
        });
        jMenu1.add(jMenuItem1);

        jMenuItem2.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItem2.setMnemonic('S');
        jMenuItem2.setText("Save");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                save(evt);
            }
        });
        jMenu1.add(jMenuItem2);

        jMenuBar1.add(jMenu1);

        jMenu2.setText("Edit");

        jMenuItem3.setText("Create glyph");
        jMenuItem3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                createGlyph(evt);
            }
        });
        jMenu2.add(jMenuItem3);

        jMenuBar1.add(jMenu2);

        setJMenuBar(jMenuBar1);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void open(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_open
        try {
            File[] fs = new NativeFileChooser().setParent(this).setTitle("Select vbf").addFilter(
                    new ExtensionFilter("Valve Bitmap Font", ".vbf")).addFilter(new ExtensionFilter(
                                    "Valve Texture File", ".vtf")).choose();
            if(fs == null) {
                return;
            }
            File f = fs[0];
            load(f.getPath().replace(".vbf", "").replace(".vtf", ""));
        } catch(IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_open

    private void save(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_save
        try {
            File[] fs = new NativeFileChooser().setParent(this).setTitle("Select save location").addFilter(
                    new ExtensionFilter("Valve Bitmap Font", ".vbf")).setDialogType(
                            BaseFileChooser.DialogType.SAVE_DIALOG).choose();
            if(fs == null) {
                return;
            }
            File f = fs[0];

            DefaultTreeModel model = (DefaultTreeModel) this.jTree1.getModel();
            MutableTreeNode root = (MutableTreeNode) model.getRoot();
            for(int i = 0; i < root.getChildCount(); i++) {
                DefaultMutableTreeNode m = (DefaultMutableTreeNode) root.getChildAt(i);
                BitmapGlyph g = (BitmapGlyph) m.getUserObject();
                for(int x = 0; x < m.getChildCount(); x++) {
                    DefaultMutableTreeNode character = (DefaultMutableTreeNode) m.getChildAt(x);
                    Object obj = character.getUserObject();
                    if(obj instanceof DisplayableCharacter) {
                        this.data.getTable()[((DisplayableCharacter) obj).getC()] = g.getIndex();
                    } else if(obj instanceof DefaultMutableTreeNode) { // XXX: hack
                        this.data.getTable()[((DisplayableCharacter) (((DefaultMutableTreeNode) obj).getUserObject())).getC()] = g.getIndex();
                    }
                }
            }

            data.save(f);
        } catch(IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_save

    private void createGlyph(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_createGlyph
        BitmapGlyph g = new BitmapGlyph();
        if(data == null) {
            data = new VBF();
            canvas.setVBF(data);
        }
        for(int i = 0; i < 256; i++) {
            if(!data.hasGlyph(i)) {
                g.setIndex((byte) i);
                break;
            }
            if(i == data.getGlyphs().size()) {
                g.setIndex((byte) (i + 1));
            }
        }
        data.getGlyphs().add(g);
        this.insertGlyph((DefaultTreeModel) jTree1.getModel(), g);
    }//GEN-LAST:event_createGlyph

    private char toCopy;

    private void mouseClicked(MouseEvent evt) {
        if(SwingUtilities.isRightMouseButton(evt)) {
            JTree jTree = ((JTree) evt.getComponent());
            TreePath clicked = jTree.getPathForLocation(evt.getX(), evt.getY());
            if(clicked == null) {
                return;
            }
            Object obj = clicked.getLastPathComponent();
            if(obj instanceof DefaultMutableTreeNode) {
                obj = ((DefaultMutableTreeNode) obj).getUserObject();
            }
            if(jTree.getSelectionPaths() == null || !Arrays.asList(jTree.getSelectionPaths()).contains(
                    clicked)) {
                jTree.setSelectionPath(clicked);
            }
            TreePath[] paths = jTree.getSelectionPaths();
            for(TreePath p : paths) {
                if(!(p.getLastPathComponent() instanceof DefaultMutableTreeNode)) {
                    return;
                }
                Object userObject = ((DefaultMutableTreeNode) p.getLastPathComponent()).getUserObject();
                if(userObject instanceof DisplayableCharacter) {
                    toCopy = ((DisplayableCharacter) userObject).getC();
                    jPopupMenu1.show(jTree, evt.getX(), evt.getY());
                }
            }
        }
    }

    private void jMenuItem4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem4ActionPerformed
        StringSelection selection = new StringSelection(String.valueOf(toCopy));
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
    }//GEN-LAST:event_jMenuItem4ActionPerformed

    private void jTree1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTree1MouseClicked
        mouseClicked(evt);
    }//GEN-LAST:event_jTree1MouseClicked

    private void jTree2MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTree2MouseClicked
        mouseClicked(evt);
    }//GEN-LAST:event_jTree2MouseClicked

    private void treeInteraction(javax.swing.event.TreeSelectionEvent evt) {//GEN-FIRST:event_treeInteraction
        TreePath selection = evt.getNewLeadSelectionPath();
        if(selection == null) {
            return;
        }
        JTree other = evt.getSource() == jTree1 ? jTree2 : evt.getSource() == jTree2 ? jTree1 : null;
        if(other != null) {
            other.setSelectionRow(-1);
        }
        Object node = selection.getLastPathComponent();
        if(!(node instanceof DefaultMutableTreeNode)) {
            return;
        }
        Object obj = ((DefaultMutableTreeNode) node).getUserObject();
        if(!(obj instanceof BitmapGlyph)) {
            return;
        }
        currentGlyph = (BitmapGlyph) obj;
        canvas.select(currentGlyph);
        if(currentGlyph.getBounds() == null) {
            currentGlyph.setBounds(new Rectangle());
        }
        Rectangle r = currentGlyph.getBounds();
        xSpinner.setValue(r.x);
        ySpinner.setValue(r.y);
        widthSpinner.setValue(r.width);
        heightSpinner.setValue(r.height);
    }//GEN-LAST:event_treeInteraction

    /**
     * @param args the command line arguments
     */
    public static void main(String... args) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new VBFTest().setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private com.timepath.hl2.swing.VBFCanvas canvas;
    private javax.swing.JSpinner heightSpinner;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JMenuItem jMenuItem4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPopupMenu jPopupMenu1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JSplitPane jSplitPane2;
    private com.timepath.swing.ReorderableJTree jTree1;
    private com.timepath.swing.ReorderableJTree jTree2;
    private javax.swing.JSpinner widthSpinner;
    private javax.swing.JSpinner xSpinner;
    private javax.swing.JSpinner ySpinner;
    // End of variables declaration//GEN-END:variables
}
