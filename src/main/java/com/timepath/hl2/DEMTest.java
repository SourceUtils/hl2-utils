package com.timepath.hl2;

import com.timepath.Pair;
import com.timepath.hex.HexEditor;
import com.timepath.hl2.io.demo.HL2DEM;
import com.timepath.hl2.io.demo.Message;
import com.timepath.hl2.io.demo.MessageType;
import com.timepath.hl2.io.demo.Packet;
import com.timepath.plaf.x.filechooser.BaseFileChooser;
import com.timepath.plaf.x.filechooser.NativeFileChooser;
import com.timepath.steam.SteamUtils;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("serial")
class DEMTest extends JFrame {

    private static final Logger LOG = Logger.getLogger(DEMTest.class.getName());
    private HexEditor   hexEditor1;
    private JTabbedPane jTabbedPane1;
    private JTable      jTable1;
    private JTree       jTree1;

    /**
     * Creates new form DEMTest
     */
    private DEMTest() {
        initComponents();
        jTable1.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        TableColumnModel colMod = jTable1.getColumnModel();
        final TableModel dataMod = jTable1.getModel();
        TableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table,
                                                           Object value,
                                                           boolean isSelected,
                                                           boolean hasFocus,
                                                           int row,
                                                           int column)
            {
                Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBorder(null);
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
                if(f.incomplete) {
                    c = Color.ORANGE;
                }
                cell.setBackground(isSelected ? cell.getBackground() : c);
                return cell;
            }
        };
        jTable1.removeColumn(colMod.getColumn(0));
        for(int i = 0; i < jTable1.getColumnCount(); i++) {
            colMod.getColumn(i).setCellRenderer(renderer);
        }
        jTable1.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                int row = jTable1.getSelectedRow();
                if(row == -1) {
                    return;
                }
                Message frame = (Message) dataMod.getValueAt(jTable1.convertRowIndexToModel(row), 0);
                hexEditor1.setData(frame.data);
                DefaultMutableTreeNode root = new DefaultMutableTreeNode();
                recurse(frame.meta, root);
                TreeModel tm = new DefaultTreeModel(root);
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

    /**
     * @param args
     *         the command line arguments
     */
    public static void main(String... args) {
        /* Create and display the form */
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new DEMTest().setVisible(true);
            }
        });
    }

    private void recurse(Iterable<?> i, DefaultMutableTreeNode root) {
        for(Object entry : i) {
            if(entry instanceof Pair) {
                Pair p = (Pair) entry;
                expand(p, p.getKey(), p.getValue(), root);
            } else if(entry instanceof Map.Entry) {
                Map.Entry e = (Map.Entry) entry;
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
            recurse((Iterable<?>) v, n);
        } else {
            root.add(new DefaultMutableTreeNode(entry));
        }
    }

    private void initComponents() {
        JSplitPane jSplitPane1 = new JSplitPane();
        JScrollPane jScrollPane2 = new JScrollPane();
        jTable1 = new JTable();
        JSplitPane jSplitPane2 = new JSplitPane();
        jTabbedPane1 = new JTabbedPane();
        JScrollPane jScrollPane1 = new JScrollPane();
        jTree1 = new JTree();
        hexEditor1 = new HexEditor();
        JMenuBar jMenuBar1 = new JMenuBar();
        JMenu jMenu1 = new JMenu();
        JMenuItem jMenuItem1 = new JMenuItem();
        JMenuItem jMenuItem2 = new JMenuItem();
        JMenuItem jMenuItem3 = new JMenuItem();
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("netdecode");
        jSplitPane1.setResizeWeight(1.0);
        jSplitPane1.setContinuousLayout(true);
        jSplitPane1.setOneTouchExpandable(true);
        jTable1.setAutoCreateRowSorter(true);
        jTable1.setModel(new DefaultTableModel(new Object[][] {
        }, new String[] {
                "Object", "Tick", "Type", "Size"
        }
        )
        {
            Class[] types = {
                    Object.class, Integer.class, Object.class, Integer.class
            };
            boolean[] canEdit = {
                    false, false, false, false
            };

            @Override
            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return canEdit[column];
            }
        });
        jTable1.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jScrollPane2.setViewportView(jTable1);
        jSplitPane1.setLeftComponent(jScrollPane2);
        jSplitPane2.setOrientation(JSplitPane.VERTICAL_SPLIT);
        jSplitPane2.setResizeWeight(1.0);
        jSplitPane2.setContinuousLayout(true);
        jSplitPane2.setOneTouchExpandable(true);
        jTabbedPane1.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        DefaultMutableTreeNode treeNode1 = new DefaultMutableTreeNode("root");
        jTree1.setModel(new DefaultTreeModel(treeNode1));
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
        jMenuItem1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jMenuItem1ActionPerformed(e);
            }
        });
        jMenu1.add(jMenuItem1);
        jMenuItem2.setText("Properties");
        jMenuItem2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jMenuItem2ActionPerformed(e);
            }
        });
        jMenu1.add(jMenuItem2);
        jMenuItem3.setText("Dump commands");
        jMenuItem3.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jMenuItem3ActionPerformed(e);
            }
        });
        jMenu1.add(jMenuItem3);
        jMenuBar1.add(jMenu1);
        setJMenuBar(jMenuBar1);
        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(jSplitPane1, GroupLayout.DEFAULT_SIZE, 920, Short.MAX_VALUE)
                                 );
        layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                      .addComponent(jSplitPane1, GroupLayout.DEFAULT_SIZE, 659, Short.MAX_VALUE)
                               );
        pack();
    }

    private void jMenuItem2ActionPerformed(ActionEvent evt) {
    }

    private void jMenuItem1ActionPerformed(ActionEvent evt) {
        try {
            File[] fs = new NativeFileChooser().setTitle("Open DEM")
                                               .setParent(this)
                                               .setDirectory(new File(SteamUtils.getSteamApps(), "common/Team Fortress 2/tf/."))
                                               .addFilter(new BaseFileChooser.ExtensionFilter("Demo files", "dem"))
                                               .choose();
            if(fs == null) {
                return;
            }
            HL2DEM d = HL2DEM.load(fs[0]);
            DefaultTableModel tableModel = (DefaultTableModel) jTable1.getModel();
            tableModel.setRowCount(0);
            for(Message f : d.getFrames()) {
                tableModel.addRow(new Object[] { f, f.tick, f.type, ( f.data == null ) ? null : f.data.capacity() });
            }
            DefaultListModel<Pair> listModelEvents = new DefaultListModel<>();
            DefaultListModel<Pair> listModelMessages = new DefaultListModel<>();
            for(Message f : d.getFrames()) {
                for(Pair p : f.meta) {
                    if(p.getKey() instanceof Message) {
                        Message m = (Message) p.getKey();
                        switch(m.type) {
                            case Packet:
                            case Signon:
                                for(Pair<Object, Object> ents : m.meta) {
                                    if(!( ents.getValue() instanceof Iterable )) {
                                        break;
                                    }
                                    for(Object o : (Iterable) ents.getValue()) {
                                        if(!( o instanceof Pair )) {
                                            break;
                                        }
                                        Pair pair = (Pair) o;
                                        if(!( pair.getKey() instanceof Packet )) {
                                            break;
                                        }
                                        Packet pack = (Packet) pair.getKey();
                                        switch(pack) {
                                            case svc_GameEvent:
                                                listModelEvents.addElement(pair);
                                                break;
                                            case svc_UserMessage:
                                                listModelMessages.addElement(pair);
                                                break;
                                        }
                                    }
                                }
                                break;
                        }
                    }
                }
            }
            while(jTabbedPane1.getTabCount() > 1) {
                jTabbedPane1.remove(1);
            }
            JPanel p = new JPanel();
            p.add(new JList<>(listModelEvents));
            JScrollPane jsp = new JScrollPane(p);
            jsp.getVerticalScrollBar().setUnitIncrement(16);
            jTabbedPane1.add("Events", jsp);
            JPanel p2 = new JPanel();
            p2.add(new JList<>(listModelMessages));
            JScrollPane jsp2 = new JScrollPane(p2);
            jsp2.getVerticalScrollBar().setUnitIncrement(16);
            jTabbedPane1.add("Messages", jsp2);
        } catch(IOException ioe) {
            LOG.log(Level.SEVERE, null, ioe);
        }
    }

    private void jMenuItem3ActionPerformed(ActionEvent evt) {
        StringBuilder sb = new StringBuilder(0);
        TableModel dataMod = jTable1.getModel();
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
    }
}
