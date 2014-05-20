package com.timepath.hl2;

import com.timepath.hl2.io.captions.VCCD;
import com.timepath.plaf.x.filechooser.BaseFileChooser;
import com.timepath.plaf.x.filechooser.NativeFileChooser;
import com.timepath.steam.io.VDF1;
import com.timepath.steam.io.storage.ACF;
import com.timepath.swing.TreeUtils;
import com.timepath.utils.Trie;
import com.timepath.vfs.SimpleVFile;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.*;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.zip.CRC32;

/**
 * @author TimePath
 */
@SuppressWarnings("serial")
class VCCDTest extends JFrame {

    private static final Logger                   LOG          = Logger.getLogger(VCCDTest.class.getName());
    private static final Preferences              prefs        = Preferences.userRoot()
                                                                            .node("timepath")
                                                                            .node("hl2-caption-editor");
    private static final String                   CHAN_UNKNOWN = "CHAN_UNKNOWN";
    private final        Trie                     trie         = new Trie();
    private final        Map<Integer, StringPair> hashmap      = new HashMap<>();
    private File       saveFile;
    private JTable     jTable1;
    private JTextField jTextField3;
    private JTextField jTextField4;

    private VCCDTest() {
        // Load known mappings from preferences
        try {
            for(String channel : prefs.childrenNames()) {
                for(String name : prefs.node(channel).keys()) {
                    int hash = prefs.getInt(name, -1);
                    LOG.log(Level.FINER, "{0} = {1}", new Object[] { name, hash });
                    if(hash != -1) {
                        hashmap.put(hash, new StringPair(name, channel));
                        trie.add(name);
                    }
                }
            }
        } catch(BackingStoreException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        initComponents();
        jTextField3.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateHash();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateHash();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateHash();
            }

            public void updateHash() {
                jTextField4.setText(hexFormat(VCCD.hash(jTextField3.getText())));
            }
        });
    }

    /**
     * @param args
     *         the command line arguments
     *
     * @throws java.io.IOException
     */
    public static void main(String... args) throws IOException {
        try {
            if(args.length > 0) {
                List<VCCD.VCCDEntry> in = VCCD.parse(new FileInputStream(args[0]));
                Map<Integer, StringPair> hashmap = new HashMap<>();
                for(VCCD.VCCDEntry i : in) { // learning
                    Object crc = i.getHash();
                    String token = i.getKey();
                    long hash = Long.parseLong(crc.toString().toLowerCase(), 16);
                    hashmap.put((int) hash, new StringPair(token, CHAN_UNKNOWN));
                }
                persistHashmap(hashmap);
                VCCD.save(in, new FileOutputStream("closecaption_english.dat"));
                return;
            }
        } catch(FileNotFoundException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                VCCDTest c = new VCCDTest();
                c.setLocationRelativeTo(null);
                c.setVisible(true);
            }
        });
    }

    private static void persistHashmap(Map<Integer, StringPair> map) {
        for(Map.Entry<Integer, StringPair> entry : map.entrySet()) {
            Integer key = entry.getKey();
            String value = entry.getValue().name;
            if(( key == null ) || ( value == null )) {
                continue;
            }
            prefs.node(entry.getValue().channel).putInt(value, key);
        }
    }

    private static String hexFormat(int in) {
        String str = Integer.toHexString(in).toUpperCase();
        while(str.length() < 8) {
            str = '0' + str;
        }
        return str;
    }

    private static TableCellEditor getKeyEditor() {
        JTextField t = new JTextField();
        //        new StringAutoCompleter(t, trie, 2);
        return new DefaultCellEditor(t);
    }

    private void generateHash() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                JFrame frame = new JFrame("Generating hash codes...");
                JProgressBar pb = new JProgressBar();
                pb.setIndeterminate(true);
                frame.add(pb);
                frame.setMinimumSize(new Dimension(300, 50));
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
                Map<Integer, StringPair> map = new HashMap<>();
                LOG.info("Generating hash codes ...");
                try {
                    CRC32 crc = new CRC32();
                    DefaultMutableTreeNode top = new DefaultMutableTreeNode();
                    List<SimpleVFile> caps = ACF.fromManifest(440).find("game_sounds");
                    pb.setMaximum(caps.size());
                    pb.setIndeterminate(false);
                    for(int i = 0; i < caps.size(); i++) {
                        VDF1 e = new VDF1();
                        e.readExternal(caps.get(i).stream());
                        TreeUtils.moveChildren(e.getRoot(), top);
                        pb.setValue(i + 1);
                    }
                    String spl = "channel == ";
                    for(int i = 0; i < top.getChildCount(); i++) {
                        TreeNode node = top.getChildAt(i);
                        String str = node.toString();
                        str = str.replaceAll("\"", "").toLowerCase();
                        String channel = CHAN_UNKNOWN;
                        for(int j = 0; j < node.getChildCount(); j++) {
                            String prop = node.getChildAt(j).toString();
                            if(prop.startsWith(spl)) {
                                channel = prop.split(spl)[1];
                            }
                        }
                        LOG.log(Level.FINER, str);
                        crc.update(str.getBytes());
                        map.put((int) crc.getValue(), new StringPair(str, channel));
                        crc.reset();
                    }
                } catch(FileNotFoundException ex) {
                    LOG.log(Level.WARNING, "Error generating hash codes", ex);
                }
                hashmap.putAll(map);
                persistHashmap(hashmap);
                frame.dispose();
            }
        }).start();
    }

    private String attemptDecode(int hash) {
        if(!hashmap.containsKey(hash)) {
            //            logger.log(Level.INFO, "hashmap does not contain {0}", hash);
            return null;
        }
        return hashmap.get(hash).name;
    }

    private void initComponents() {
        JPanel hashPanel = new JPanel();
        jTextField3 = new JTextField();
        jTextField4 = new JTextField();
        JScrollPane contentPane = new JScrollPane();
        jTable1 = new JTable();
        JMenuBar menuBar = new JMenuBar();
        JMenu jMenu1 = new JMenu();
        JMenuItem jMenuItem6 = new JMenuItem();
        JMenuItem jMenuItem1 = new JMenuItem();
        JMenuItem jMenuItem3 = new JMenuItem();
        JMenuItem jMenuItem8 = new JMenuItem();
        JMenuItem jMenuItem12 = new JMenuItem();
        JMenuItem jMenuItem11 = new JMenuItem();
        JMenuItem jMenuItem2 = new JMenuItem();
        JMenuItem jMenuItem10 = new JMenuItem();
        JMenu jMenu2 = new JMenu();
        JMenuItem jMenuItem4 = new JMenuItem();
        JMenuItem jMenuItem5 = new JMenuItem();
        JMenuItem jMenuItem9 = new JMenuItem();
        JMenu jMenu3 = new JMenu();
        JMenuItem jMenuItem7 = new JMenuItem();
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Caption Editor");
        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        hashPanel.setBorder(BorderFactory.createTitledBorder("CRC32"));
        hashPanel.setMaximumSize(new Dimension(2147483647, 83));
        hashPanel.setLayout(new BorderLayout());
        hashPanel.add(jTextField3, BorderLayout.PAGE_START);
        jTextField4.setEditable(false);
        jTextField4.setText("The CRC will appear here");
        hashPanel.add(jTextField4, BorderLayout.PAGE_END);
        getContentPane().add(hashPanel);
        jTable1.setAutoCreateRowSorter(true);
        jTable1.setModel(new DefaultTableModel(new Object[][] {
        }, new String[] {
                "CRC32", "Key", "Value"
        }
        )
        {
            Class[] types = {
                    Object.class, String.class, String.class
            };
            boolean[] canEdit = {
                    false, true, true
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
        jTable1.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        jTable1.setRowHeight(24);
        contentPane.setViewportView(jTable1);
        if(jTable1.getColumnModel().getColumnCount() > 0) {
            jTable1.getColumnModel().getColumn(0).setMinWidth(85);
            jTable1.getColumnModel().getColumn(0).setPreferredWidth(85);
            jTable1.getColumnModel().getColumn(0).setMaxWidth(85);
            jTable1.getColumnModel().getColumn(1).setPreferredWidth(160);
            jTable1.getColumnModel().getColumn(1).setCellEditor(getKeyEditor());
            jTable1.getColumnModel().getColumn(2).setPreferredWidth(160);
        }
        getContentPane().add(contentPane);
        jMenu1.setText("File");
        jMenuItem6.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_MASK));
        jMenuItem6.setMnemonic('N');
        jMenuItem6.setText("New");
        jMenuItem6.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                createNew(e);
            }
        });
        jMenu1.add(jMenuItem6);
        jMenuItem1.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
        jMenuItem1.setMnemonic('O');
        jMenuItem1.setText("Open");
        jMenuItem1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadCaptions(e);
            }
        });
        jMenu1.add(jMenuItem1);
        jMenuItem3.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_MASK));
        jMenuItem3.setMnemonic('I');
        jMenuItem3.setText("Import");
        jMenuItem3.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                importCaptions(e);
            }
        });
        jMenu1.add(jMenuItem3);
        jMenuItem8.setMnemonic('X');
        jMenuItem8.setText("Export");
        jMenuItem8.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                export(e);
            }
        });
        jMenu1.add(jMenuItem8);
        jMenuItem12.setMnemonic('E');
        jMenuItem12.setText("Export all");
        jMenuItem12.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exportAll(e);
            }
        });
        jMenu1.add(jMenuItem12);
        jMenuItem11.setText("Generate hash codes");
        jMenuItem11.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                generateHash(e);
            }
        });
        jMenu1.add(jMenuItem11);
        jMenuItem2.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
        jMenuItem2.setMnemonic('S');
        jMenuItem2.setText("Save");
        jMenuItem2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveCaptions(e);
            }
        });
        jMenu1.add(jMenuItem2);
        jMenuItem10.setMnemonic('V');
        jMenuItem10.setText("Save As...");
        jMenuItem10.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveCaptionsAs(e);
            }
        });
        jMenu1.add(jMenuItem10);
        menuBar.add(jMenu1);
        jMenu2.setText("Edit");
        jMenuItem4.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, InputEvent.CTRL_MASK));
        jMenuItem4.setText("Insert row");
        jMenuItem4.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                insertRow(e);
            }
        });
        jMenu2.add(jMenuItem4);
        jMenuItem5.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, InputEvent.CTRL_MASK));
        jMenuItem5.setText("Delete row");
        jMenuItem5.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteRow(e);
            }
        });
        jMenu2.add(jMenuItem5);
        jMenuItem9.setText("Goto");
        jMenuItem9.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gotoRow(e);
            }
        });
        jMenu2.add(jMenuItem9);
        menuBar.add(jMenu2);
        jMenu3.setText("Help");
        jMenuItem7.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
        jMenuItem7.setText("Formatting");
        jMenuItem7.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                formattingHelp(e);
            }
        });
        jMenu3.add(jMenuItem7);
        menuBar.add(jMenu3);
        setJMenuBar(menuBar);
        pack();
    }

    private void loadCaptions(ActionEvent evt) {
        try {
            NativeFileChooser fc = new NativeFileChooser();
            fc.setTitle("Open");
            fc.addFilter(new BaseFileChooser.ExtensionFilter("VCCD Binary Files", ".dat"));
            fc.setParent(this);
            File[] files = fc.choose();
            if(files == null) {
                return;
            }
            List<VCCD.VCCDEntry> entries;
            try {
                entries = VCCD.load(new FileInputStream(files[0]));
            } catch(FileNotFoundException ex) {
                LOG.log(Level.SEVERE, null, ex);
                return;
            }
            LOG.log(Level.INFO, "Entries: {0}", entries.size());
            DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
            for(int i = model.getRowCount() - 1; i >= 0; i--) {
                model.removeRow(i);
            }
            for(VCCD.VCCDEntry entry : entries) {
                model.addRow(new Object[] { hexFormat(entry.getHash()), attemptDecode(entry.getHash()), entry.getValue() });
            }
            saveFile = files[0];
        } catch(IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    private void save(boolean flag) {
        if(( saveFile == null ) || flag) {
            try {
                NativeFileChooser fc = new NativeFileChooser();
                fc.setDialogType(BaseFileChooser.DialogType.SAVE_DIALOG);
                fc.setTitle("Save (as closecaption_<language>.dat)");
                fc.addFilter(new BaseFileChooser.ExtensionFilter("VCCD Binary Files", ".dat"));
                fc.setParent(this);
                File[] fs = fc.choose();
                if(fs == null) {
                    return;
                }
                saveFile = fs[0];
            } catch(IOException ex) {
                LOG.log(Level.SEVERE, null, ex);
                return;
            }
        }
        if(jTable1.isEditing()) {
            jTable1.getCellEditor().stopCellEditing();
        }
        List<VCCD.VCCDEntry> entries = new LinkedList<>();
        TableModel model = jTable1.getModel();
        for(int i = 0; i < model.getRowCount(); i++) {
            Object crc = model.getValueAt(i, 0);
            if(( model.getValueAt(i, 1) != null ) && !model.getValueAt(i, 1).toString().isEmpty()) {
                crc = hexFormat(VCCD.hash(model.getValueAt(i, 1).toString()));
            }
            int hash = (int) Long.parseLong(crc.toString().toLowerCase(), 16);
            Object key = model.getValueAt(i, 1);
            String token = ( key instanceof String ) ? key.toString() : null;
            if(!hashmap.containsKey(hash)) {
                hashmap.put(hash, new StringPair(token, CHAN_UNKNOWN));
            }
            entries.add(new VCCD.VCCDEntry(hash, model.getValueAt(i, 2).toString()));
        }
        persistHashmap(hashmap);
        try {
            VCCD.save(entries, new FileOutputStream(saveFile));
        } catch(IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    private void saveCaptions(ActionEvent evt) {
        save(false);
    }

    private void importCaptions(ActionEvent evt) {
        try {
            NativeFileChooser fc = new NativeFileChooser();
            fc.setTitle("Import");
            fc.setParent(this);
            fc.addFilter(new BaseFileChooser.ExtensionFilter("VCCD Source Files", ".txt"));
            File[] files = fc.choose();
            if(files == null) {
                return;
            }
            List<VCCD.VCCDEntry> entries = VCCD.parse(new FileInputStream(files[0]));
            LOG.log(Level.INFO, "Entries: {0}", entries.size());
            DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
            for(int i = model.getRowCount() - 1; i >= 0; i--) {
                model.removeRow(i);
            }
            for(VCCD.VCCDEntry entrie : entries) {
                int hash = entrie.getHash();
                String token = entrie.getKey();
                if(!hashmap.containsKey(hash)) {
                    hashmap.put(hash, new StringPair(token, CHAN_UNKNOWN));
                }
                model.addRow(new Object[] { hexFormat(entrie.getHash()), entrie.getKey(), entrie.getValue() });
            }
            persistHashmap(hashmap);
        } catch(IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    private void insertRow(ActionEvent evt) {
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        model.addRow(new Object[] { 0, "", "" });
    }

    private void deleteRow(ActionEvent evt) {
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        int newRow = Math.min(jTable1.getSelectedRow(), jTable1.getRowCount() - 1);
        if(jTable1.getSelectedRow() == ( jTable1.getRowCount() - 1 )) {
            newRow = jTable1.getRowCount() - 2;
        }
        LOG.log(Level.FINER, "New row: {0}", newRow);
        model.removeRow(jTable1.getSelectedRow());
        if(jTable1.getRowCount() > 0) {
            jTable1.setRowSelectionInterval(newRow, newRow);
        }
    }

    private void createNew(ActionEvent evt) {
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        for(int i = model.getRowCount() - 1; i >= 0; i--) {
            model.removeRow(i);
        }
        model.addRow(new Object[] { 0, "", "" });
    }

    private void formattingHelp(ActionEvent evt) {
        String message = "Main:\n" + "Avoid using spaces immediately after opening tags.\n" + "<clr:r,g,b>\n" +
                         "  Sets the color of the caption using an RGB color; 0 is no color, 255 is full color.\n" +
                         "  For example, <clr:255,100,100> would be red.\n" +
                         "  <clr> with no arguments should restore the previous color for the next phrase, but doesn't?\n" +
                         "<B>\n" + "  Toggles bold text for the next phrase.\n" + "<I>\n" +
                         "  Toggles italicised text for the next phrase.\n" + "<U>\n" +
                         "  Toggles underlined text for the next phrase.\n" + "<cr>\n" + "  Go to new line for next phrase.\n" +
                         "Other:\n" + "<sfx>\n" +
                         "  Marks a line as a sound effect that will only be displayed with full closed captioning.\n" +
                         "  If the user has cc_subtitles 1, it will not display these lines.\n" + "<delay:#>\n" +
                         "  Sets a pre-display delay. The sfx tag overrides this. This tag should come before all others. Can " +
                         "take a decimal value.\n" +
                         "\nUnknown:\n" + "<sameline>\n" + "  Don't go to new line for next phrase.\n" +
                         "<linger:#> / <persist:#> / <len:#>\n" +
                         "  Indicates how much longer than usual the caption should appear on the screen.\n" +
                         "<position:where>\n" + "  I don't know how this one works, but from the sdk comments:\n" +
                         "  Draw caption at special location ??? needed.\n" + "<norepeat:#>\n" +
                         "  Sets how long until the caption can appear again. Useful for frequent sounds.\n" +
                         "  See also: cc_sentencecaptionnorepeat\n" +
                         "<playerclr:playerRed,playerGreen,playerBlue:npcRed,npcGreen,npcBlue>\n" + '\n' +
                         "closecaption 1 enables the captions\n" + "cc_subtitles 1 disables <sfx> captions\n" +
                         "Captions last for 5 seconds + cc_linger_time\n" +
                         "Captions are delayed by cc_predisplay_time seconds\n" +
                         "Changing caption languages (cc_lang) reloads them from tf/resource/closecaption_language.dat\n" +
                         "cc_random emits a random caption\n" + "";
        JScrollPane jsp = new JScrollPane(new JTextArea(message));
        jsp.setPreferredSize(new Dimension(500, 500));
        JOptionPane pane = new JOptionPane(jsp, JOptionPane.INFORMATION_MESSAGE);
        JDialog dialog = pane.createDialog(this, "Formatting");
        dialog.setResizable(true);
        dialog.setModal(false);
        dialog.setVisible(true);
    }

    private void export(ActionEvent evt) {
        StringBuilder sb = new StringBuilder(jTable1.getRowCount() * 100); // rough estimate
        TableModel model = jTable1.getModel();
        for(int i = 0; i < model.getRowCount(); i++) {
            sb.append(MessageFormat.format("{0}\t{1}\n", model.getValueAt(i, 0), model.getValueAt(i, 2)));
        }
        JTextArea pane = new JTextArea(sb.toString());
        Dimension s = Toolkit.getDefaultToolkit().getScreenSize();
        pane.setLineWrap(false);
        pane.setPreferredSize(new Dimension(s.width / 3, s.height / 2));
        pane.setEditable(false);
        pane.setOpaque(false);
        pane.setBackground(new Color(0, 0, 0, 0));
        JScrollPane jsp = new JScrollPane(pane);
        jsp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        JOptionPane.showMessageDialog(this, jsp, "Hash List", JOptionPane.INFORMATION_MESSAGE);
    }

    private int showInputDialog() {
        String inputValue = JOptionPane.showInputDialog("Enter row", jTable1.getSelectedRow() + 1);
        int intValue = -1;
        if(inputValue != null) {
            try {
                intValue = Integer.parseInt(inputValue) - 1;
            } catch(NumberFormatException e) {
                showInputDialog();
            }
            if(intValue < 0) {
                showInputDialog();
            }
        }
        return intValue;
    }

    private void gotoRow(ActionEvent evt) {
        int row = showInputDialog();
        if(row < 0) {
            return;
        }
        if(row > jTable1.getRowCount()) {
            row = jTable1.getRowCount();
        }
        jTable1.setRowSelectionInterval(row, row);
        jTable1.scrollRectToVisible(jTable1.getCellRect(row, 0, true));
    }

    private void saveCaptionsAs(ActionEvent evt) {
        save(true);
    }

    private void generateHash(ActionEvent evt) {
        generateHash();
    }

    private void exportAll(ActionEvent evt) {
        try {
            NativeFileChooser fc = new NativeFileChooser();
            fc.setDialogType(BaseFileChooser.DialogType.SAVE_DIALOG);
            fc.setTitle("Export");
            fc.addFilter(new BaseFileChooser.ExtensionFilter("XML", ".xml"));
            fc.setParent(this);
            File[] fs = fc.choose();
            if(fs == null) {
                return;
            }
            saveFile = fs[0];
            prefs.exportSubtree(new FileOutputStream(saveFile));
        } catch(IOException | BackingStoreException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    private static class StringPair {

        String channel, name;

        StringPair(String name, String channel) {
            this.channel = channel;
            this.name = name;
        }
    }
}