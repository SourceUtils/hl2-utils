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
import org.jdesktop.swingx.decorator.AbstractHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.AbstractTableModel;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
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
        table.addHighlighter(new AbstractHighlighter() {
            @Override
            protected Component doHighlight(final Component component, final ComponentAdapter adapter) {
                if(adapter.row >= 0 && tableModel.messages.size() > 0 && adapter.row < tableModel.messages.size()) {
                    Message f = tableModel.messages.get(DEMTest.this.table.convertRowIndexToModel(adapter.row));
                    Color c;
                    if(f.incomplete) {
                        c = Color.ORANGE;
                    } else {
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
                    }
                    component.setBackground(adapter.isSelected() ? component.getBackground() : c);
                }
                return component;
            }
        });
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
                        int offsetBytes = p.offset / 8;
                        int offsetBits = p.offset % 8;
                        hex.seek(offsetBytes - ( offsetBytes % 16 )); // Start of row
                        hex.setCaretLocation(offsetBytes);
                        hex.setBitShift(offsetBits);
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
            final File[] fs = new NativeFileChooser().setTitle("Open DEM")
                                                     .setDirectory(new File(SteamUtils.getSteamApps(),
                                                                            "common/Team Fortress 2/tf/."))
                                                     .addFilter(new BaseFileChooser.ExtensionFilter("Demo files",
                                                                                                    "dem"))
                                                     .choose();
            if(fs == null) return;
            new SwingWorker<HL2DEM, Message>() {
                DefaultListModel<Pair> listEvt = new DefaultListModel<>();
                DefaultListModel<Pair> listMsg = new DefaultListModel<>();
                int incomplete = 0;

                @Override
                protected HL2DEM doInBackground() throws Exception {
                    tableModel.messages.clear();
                    HL2DEM demo = HL2DEM.load(fs[0]);
                    List<Message> frames = demo.getFrames(); // TODO: Stream
                    publish(frames.toArray(new Message[frames.size()]));
                    return demo;
                }

                @Override
                protected void process(final List<Message> chunks) {
                    for(Message m : chunks) {
                        if(m.incomplete) incomplete++;
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
                    tableModel.fireTableDataChanged(); // FIXME
                }

                @Override
                protected void done() {
                    HL2DEM demo;
                    try {
                        demo = get();
                    } catch(InterruptedException ignored) {
                        return;
                    } catch(ExecutionException e) {
                        LOG.log(Level.SEVERE, null, e);
                        return;
                    }
                    LOG.info(String.format("Total incomplete messages: %d / %d", incomplete, demo.getFrames().size()));
                    while(tabs.getTabCount() > 1) tabs.remove(1); // Remove previous events and messages
                    JScrollPane jsp = new JScrollPane(new JList<>(listEvt));
                    jsp.getVerticalScrollBar().setUnitIncrement(16);
                    tabs.add("Events", jsp);
                    JScrollPane jsp2 = new JScrollPane(new JList<>(listMsg));
                    jsp2.getVerticalScrollBar().setUnitIncrement(16);
                    tabs.add("Messages", jsp2);
                    table.setModel(tableModel);
                }
            }.execute();
        } catch(IOException e) {
            LOG.log(Level.SEVERE, null, e);
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

    protected class MessageModel extends AbstractTableModel {

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
            if(messages.isEmpty()) return null;
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
