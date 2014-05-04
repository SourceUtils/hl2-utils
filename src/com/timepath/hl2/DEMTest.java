package com.timepath.hl2;

import com.timepath.Pair;
import com.timepath.hl2.io.demo.*;
import com.timepath.plaf.x.filechooser.BaseFileChooser.ExtensionFilter;
import com.timepath.plaf.x.filechooser.NativeFileChooser;
import com.timepath.steam.SteamUtils;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

@SuppressWarnings("serial")
public class DEMTest extends javax.swing.JFrame {

    private static final Logger LOG = Logger.getLogger(DEMTest.class.getName());

    /** Creates new form DEMTest */
    public DEMTest() {
        initComponents();

        this.jTable1.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        final TableColumnModel colMod = this.jTable1.getColumnModel();
        final TableModel dataMod = this.jTable1.getModel();

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                this.setBorder(null);

                Message f = (Message) dataMod.getValueAt(jTable1.convertRowIndexToModel(row), 0);
                Color c;
                switch(f.type) {
                    case Signon:
                    case Packet:
                        c = Color.CYAN;
                        break;
                    case UserCmd:
                        c = Color.GREEN;
                        break;
                    case ConsoleCmd:
                        c = Color.PINK;
                        break;
                    default:
                        c = Color.WHITE;
                        break;
                }
                cell.setBackground(isSelected ? cell.getBackground() : c);

                return cell;
            }
        };

        this.jTable1.removeColumn(colMod.getColumn(0));
        for(int i = 0; i < this.jTable1.getColumnCount(); i++) {
            colMod.getColumn(i).setCellRenderer(renderer);
        }

        this.jTable1.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent evt) {
                int row = jTable1.getSelectedRow();
                if(row == -1) {
                    return;
                }
                Message frame = (Message) dataMod.getValueAt(jTable1.convertRowIndexToModel(row), 0);

                hexEditor1.setData(frame.data);

                DefaultMutableTreeNode root = new DefaultMutableTreeNode();
                recurse(frame.meta, root);
                DefaultTreeModel tm = new DefaultTreeModel(root);
                jTree1.setModel(tm);

                // Expand all
                int j = jTree1.getRowCount();
                int i = 0;
                while(i < j) {
                    DefaultMutableTreeNode t = (DefaultMutableTreeNode) jTree1.getPathForRow(i).getLastPathComponent();
                    if(t.getLevel() < 3) {
                        jTree1.expandRow(i);
                    }
                    i++;
                    j = jTree1.getRowCount();
                }
            }
        });
    }

    private void recurse(Iterable<? extends Object> i, DefaultMutableTreeNode root) {
        for(Object entry : i) {
            if(entry instanceof Pair) {
                Pair p = ((Pair) entry);
                expand(p, p.getKey(), p.getValue(), root);
            } else if(entry instanceof Entry) {
                Entry e = ((Entry) entry);
                expand(e, e.getKey(), e.getValue(), root);
            } else {
                root.add(new DefaultMutableTreeNode(entry));
            }
        }
    }

    private void expand(Object entry, Object k, Object v, DefaultMutableTreeNode root) {
        if(v instanceof Iterable) {
            DefaultMutableTreeNode n = new DefaultMutableTreeNode(k);
            root.add(n);
            recurse((Iterable<? extends Object>) v, n);
        } else {
            root.add(new DefaultMutableTreeNode(entry));
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new DEMTest().setVisible(true);
            }
        });
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSplitPane1 = new javax.swing.JSplitPane();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jSplitPane2 = new javax.swing.JSplitPane();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTree1 = new javax.swing.JTree();
        hexEditor1 = new com.timepath.hex.HexEditor();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();
        jMenuItem3 = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("netdecode");

        jSplitPane1.setDividerSize(0);
        jSplitPane1.setResizeWeight(1.0);
        jSplitPane1.setEnabled(false);

        jTable1.setAutoCreateRowSorter(true);
        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Object", "Tick", "Type", "Size"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Object.class, java.lang.Integer.class, java.lang.Object.class, java.lang.Integer.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jTable1.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane2.setViewportView(jTable1);

        jSplitPane1.setLeftComponent(jScrollPane2);

        jSplitPane2.setDividerSize(0);
        jSplitPane2.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jSplitPane2.setResizeWeight(1.0);
        jSplitPane2.setEnabled(false);

        jTabbedPane1.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));

        javax.swing.tree.DefaultMutableTreeNode treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("root");
        jTree1.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        jTree1.setRootVisible(false);
        jTree1.setShowsRootHandles(true);
        jScrollPane1.setViewportView(jTree1);

        jTabbedPane1.addTab("Hierarchy", jScrollPane1);

        jSplitPane2.setTopComponent(jTabbedPane1);
        jSplitPane2.setRightComponent(hexEditor1);

        jSplitPane1.setRightComponent(jSplitPane2);

        jMenu1.setMnemonic('F');
        jMenu1.setText("File");

        jMenuItem1.setText("Open");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem1);

        jMenuItem2.setText("Properties");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem2);

        jMenuItem3.setText("Dump commands");
        jMenuItem3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem3ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem3);

        jMenuBar1.add(jMenu1);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 920, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 659, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed

    }//GEN-LAST:event_jMenuItem2ActionPerformed

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        try {

            File[] fs = new NativeFileChooser()
                .setTitle("Open DEM")
                .setParent(this)
                .setDirectory(new File(SteamUtils.getSteamApps(), "common/Team Fortress 2/tf/."))
                .addFilter(new ExtensionFilter("Demo files", "dem"))
                .choose();

            if(fs == null) {
                return;
            }

            HL2DEM d = HL2DEM.load(fs[0]);
            DefaultTableModel tableModel = (DefaultTableModel) this.jTable1.getModel();
            tableModel.setRowCount(0);
            for(Message f : d.getFrames()) {
                tableModel.addRow(new Object[] {f, f.tick, f.type, f.data == null ? null : f.data.capacity()});
            }
            DefaultListModel<Pair> listModel = new DefaultListModel<>();
            for(Message f : d.getFrames()) {
                for(Pair p : f.meta) {
                    if(p.getKey() instanceof Message) {
                        Message m = (Message) p.getKey();
                        switch(m.type) {
                            case Packet:
                            case Signon:
                                for(Pair<Object, Object> ents : m.meta) {
                                    if(!(ents.getValue() instanceof Iterable)) {
                                        break;
                                    }
                                    for(Object o : (Iterable) ents.getValue()) {
                                        if(!(o instanceof Pair)) {
                                            break;
                                        }
                                        Pair pair = (Pair) o;
                                        if(!(pair.getKey() instanceof Packet)) {
                                            break;
                                        }
                                        Packet pack = (Packet) pair.getKey();
                                        switch(pack) {
                                            case svc_GameEvent:
                                            case svc_UserMessage:
                                                listModel.addElement(pair);
                                                break;
                                        }
                                    }
                                }
                                break;
                        }
                    }
                }
            }
            JPanel p = new JPanel();
            JList<Pair> l = new JList<>(listModel);
            p.add(l);
            if(this.jTabbedPane1.getTabCount() > 1) {
                this.jTabbedPane1.remove(1);
            }
            JScrollPane jsp = new JScrollPane(p);
            jsp.getVerticalScrollBar().setUnitIncrement(16);
            this.jTabbedPane1.add("Events", jsp);
        } catch(IOException ioe) {
            LOG.log(Level.SEVERE, null, ioe);
        }
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void jMenuItem3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem3ActionPerformed
        StringBuilder sb = new StringBuilder(0);
        final TableModel dataMod = this.jTable1.getModel();
        for(int row = 0; row < dataMod.getRowCount(); row++) {
            Message f = (Message) dataMod.getValueAt(row, 0);
            if(f.type == MessageType.ConsoleCmd) {
                for(Pair p : f.meta) {
                    sb.append(p.getValue()).append('\n');
                }
            }
        }
        JScrollPane jsp = new JScrollPane(new JTextArea(sb.toString()));
        jsp.setPreferredSize(new Dimension(500, 500));
        JOptionPane.showMessageDialog(this, jsp);
    }//GEN-LAST:event_jMenuItem3ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private com.timepath.hex.HexEditor hexEditor1;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JSplitPane jSplitPane2;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JTree jTree1;
    // End of variables declaration//GEN-END:variables

}
