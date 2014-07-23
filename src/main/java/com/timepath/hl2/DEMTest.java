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
import org.jdesktop.swingx.JXFrame;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.JXTree;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyVetoException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author TimePath
 */
@SuppressWarnings("serial")
public class DEMTest extends JPanel {

    private static final Logger LOG = Logger.getLogger(DEMTest.class.getName());
    public final JMenuBar     menu;
    protected    HexEditor    hex;
    protected    JTabbedPane  tabs;
    protected    JXTable      table;
    protected    JXTree       tree;
    protected    MessageModel tableModel;

    protected DEMTest() {
        setLayout(new BorderLayout());
        add(new JSplitPane() {{
            setResizeWeight(1);
            setContinuousLayout(true);
            setOneTouchExpandable(true);
            setLeftComponent(new JScrollPane(table = new JXTable() {{
                setAutoCreateRowSorter(true);
                setColumnControlVisible(true);
                setSortOrderCycle(SortOrder.ASCENDING, SortOrder.DESCENDING, SortOrder.UNSORTED);
                setModel(tableModel = new MessageModel());
                setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            }}));
            setRightComponent(new JSplitPane() {{
                setOrientation(JSplitPane.VERTICAL_SPLIT);
                setResizeWeight(1);
                setContinuousLayout(true);
                setOneTouchExpandable(true);
                setTopComponent(tabs = new JTabbedPane() {{
                    setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
                    addTab("Hierarchy", new JScrollPane(tree = new JXTree() {{
                        setModel(new DefaultTreeModel(new DefaultMutableTreeNode("root")));
                        setRootVisible(false);
                        setShowsRootHandles(true);
                    }}));
                }});
                setRightComponent(hex = new HexEditor());
            }});
        }});
        // Cell renderer
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
                Message f = tableModel.messages.get(DEMTest.this.table.convertRowIndexToModel(row));
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
                if(f.incomplete) c = Color.ORANGE;
                cell.setBackground(isSelected ? cell.getBackground() : c);
                return cell;
            }
        };
        for(int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                int row = table.getSelectedRow();
                if(row == -1) return;
                Message frame = tableModel.messages.get(table.convertRowIndexToModel(row));
                hex.setData(frame.data);
                DefaultMutableTreeNode root = new DefaultMutableTreeNode(frame);
                recurse(frame.meta, root);
                DefaultMutableTreeNode container = new DefaultMutableTreeNode();
                container.add(root);
                TreeModel tm = new DefaultTreeModel(container);
                tree.setModel(tm);
                for(int i = -1; ++i < tree.getRowCount(); ) { // Expand all
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getPathForRow(i).getLastPathComponent();
                    if(node.getLevel() < 3) tree.expandRow(i);
                }
            }
        });
        tree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                TreePath selectionPath = tree.getSelectionPath();
                if(selectionPath == null) return;
                Object lastPathComponent = selectionPath.getLastPathComponent();
                Object o = ( (DefaultMutableTreeNode) lastPathComponent ).getUserObject();
                if(o instanceof Packet) {
                    Packet p = (Packet) o;
                    try {
                        hex.setCaretLocation(p.offset / 8);
                        hex.setBitShift(p.offset % 8);
                        hex.update();
                    } catch(PropertyVetoException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        });
        menu = new JMenuBar() {{
            add(new JMenu("File") {{
                setMnemonic('F');
                add(new JMenuItem("Open") {{
                    addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) { open(); }
                    });
                }});
                add(new JMenuItem("Dump commands") {{
                    addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) { showCommands(); }
                    });
                }});
            }});
        }};
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                JXFrame f = new JXFrame("netdecode");
                f.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                DEMTest demTest = new DEMTest();
                f.add(demTest);
                f.setJMenuBar(demTest.menu);
                f.pack();
                f.setLocationRelativeTo(null);
                f.setVisible(true);
            }
        });
    }

    protected void recurse(Iterable<?> i, DefaultMutableTreeNode root) {
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

    protected void expand(Object entry, Object k, Object v, DefaultMutableTreeNode root) {
        if(v instanceof Iterable) {
            DefaultMutableTreeNode n = new DefaultMutableTreeNode(k);
            root.add(n);
            recurse((Iterable<?>) v, n);
        } else {
            root.add(new DefaultMutableTreeNode(entry));
        }
    }

    protected void open() {
        try {
            File[] fs = new NativeFileChooser().setTitle("Open DEM")
                                               .setDirectory(new File(SteamUtils.getSteamApps(),
                                                                      "common/Team Fortress 2/tf/."))
                                               .addFilter(new BaseFileChooser.ExtensionFilter("Demo files", "dem"))
                                               .choose();
            if(fs == null) return;
            HL2DEM demo = HL2DEM.load(fs[0]);
            DefaultListModel<Pair> listEvt = new DefaultListModel<>(), listMsg = new DefaultListModel<>();
            tableModel.messages.clear();
            int fail = 0;
            for(Message m : demo.getFrames()) {
                if(m.incomplete) fail++;
                tableModel.messages.add(m);
                switch(m.type) {
                    case Packet:
                    case Signon:
                        for(Pair<Object, Object> ents : m.meta) {
                            if(!( ents.getKey() instanceof Packet )) break;
                            if(!( ents.getValue() instanceof Iterable )) break;
                            for(Object o : (Iterable) ents.getValue()) {
                                if(!( o instanceof Pair )) break;
                                Pair pair = (Pair) o;
                                switch(( (Packet) ents.getKey() ).type) {
                                    case svc_GameEvent:
                                        listEvt.addElement(pair);
                                        break;
                                    case svc_UserMessage:
                                        listMsg.addElement(pair);
                                        break;
                                }
                            }
                        }
                        break;
                }
            }
            LOG.info(String.format("Total incomplete messages: %d / %d", fail, demo.getFrames().size()));
            while(tabs.getTabCount() > 1) tabs.remove(1); // Remove previous events and messages
            JScrollPane jsp = new JScrollPane(new JList<>(listEvt));
            jsp.getVerticalScrollBar().setUnitIncrement(16);
            tabs.add("Events", jsp);
            JScrollPane jsp2 = new JScrollPane(new JList<>(listMsg));
            jsp2.getVerticalScrollBar().setUnitIncrement(16);
            tabs.add("Messages", jsp2);
        } catch(IOException ioe) {
            LOG.log(Level.SEVERE, null, ioe);
        }
    }

    protected void showCommands() {
        StringBuilder sb = new StringBuilder();
        for(Message m : tableModel.messages) {
            if(m.type != MessageType.ConsoleCmd) continue;
            for(Pair p : m.meta) {
                sb.append('\n').append(p.getValue());
            }
        }
        JScrollPane jsp = new JScrollPane(new JTextArea(sb.length() > 0 ? sb.substring(1) : ""));
        jsp.setPreferredSize(new Dimension(500, 500));
        JOptionPane.showMessageDialog(this, jsp);
    }

    protected class MessageModel implements TableModel {

        protected ArrayList<Message> messages = new ArrayList<>();

        @Override
        public int getRowCount() { return messages.size(); }

        protected String[] columns = { "Tick", "Type", "Size" };
        protected Class[]  types   = { Integer.class, Enum.class, Integer.class };

        @Override
        public int getColumnCount() { return columns.length; }

        @Override
        public String getColumnName(final int columnIndex) { return columns[columnIndex]; }

        @Override
        public Class<?> getColumnClass(final int columnIndex) { return types[columnIndex]; }

        @Override
        public boolean isCellEditable(final int rowIndex, final int columnIndex) { return false; }

        @Override
        public Object getValueAt(final int rowIndex, final int columnIndex) {
            Message m = messages.get(rowIndex);
            switch(columnIndex) {
                case 0:
                    return m.tick;
                case 1:
                    return m.type;
                case 2:
                    return ( m.data == null ) ? null : m.data.capacity();
            }
            return null;
        }

        @Override
        public void setValueAt(final Object aValue, final int rowIndex, final int columnIndex) { }

        @Override
        public void addTableModelListener(final TableModelListener l) { }

        @Override
        public void removeTableModelListener(final TableModelListener l) { }
    }
}
