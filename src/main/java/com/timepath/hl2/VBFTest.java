package com.timepath.hl2;

import com.timepath.hl2.io.font.VBF;
import com.timepath.hl2.io.image.VTF;
import com.timepath.hl2.swing.VBFCanvas;
import com.timepath.plaf.x.filechooser.BaseFileChooser;
import com.timepath.plaf.x.filechooser.NativeFileChooser;
import com.timepath.swing.ReorderableJTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author TimePath
 */
@SuppressWarnings("serial")
class VBFTest extends JFrame {

    private static final Logger LOG = Logger.getLogger(VBFTest.class.getName());
    private VBFCanvas canvas;
    private VBF.BitmapGlyph currentGlyph;
    private VBF data;
    private JSpinner heightSpinner;
    @Nullable
    private VTF image;
    private JPopupMenu jPopupMenu1;
    private ReorderableJTree jTree1;
    private ReorderableJTree jTree2;
    private char toCopy;
    private JSpinner widthSpinner;
    private JSpinner xSpinner;
    private JSpinner ySpinner;

    /**
     * Creates new form VBFTest
     */
    private VBFTest() {
        initComponents();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int choice = JOptionPane.showInternalConfirmDialog(getContentPane(), "Do you want to save?");
                if (choice == JOptionPane.NO_OPTION) {
                    dispose();
                }
            }
        });
        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                JTree which = jTree1;
                if (jTree2.getSelectionRows() != null) {
                    which = jTree2;
                }
                jTree1.setSelectionRow(-1);
                jTree2.setSelectionRow(-1);
                @Nullable VBF.BitmapGlyph seek = canvas.getSelected();
                if (seek == null) {
                    return;
                }
                for (int i = 0; i < which.getModel().getChildCount(which.getModel().getRoot()); i++) {
                    @NotNull DefaultMutableTreeNode node = (DefaultMutableTreeNode) which.getModel()
                            .getChild(which.getModel().getRoot(), i);
                    if (node.getUserObject() == seek) {
                        which.setSelectionRow(node.getParent().getIndex(node) + 1);
                        break;
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (currentGlyph == null) {
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
        @NotNull TreeCellRenderer renderer = new DefaultTreeCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getTreeCellRendererComponent(@NotNull JTree tree,
                                                          @NotNull Object value,
                                                          boolean sel,
                                                          boolean expanded,
                                                          boolean leaf,
                                                          int row,
                                                          boolean hasFocus) {
                return super.getTreeCellRendererComponent(tree,
                        value,
                        sel,
                        expanded,
                        (((TreeNode) value).getParent() != null) &&
                                !((TreeNode) value).getParent().equals(tree.getModel().getRoot()),
                        row,
                        hasFocus
                );
            }

            @NotNull
            public TreeCellRenderer init() {
                setLeafIcon(null);
                return this;
            }
        }.init();
        jTree1.setCellRenderer(renderer);
        jTree2.setCellRenderer(renderer);
        final boolean spinners = false;
        xSpinner.addChangeListener(new ChangeListener() {
            private int old;

            @Override
            public void stateChanged(ChangeEvent e) {
                if ((data == null) || (currentGlyph == null)) {
                    xSpinner.setValue(0);
                    return;
                }
                int current = ((Number) xSpinner.getValue()).intValue();
                currentGlyph.getBounds().x = current;
                doRepaint(Math.min(old, current),
                        ((Number) ySpinner.getValue()).intValue(),
                        Math.max(old, current) + ((Number) widthSpinner.getValue()).intValue(),
                        ((Number) heightSpinner.getValue()).intValue());
                old = current;
                int wide = (image != null) ? image.getWidth() : data.getWidth();
                if (spinners) {
                    ((SpinnerNumberModel) widthSpinner.getModel()).setMaximum(
                            wide - ((Number) xSpinner.getValue()).intValue());
                }
            }
        });
        widthSpinner.addChangeListener(new ChangeListener() {
            private int old;

            @Override
            public void stateChanged(ChangeEvent e) {
                if ((data == null) || (currentGlyph == null)) {
                    widthSpinner.setValue(0);
                    return;
                }
                int current = ((Number) widthSpinner.getValue()).intValue();
                currentGlyph.getBounds().width = current;
                doRepaint(((Number) xSpinner.getValue()).intValue(),
                        ((Number) ySpinner.getValue()).intValue(),
                        Math.max(old, current),
                        ((Number) heightSpinner.getValue()).intValue());
                old = current;
                int wide = (image != null) ? image.getWidth() : data.getWidth();
                if (spinners) {
                    ((SpinnerNumberModel) xSpinner.getModel()).setMaximum(
                            wide - ((Number) widthSpinner.getValue()).intValue());
                }
            }
        });
        ySpinner.addChangeListener(new ChangeListener() {
            private int old;

            @Override
            public void stateChanged(ChangeEvent e) {
                if ((data == null) || (currentGlyph == null)) {
                    ySpinner.setValue(0);
                    return;
                }
                int current = ((Number) ySpinner.getValue()).intValue();
                currentGlyph.getBounds().y = current;
                doRepaint(((Number) xSpinner.getValue()).intValue(),
                        Math.min(old, current),
                        ((Number) widthSpinner.getValue()).intValue(),
                        Math.max(old, current) + ((Number) heightSpinner.getValue()).intValue());
                old = current;
                int high = (image != null) ? image.getHeight() : data.getHeight();
                if (spinners) {
                    ((SpinnerNumberModel) heightSpinner.getModel()).setMaximum(
                            high - ((Number) ySpinner.getValue()).intValue());
                }
            }
        });
        heightSpinner.addChangeListener(new ChangeListener() {
            private int old;

            @Override
            public void stateChanged(ChangeEvent e) {
                if ((data == null) || (currentGlyph == null)) {
                    heightSpinner.setValue(0);
                    return;
                }
                int current = ((Number) heightSpinner.getValue()).intValue();
                currentGlyph.getBounds().height = current;
                doRepaint(((Number) xSpinner.getValue()).intValue(),
                        ((Number) ySpinner.getValue()).intValue(),
                        ((Number) widthSpinner.getValue()).intValue(),
                        Math.max(old, current));
                old = current;
                int high = (image != null) ? image.getHeight() : data.getHeight();
                if (spinners) {
                    ((SpinnerNumberModel) ySpinner.getModel()).setMaximum(
                            high - ((Number) heightSpinner.getValue()).intValue());
                }
            }
        });
    }

    public static void main(String... args) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new VBFTest().setVisible(true);
            }
        });
    }

    private void createGlyph(ActionEvent evt) {
        @NotNull VBF.BitmapGlyph g = new VBF.BitmapGlyph();
        if (data == null) {
            data = new VBF();
            canvas.setVBF(data);
        }
        for (int i = 0; i < 256; i++) {
            if (!data.hasGlyph(i)) {
                g.setIndex((byte) i);
                break;
            }
            if (i == data.getGlyphs().size()) {
                g.setIndex((byte) (i + 1));
            }
        }
        data.getGlyphs().add(g);
        insertGlyph((DefaultTreeModel) jTree1.getModel(), g);
    }

    private void doRepaint(int x, int y, int w, int h) {
        canvas.repaint();//x, y, h, h);
    }

    private void initComponents() {
        jPopupMenu1 = new JPopupMenu();
        @NotNull JMenuItem jMenuItem4 = new JMenuItem();
        @NotNull JSplitPane jSplitPane1 = new JSplitPane();
        @NotNull JPanel jPanel1 = new JPanel();
        @NotNull JSplitPane jSplitPane2 = new JSplitPane();
        @NotNull JScrollPane jScrollPane2 = new JScrollPane();
        jTree1 = new ReorderableJTree();
        @NotNull JScrollPane jScrollPane3 = new JScrollPane();
        jTree2 = new ReorderableJTree();
        @NotNull JPanel jPanel2 = new JPanel();
        @NotNull JPanel jPanel3 = new JPanel();
        xSpinner = new JSpinner();
        ySpinner = new JSpinner();
        @NotNull JPanel jPanel5 = new JPanel();
        widthSpinner = new JSpinner();
        heightSpinner = new JSpinner();
        @NotNull JScrollPane jScrollPane1 = new JScrollPane();
        canvas = new VBFCanvas();
        @NotNull JMenuBar jMenuBar1 = new JMenuBar();
        @NotNull JMenu jMenu1 = new JMenu();
        @NotNull JMenuItem jMenuItem1 = new JMenuItem();
        @NotNull JMenuItem jMenuItem2 = new JMenuItem();
        @NotNull JMenu jMenu2 = new JMenu();
        @NotNull JMenuItem jMenuItem3 = new JMenuItem();
        jMenuItem4.setText("Copy");
        jMenuItem4.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jMenuItem4ActionPerformed(e);
            }
        });
        jPopupMenu1.add(jMenuItem4);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Bitmap Font Editor");
        setPreferredSize(new Dimension(640, 480));
        jSplitPane1.setDividerLocation(250);
        jSplitPane1.setContinuousLayout(true);
        jSplitPane1.setPreferredSize(new Dimension(360, 403));
        jSplitPane2.setDividerSize(2);
        jSplitPane2.setResizeWeight(0.5);
        jSplitPane2.setEnabled(false);
        @NotNull DefaultMutableTreeNode treeNode1 = new DefaultMutableTreeNode("Glyphs");
        jTree1.setModel(new DefaultTreeModel(treeNode1));
        jTree1.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(@NotNull MouseEvent e) {
                jTree1MouseClicked(e);
            }
        });
        jTree1.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(@NotNull TreeSelectionEvent e) {
                treeInteraction(e);
            }
        });
        jScrollPane2.setViewportView(jTree1);
        jSplitPane2.setLeftComponent(jScrollPane2);
        jTree2.setModel(jTree1.getModel());
        jTree2.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(@NotNull MouseEvent e) {
                jTree2MouseClicked(e);
            }
        });
        jTree2.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(@NotNull TreeSelectionEvent e) {
                treeInteraction(e);
            }
        });
        jScrollPane3.setViewportView(jTree2);
        jSplitPane2.setRightComponent(jScrollPane3);
        jPanel2.setPreferredSize(new Dimension(200, 195));
        jPanel3.setBorder(BorderFactory.createTitledBorder("Position"));
        jPanel3.setLayout(new BoxLayout(jPanel3, BoxLayout.LINE_AXIS));
        xSpinner.setModel(new SpinnerNumberModel(0, 0, null, 1));
        jPanel3.add(xSpinner);
        ySpinner.setModel(new SpinnerNumberModel(0, 0, null, 1));
        jPanel3.add(ySpinner);
        jPanel5.setBorder(BorderFactory.createTitledBorder("Dimensions"));
        jPanel5.setLayout(new BoxLayout(jPanel5, BoxLayout.LINE_AXIS));
        widthSpinner.setModel(new SpinnerNumberModel(0, 0, null, 1));
        jPanel5.add(widthSpinner);
        heightSpinner.setModel(new SpinnerNumberModel(0, 0, null, 1));
        jPanel5.add(heightSpinner);
        @NotNull GroupLayout jPanel2Layout = new GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(jPanel5,
                                GroupLayout.Alignment.TRAILING,
                                GroupLayout.DEFAULT_SIZE,
                                GroupLayout.DEFAULT_SIZE,
                                Short.MAX_VALUE)
                        .addComponent(jPanel3,
                                GroupLayout.Alignment.TRAILING,
                                GroupLayout.DEFAULT_SIZE,
                                GroupLayout.DEFAULT_SIZE,
                                Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(jPanel3,
                                        GroupLayout.PREFERRED_SIZE,
                                        GroupLayout.DEFAULT_SIZE,
                                        GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement
                                        .RELATED)
                                .addComponent(jPanel5,
                                        GroupLayout.PREFERRED_SIZE,
                                        GroupLayout.DEFAULT_SIZE,
                                        GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE))
        );
        @NotNull GroupLayout jPanel1Layout = new GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(jPanel2, GroupLayout.DEFAULT_SIZE, 249, Short.MAX_VALUE)
                        .addComponent(jSplitPane2, GroupLayout.DEFAULT_SIZE, 249, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.TRAILING,
                                jPanel1Layout.createSequentialGroup()
                                        .addComponent(jSplitPane2,
                                                GroupLayout.DEFAULT_SIZE,
                                                301,
                                                Short.MAX_VALUE)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement
                                                .RELATED)
                                        .addComponent(jPanel2,
                                                GroupLayout.PREFERRED_SIZE,
                                                148,
                                                GroupLayout.PREFERRED_SIZE)
                        )
        );
        jSplitPane1.setLeftComponent(jPanel1);
        @NotNull GroupLayout canvasLayout = new GroupLayout(canvas);
        canvas.setLayout(canvasLayout);
        canvasLayout.setHorizontalGroup(canvasLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGap(0, 373, Short.MAX_VALUE)
        );
        canvasLayout.setVerticalGroup(canvasLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGap(0, 449, Short.MAX_VALUE)
        );
        jScrollPane1.setViewportView(canvas);
        jSplitPane1.setRightComponent(jScrollPane1);
        getContentPane().add(jSplitPane1, BorderLayout.CENTER);
        jMenu1.setText("File");
        jMenuItem1.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
        jMenuItem1.setMnemonic('O');
        jMenuItem1.setText("Open");
        jMenuItem1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                open(e);
            }
        });
        jMenu1.add(jMenuItem1);
        jMenuItem2.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
        jMenuItem2.setMnemonic('S');
        jMenuItem2.setText("Save");
        jMenuItem2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                save(e);
            }
        });
        jMenu1.add(jMenuItem2);
        jMenuBar1.add(jMenu1);
        jMenu2.setText("Edit");
        jMenuItem3.setText("Create glyph");
        jMenuItem3.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                createGlyph(e);
            }
        });
        jMenu2.add(jMenuItem3);
        jMenuBar1.add(jMenu2);
        setJMenuBar(jMenuBar1);
        pack();
    }

    private void insertCharacters(@NotNull DefaultTreeModel model, MutableTreeNode child, int g) {
        for (int i = 0; i < data.getTable().length; i++) {
            int glyphIndex = data.getTable()[i];
            if (glyphIndex != g) {
                continue;
            }
            @NotNull MutableTreeNode sub = new DefaultMutableTreeNode(new DisplayableCharacter(i));
            model.insertNodeInto(sub, child, model.getChildCount(child));
        }
    }

    private void insertGlyph(@NotNull DefaultTreeModel model, @NotNull VBF.BitmapGlyph glyph) {
        @NotNull MutableTreeNode child = new DefaultMutableTreeNode(glyph);
        model.insertNodeInto(child, (MutableTreeNode) model.getRoot(), model.getChildCount(model.getRoot()));
        insertCharacters(model, child, glyph.getIndex());
        model.reload();
    }

    private void jMenuItem4ActionPerformed(ActionEvent evt) {
        @NotNull StringSelection selection = new StringSelection(String.valueOf(toCopy));
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
    }

    private void jTree1MouseClicked(@NotNull MouseEvent evt) {
        mouseClicked(evt);
    }

    private void jTree2MouseClicked(@NotNull MouseEvent evt) {
        mouseClicked(evt);
    }

    private void load(String s) throws IOException {
        LOG.log(Level.INFO, "Loading {0}", s);
        VBFCanvas p = canvas;
        @NotNull File vbf = new File(s + ".vbf");
        @NotNull File vtf = new File(s + ".vtf");
        if (vbf.exists()) {
            data = new VBF(new FileInputStream(vbf));
            p.setVBF(data);
            @NotNull DefaultTreeModel model = (DefaultTreeModel) jTree1.getModel();
            @NotNull DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
            root.removeAllChildren();
            for (@NotNull VBF.BitmapGlyph g : data.getGlyphs()) {
                insertGlyph(model, g);
            }
        }
        if (vtf.exists()) {
            image = VTF.load(new FileInputStream(vtf));
            p.setVTF(image);
        }
        canvas.repaint();
    }

    private void mouseClicked(@NotNull MouseEvent evt) {
        if (SwingUtilities.isRightMouseButton(evt)) {
            @NotNull JTree jTree = (JTree) evt.getComponent();
            @Nullable TreePath clicked = jTree.getPathForLocation(evt.getX(), evt.getY());
            if (clicked == null) {
                return;
            }
            if ((jTree.getSelectionPaths() == null) || !Arrays.asList(jTree.getSelectionPaths()).contains(clicked)) {
                jTree.setSelectionPath(clicked);
            }
            for (@NotNull TreePath p : jTree.getSelectionPaths()) {
                if (!(p.getLastPathComponent() instanceof DefaultMutableTreeNode)) {
                    return;
                }
                Object userObject = ((DefaultMutableTreeNode) p.getLastPathComponent()).getUserObject();
                if (userObject instanceof DisplayableCharacter) {
                    toCopy = ((DisplayableCharacter) userObject).getC();
                    jPopupMenu1.show(jTree, evt.getX(), evt.getY());
                }
            }
        }
    }

    private void open(ActionEvent evt) {
        try {
            @Nullable File[] fs = new NativeFileChooser().setParent(this)
                    .setTitle("Select vbf")
                    .addFilter(new BaseFileChooser.ExtensionFilter("Valve Bitmap Font", ".vbf"))
                    .addFilter(new BaseFileChooser.ExtensionFilter("Valve Texture File", ".vtf"))
                    .choose();
            if (fs == null) {
                return;
            }
            File file = fs[0];
            load(file.getPath().replace(".vbf", "").replace(".vtf", ""));
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    private void save(ActionEvent evt) {
        try {
            @Nullable File[] fs = new NativeFileChooser().setParent(this)
                    .setTitle("Select save location")
                    .addFilter(new BaseFileChooser.ExtensionFilter("Valve Bitmap Font", ".vbf"))
                    .setDialogType(BaseFileChooser.DialogType.SAVE_DIALOG)
                    .choose();
            if (fs == null) {
                return;
            }
            File file = fs[0];
            TreeModel model = jTree1.getModel();
            @NotNull TreeNode root = (TreeNode) model.getRoot();
            for (int i = 0; i < root.getChildCount(); i++) {
                @NotNull DefaultMutableTreeNode node = (DefaultMutableTreeNode) root.getChildAt(i);
                @NotNull VBF.BitmapGlyph g = (VBF.BitmapGlyph) node.getUserObject();
                for (int x = 0; x < node.getChildCount(); x++) {
                    @NotNull DefaultMutableTreeNode character = (DefaultMutableTreeNode) node.getChildAt(x);
                    Object obj = character.getUserObject();
                    if (obj instanceof DisplayableCharacter) {
                        data.getTable()[((DisplayableCharacter) obj).getC()] = g.getIndex();
                    } else if (obj instanceof DefaultMutableTreeNode) { // XXX: hack
                        data.getTable()[((DisplayableCharacter) ((DefaultMutableTreeNode) obj).getUserObject()).getC()]
                                = g.getIndex();
                    }
                }
            }
            data.save(new FileOutputStream(file));
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    private void treeInteraction(@NotNull TreeSelectionEvent evt) {
        TreePath selection = evt.getNewLeadSelectionPath();
        if (selection == null) {
            return;
        }
        @Nullable JTree other = (evt.getSource() == jTree1) ? jTree2 : ((evt.getSource() == jTree2) ? jTree1 : null);
        if (other != null) {
            other.setSelectionRow(-1);
        }
        Object node = selection.getLastPathComponent();
        if (!(node instanceof DefaultMutableTreeNode)) {
            return;
        }
        Object obj = ((DefaultMutableTreeNode) node).getUserObject();
        if (!(obj instanceof VBF.BitmapGlyph)) {
            return;
        }
        currentGlyph = (VBF.BitmapGlyph) obj;
        canvas.select(currentGlyph);
        if (currentGlyph.getBounds() == null) {
            currentGlyph.setBounds(new Rectangle());
        }
        Rectangle r = currentGlyph.getBounds();
        xSpinner.setValue(r.x);
        ySpinner.setValue(r.y);
        widthSpinner.setValue(r.width);
        heightSpinner.setValue(r.height);
    }

    private static class DisplayableCharacter {

        private final char c;

        DisplayableCharacter(int i) {
            c = (char) i;
        }

        public char getC() {
            return c;
        }

        @Override
        public String toString() {
            Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
            boolean unprintable = Character.isISOControl(c) || (c == KeyEvent.CHAR_UNDEFINED) || (block == null) ||
                    block.equals(Character.UnicodeBlock.SPECIALS);
            if (unprintable) {
                return "0x" + ((c <= 0xF) ? "0" : "") + Integer.toHexString(c).toUpperCase();
            }
            return Character.toString(c);
        }
    }
}
