package com.timepath.hl2;

import com.timepath.hl2.io.captions.VCCD;
import com.timepath.hl2.io.captions.VCCD.VCCDEntry;
import com.timepath.plaf.x.filechooser.BaseFileChooser;
import com.timepath.plaf.x.filechooser.BaseFileChooser.ExtensionFilter;
import com.timepath.plaf.x.filechooser.NativeFileChooser;
import com.timepath.steam.io.VDF1;
import com.timepath.steam.io.storage.ACF;
import com.timepath.swing.TreeUtils;
import com.timepath.utils.Trie;
import com.timepath.vfs.SimpleVFile;
import java.awt.*;
import java.io.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.zip.CRC32;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

/**
 *
 * @author TimePath
 */
@SuppressWarnings("serial")
public class VCCDTest extends javax.swing.JFrame {

    private static final Logger LOG = Logger.getLogger(VCCDTest.class.getName());

    private static final Preferences prefs = Preferences.userRoot().node("timepath").node("hl2-caption-editor");

    public VCCDTest() {
        // Load known mappings from preferences
        try {
            for(String channel : prefs.childrenNames()) {
                for(String name : prefs.node(channel).keys()) {
                    int hash = prefs.getInt(name, -1);
                    LOG.log(Level.FINER, "{0} = {1}", new Object[] {name, hash});
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
            public void changedUpdate(DocumentEvent e) {
                updateHash();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                updateHash();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateHash();
            }

            public void updateHash() {
                jTextField4.setText(hexFormat(VCCD.hash(jTextField3.getText())));
            }
        });
    }

    private final HashMap<Integer, StringPair> hashmap = new HashMap<>();

    private final Trie trie = new Trie();

    private TableCellEditor getKeyEditor() {
        JTextField t = new JTextField();
//        new StringAutoCompleter(t, trie, 2);
        return new DefaultCellEditor(t);
    }

    /**
     * @param args the command line arguments
     * <p/>
     * @throws java.io.IOException
     */
    public static void main(String[] args) throws IOException {
        try {
            if(args.length > 0) {
                List<VCCDEntry> in = VCCD.parse(new FileInputStream(args[0]));
                HashMap<Integer, StringPair> hashmap = new HashMap<>();
                for(VCCDEntry i : in) { // learning
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

        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                VCCDTest c = new VCCDTest();
                c.setLocationRelativeTo(null);
                c.setVisible(true);
            }
        });
    }
    //<editor-fold defaultstate="collapsed" desc="Hash codes">

    private static final String CHAN_UNKNOWN = "CHAN_UNKNOWN";

    private static class StringPair {

        String channel, name;

        StringPair(String name, String channel) {
            this.channel = channel;
            this.name = name;
        }

    }

    private void generateHash() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                final JFrame frame = new JFrame("Generating hash codes...");
                JProgressBar pb = new JProgressBar();
                pb.setIndeterminate(true);
                frame.add(pb);
                frame.setMinimumSize(new Dimension(300, 50));
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);

                HashMap<Integer, StringPair> map = new HashMap<>();
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

    private static void persistHashmap(HashMap<Integer, StringPair> map) {
        for(Entry<Integer, StringPair> entry : map.entrySet()) {
            Integer key = entry.getKey();
            String value = entry.getValue().name;
            if(key == null || value == null) {
                continue;
            }
            prefs.node(entry.getValue().channel).putInt(value, key);
        }
    }

    private static String hexFormat(int in) {
        String str = Integer.toHexString(in).toUpperCase();
        while(str.length() < 8) {
            str = "0" + str;
        }
        return str;
    }

    private String attemptDecode(int hash) {
        if(!hashmap.containsKey(hash)) {
//            logger.log(Level.INFO, "hashmap does not contain {0}", hash);
            return null;
        }
        return hashmap.get(hash).name;
    }
    //</editor-fold>

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        hashPanel = new javax.swing.JPanel();
        jTextField3 = new javax.swing.JTextField();
        jTextField4 = new javax.swing.JTextField();
        contentPane = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        menuBar = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenuItem6 = new javax.swing.JMenuItem();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem3 = new javax.swing.JMenuItem();
        jMenuItem8 = new javax.swing.JMenuItem();
        jMenuItem12 = new javax.swing.JMenuItem();
        jMenuItem11 = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();
        jMenuItem10 = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        jMenuItem4 = new javax.swing.JMenuItem();
        jMenuItem5 = new javax.swing.JMenuItem();
        jMenuItem9 = new javax.swing.JMenuItem();
        jMenu3 = new javax.swing.JMenu();
        jMenuItem7 = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Caption Editor");
        getContentPane().setLayout(new javax.swing.BoxLayout(getContentPane(), javax.swing.BoxLayout.Y_AXIS));

        hashPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("CRC32"));
        hashPanel.setMaximumSize(new java.awt.Dimension(2147483647, 83));
        hashPanel.setLayout(new java.awt.BorderLayout());
        hashPanel.add(jTextField3, java.awt.BorderLayout.PAGE_START);

        jTextField4.setEditable(false);
        jTextField4.setText("The CRC will appear here");
        hashPanel.add(jTextField4, java.awt.BorderLayout.PAGE_END);

        getContentPane().add(hashPanel);

        jTable1.setAutoCreateRowSorter(true);
        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "CRC32", "Key", "Value"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Object.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, true, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jTable1.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jTable1.setRowHeight(24);
        contentPane.setViewportView(jTable1);
        if (jTable1.getColumnModel().getColumnCount() > 0) {
            jTable1.getColumnModel().getColumn(0).setMinWidth(85);
            jTable1.getColumnModel().getColumn(0).setPreferredWidth(85);
            jTable1.getColumnModel().getColumn(0).setMaxWidth(85);
            jTable1.getColumnModel().getColumn(1).setPreferredWidth(160);
            jTable1.getColumnModel().getColumn(1).setCellEditor(getKeyEditor());
            jTable1.getColumnModel().getColumn(2).setPreferredWidth(160);
        }

        getContentPane().add(contentPane);

        jMenu1.setText("File");

        jMenuItem6.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItem6.setMnemonic('N');
        jMenuItem6.setText("New");
        jMenuItem6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                createNew(evt);
            }
        });
        jMenu1.add(jMenuItem6);

        jMenuItem1.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItem1.setMnemonic('O');
        jMenuItem1.setText("Open");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadCaptions(evt);
            }
        });
        jMenu1.add(jMenuItem1);

        jMenuItem3.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_I, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItem3.setMnemonic('I');
        jMenuItem3.setText("Import");
        jMenuItem3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importCaptions(evt);
            }
        });
        jMenu1.add(jMenuItem3);

        jMenuItem8.setMnemonic('X');
        jMenuItem8.setText("Export");
        jMenuItem8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                export(evt);
            }
        });
        jMenu1.add(jMenuItem8);

        jMenuItem12.setMnemonic('E');
        jMenuItem12.setText("Export all");
        jMenuItem12.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportAll(evt);
            }
        });
        jMenu1.add(jMenuItem12);

        jMenuItem11.setText("Generate hash codes");
        jMenuItem11.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                generateHash(evt);
            }
        });
        jMenu1.add(jMenuItem11);

        jMenuItem2.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItem2.setMnemonic('S');
        jMenuItem2.setText("Save");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveCaptions(evt);
            }
        });
        jMenu1.add(jMenuItem2);

        jMenuItem10.setMnemonic('V');
        jMenuItem10.setText("Save As...");
        jMenuItem10.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveCaptionsAs(evt);
            }
        });
        jMenu1.add(jMenuItem10);

        menuBar.add(jMenu1);

        jMenu2.setText("Edit");

        jMenuItem4.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_INSERT, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItem4.setText("Insert row");
        jMenuItem4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                insertRow(evt);
            }
        });
        jMenu2.add(jMenuItem4);

        jMenuItem5.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItem5.setText("Delete row");
        jMenuItem5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteRow(evt);
            }
        });
        jMenu2.add(jMenuItem5);

        jMenuItem9.setText("Goto");
        jMenuItem9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gotoRow(evt);
            }
        });
        jMenu2.add(jMenuItem9);

        menuBar.add(jMenu2);

        jMenu3.setText("Help");

        jMenuItem7.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F1, 0));
        jMenuItem7.setText("Formatting");
        jMenuItem7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                formattingHelp(evt);
            }
        });
        jMenu3.add(jMenuItem7);

        menuBar.add(jMenu3);

        setJMenuBar(menuBar);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void loadCaptions(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadCaptions
        try {
            NativeFileChooser fc = new NativeFileChooser();
            fc.setTitle("Open");

            fc.addFilter(new ExtensionFilter("VCCD Binary Files", ".dat"));
            fc.setParent(this);
            File[] files = fc.choose();
            if(files == null) {
                return;
            }
            List<VCCDEntry> entries;
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
            for(VCCDEntry entry : entries) {
                model.addRow(
                    new Object[] {hexFormat(entry.getHash()), attemptDecode(entry.getHash()), entry.getValue()});
            }
            saveFile = files[0];
        } catch(IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_loadCaptions

    private File saveFile;

    private void save(boolean flag) {
        if(saveFile == null || flag) {
            try {
                NativeFileChooser fc = new NativeFileChooser();
                fc.setDialogType(BaseFileChooser.DialogType.SAVE_DIALOG);
                fc.setTitle("Save (as closecaption_<language>.dat)");
                fc.addFilter(new ExtensionFilter("VCCD Binary Files", ".dat"));
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

        List<VCCDEntry> entries = new LinkedList<>();
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        for(int i = 0; i < model.getRowCount(); i++) {
            Object crc = model.getValueAt(i, 0);
            if(model.getValueAt(i, 1) != null && !model.getValueAt(i, 1).toString().isEmpty()) {
                crc = hexFormat(VCCD.hash(model.getValueAt(i, 1).toString()));
            }
            int hash = (int) Long.parseLong(crc.toString().toLowerCase(), 16);
            Object key = model.getValueAt(i, 1);
            String token = key instanceof String ? key.toString() : null;
            if(!hashmap.containsKey(hash)) {
                hashmap.put(hash, new StringPair(token, CHAN_UNKNOWN));
            }
            entries.add(new VCCDEntry(hash, model.getValueAt(i, 2).toString()));
        }
        persistHashmap(hashmap);
        try {
            VCCD.save(entries, new FileOutputStream(saveFile));
        } catch(IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    private void saveCaptions(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveCaptions
        save(false);
    }//GEN-LAST:event_saveCaptions

    private void importCaptions(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importCaptions
        try {
            NativeFileChooser fc = new NativeFileChooser();
            fc.setTitle("Import");
            fc.setParent(this);
            fc.addFilter(new ExtensionFilter("VCCD Source Files", ".txt"));

            File[] files = fc.choose();
            if(files == null) {
                return;
            }
            List<VCCDEntry> entries = VCCD.parse(new FileInputStream(files[0]));
            LOG.log(Level.INFO, "Entries: {0}", entries.size());

            DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
            for(int i = model.getRowCount() - 1; i >= 0; i--) {
                model.removeRow(i);
            }
            for(VCCDEntry entrie : entries) {
                int hash = entrie.getHash();
                String token = entrie.getKey();
                if(!hashmap.containsKey(hash)) {
                    hashmap.put(hash, new StringPair(token, CHAN_UNKNOWN));
                }
                model.addRow(new Object[] {hexFormat(entrie.getHash()), entrie.getKey(), entrie.getValue()});
            }
            persistHashmap(hashmap);
        } catch(IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_importCaptions

    private void insertRow(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_insertRow
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        model.addRow(new Object[] {0, "", ""});
    }//GEN-LAST:event_insertRow

    private void deleteRow(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteRow
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        int newRow = Math.min(jTable1.getSelectedRow(), jTable1.getRowCount() - 1);
        if(jTable1.getSelectedRow() == jTable1.getRowCount() - 1) {
            newRow = jTable1.getRowCount() - 2;
        }
        LOG.log(Level.FINER, "New row: {0}", newRow);
        model.removeRow(jTable1.getSelectedRow());
        if(jTable1.getRowCount() > 0) {
            jTable1.setRowSelectionInterval(newRow, newRow);
        }
    }//GEN-LAST:event_deleteRow

    private void createNew(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_createNew
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        for(int i = model.getRowCount() - 1; i >= 0; i--) {
            model.removeRow(i);
        }
        model.addRow(new Object[] {0, "", ""});
    }//GEN-LAST:event_createNew

    private void formattingHelp(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_formattingHelp
        String message = "Main:\n"
                             + "Avoid using spaces immediately after opening tags.\n"
                             + "<clr:r,g,b>\n"
                             + "  Sets the color of the caption using an RGB color; 0 is no color, 255 is full color.\n"
                             + "  For example, <clr:255,100,100> would be red.\n"
                             + "  <clr> with no arguments should restore the previous color for the next phrase, but doesn't?\n"
                         + "<B>\n"
                             + "  Toggles bold text for the next phrase.\n"
                             + "<I>\n"
                             + "  Toggles italicised text for the next phrase.\n"
                             + "<U>\n"
                             + "  Toggles underlined text for the next phrase.\n"
                             + "<cr>\n"
                             + "  Go to new line for next phrase.\n"
                             + "Other:\n"
                             + "<sfx>\n"
                             + "  Marks a line as a sound effect that will only be displayed with full closed captioning.\n"
                         + "  If the user has cc_subtitles 1, it will not display these lines.\n"
                             + "<delay:#>\n"
                             + "  Sets a pre-display delay. The sfx tag overrides this. This tag should come before all others. Can take a decimal value.\n"
                         + "\nUnknown:\n"
                             + "<sameline>\n"
                             + "  Don't go to new line for next phrase.\n"
                             + "<linger:#> / <persist:#> / <len:#>\n"
                             + "  Indicates how much longer than usual the caption should appear on the screen.\n"
                             + "<position:where>\n"
                             + "  I don't know how this one works, but from the sdk comments:\n"
                             + "  Draw caption at special location ??? needed.\n"
                             + "<norepeat:#>\n"
                             + "  Sets how long until the caption can appear again. Useful for frequent sounds.\n"
                             + "  See also: cc_sentencecaptionnorepeat\n"
                             + "<playerclr:playerRed,playerGreen,playerBlue:npcRed,npcGreen,npcBlue>\n"
                             + "\n"
                             + "closecaption 1 enables the captions\n"
                             + "cc_subtitles 1 disables <sfx> captions\n"
                             + "Captions last for 5 seconds + cc_linger_time\n"
                             + "Captions are delayed by cc_predisplay_time seconds\n"
                             + "Changing caption languages (cc_lang) reloads them from tf/resource/closecaption_language.dat\n"
                         + "cc_random emits a random caption\n"
                             + "";
        JOptionPane pane = new JOptionPane(new JScrollPane(new JTextArea(message)),
                                           JOptionPane.INFORMATION_MESSAGE);
        JDialog dialog = pane.createDialog(this, "Formatting");
        dialog.setResizable(true);
        dialog.setModal(false);
        dialog.setVisible(true);
    }//GEN-LAST:event_formattingHelp

    private void export(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_export
        StringBuilder sb = new StringBuilder(jTable1.getRowCount() * 100); // rough estimate
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
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
        jsp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        JOptionPane.showMessageDialog(this, jsp, "Hash List", JOptionPane.INFORMATION_MESSAGE);
    }//GEN-LAST:event_export

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

    private void gotoRow(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gotoRow
        int row = showInputDialog();
        if(row < 0) {
            return;
        }
        if(row > jTable1.getRowCount()) {
            row = jTable1.getRowCount();
        }
        jTable1.setRowSelectionInterval(row, row);
        jTable1.scrollRectToVisible(jTable1.getCellRect(row, 0, true));
    }//GEN-LAST:event_gotoRow

    private void saveCaptionsAs(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveCaptionsAs
        save(true);
    }//GEN-LAST:event_saveCaptionsAs

    private void generateHash(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_generateHash
        generateHash();
    }//GEN-LAST:event_generateHash

    private void exportAll(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportAll
        try {
            NativeFileChooser fc = new NativeFileChooser();
            fc.setDialogType(BaseFileChooser.DialogType.SAVE_DIALOG);
            fc.setTitle("Export");
            fc.addFilter(new ExtensionFilter("XML", ".xml"));
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
    }//GEN-LAST:event_exportAll

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane contentPane;
    private javax.swing.JPanel hashPanel;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem10;
    private javax.swing.JMenuItem jMenuItem11;
    private javax.swing.JMenuItem jMenuItem12;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JMenuItem jMenuItem4;
    private javax.swing.JMenuItem jMenuItem5;
    private javax.swing.JMenuItem jMenuItem6;
    private javax.swing.JMenuItem jMenuItem7;
    private javax.swing.JMenuItem jMenuItem8;
    private javax.swing.JMenuItem jMenuItem9;
    private javax.swing.JTable jTable1;
    private javax.swing.JTextField jTextField3;
    private javax.swing.JTextField jTextField4;
    private javax.swing.JMenuBar menuBar;
    // End of variables declaration//GEN-END:variables

}
