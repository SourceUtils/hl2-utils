package com.timepath.hl2;

import com.timepath.hl2.cvar.CVar;
import com.timepath.hl2.cvar.CVarList;
import com.timepath.plaf.x.filechooser.NativeFileChooser;
import com.timepath.swing.StatusBar;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * @author TimePath
 */
@SuppressWarnings("serial")
class CVarTest extends JFrame {

    private static final Logger LOG = Logger.getLogger(CVarTest.class.getName());
    @NotNull
    private final TableRowSorter<TableModel> sorter;
    private JCheckBox caseSensitiveCheckBox;
    private JLabel jLabel1;
    private JLabel jLabel5;
    private JTable jTable1;
    private JTextField jTextField1;
    private JCheckBox notCheckBox;
    private JCheckBox regexCheckBox;

    /**
     * Creates new form CVarTest
     */
    private CVarTest() {
        initComponents();
        sorter = new TableRowSorter<>(jTable1.getModel());
        @NotNull Comparator<String> comparator = new Comparator<String>() {
            @Override
            public int compare(@NotNull String o1, @NotNull String o2) {
                return o1.replaceFirst("\\+", "")
                        .replaceFirst("-", "")
                        .toLowerCase()
                        .compareTo(o2.replaceFirst("\\+", "").replaceFirst("-", "").toLowerCase());
            }
        };
        sorter.setComparator(0, comparator);
        @NotNull List<RowSorter.SortKey> sortKeys = new LinkedList<>();
        sortKeys.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
        sorter.setSortKeys(sortKeys);
        jTable1.setRowSorter(sorter);
        jTextField1.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filter();
            }
        });
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String... args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new CVarTest().setVisible(true);
            }
        });
    }

    private void filter() {
        jLabel1.setText(Integer.toString(sorter.getModelRowCount()));
        try {
            String str = jTextField1.getText();
            if (str.isEmpty()) {
                sorter.setRowFilter(null);
            } else {
                if (!regexCheckBox.isSelected()) {
                    str = Pattern.quote(str);
                }
                if (!caseSensitiveCheckBox.isSelected()) {
                    str = "(?i)" + str;
                }
                RowFilter<TableModel, Object> rf = RowFilter.regexFilter(str, 0, 1, 2, 3, 4, 5, 6);
                if (notCheckBox.isSelected()) {
                    rf = RowFilter.notFilter(rf);
                }
                sorter.setRowFilter(rf);
            }
            jLabel5.setText(Integer.toString(sorter.getViewRowCount()));
            jTextField1.setForeground(Color.BLACK);
        } catch (PatternSyntaxException e) {
            jTextField1.setForeground(Color.RED);
        }
    }

    private void initComponents() {
        @NotNull JScrollPane jScrollPane1 = new JScrollPane();
        jTable1 = new JTable();
        @NotNull StatusBar statusBar1 = new StatusBar();
        @NotNull JLabel jLabel2 = new JLabel();
        jLabel1 = new JLabel();
        @NotNull JToolBar.Separator jSeparator1 = new JToolBar.Separator();
        @NotNull JLabel jLabel4 = new JLabel();
        jLabel5 = new JLabel();
        @NotNull JPanel jPanel1 = new JPanel();
        jTextField1 = new JTextField();
        @NotNull JLabel jLabel3 = new JLabel();
        regexCheckBox = new JCheckBox();
        caseSensitiveCheckBox = new JCheckBox();
        notCheckBox = new JCheckBox();
        @NotNull JMenuBar jMenuBar1 = new JMenuBar();
        @NotNull JMenu jMenu1 = new JMenu();
        @NotNull JMenuItem jMenuItem1 = new JMenuItem();
        @NotNull JMenuItem jMenuItem4 = new JMenuItem();
        @NotNull JMenuItem jMenuItem6 = new JMenuItem();
        @NotNull JMenuItem jMenuItem5 = new JMenuItem();
        @NotNull JMenuItem jMenuItem7 = new JMenuItem();
        @NotNull JMenu jMenu2 = new JMenu();
        @NotNull JMenuItem jMenuItem2 = new JMenuItem();
        @NotNull JMenuItem jMenuItem3 = new JMenuItem();
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("CVar listing");
        jTable1.setModel(new DefaultTableModel(new Object[][]{
        }, new String[]{
                "Name", "Value", "Default", "Min", "Max", "Tags", "Description"
        }
        ));
        jScrollPane1.setViewportView(jTable1);
        statusBar1.setRollover(true);
        jLabel2.setText(" Total convars/concommands: ");
        statusBar1.add(jLabel2);
        jLabel1.setText("0");
        statusBar1.add(jLabel1);
        statusBar1.add(jSeparator1);
        jLabel4.setText("Showing: ");
        statusBar1.add(jLabel4);
        jLabel5.setText("0");
        statusBar1.add(jLabel5);
        jLabel3.setText("Find:");
        regexCheckBox.setMnemonic('R');
        regexCheckBox.setText("Regular Expression");
        regexCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                regexCheckBoxActionPerformed(e);
            }
        });
        caseSensitiveCheckBox.setMnemonic('M');
        caseSensitiveCheckBox.setText("Match Case");
        caseSensitiveCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                caseSensitiveCheckBoxActionPerformed(e);
            }
        });
        notCheckBox.setMnemonic('M');
        notCheckBox.setText("Not");
        notCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                notCheckBoxActionPerformed(e);
            }
        });
        @NotNull GroupLayout jPanel1Layout = new GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel3)
                                .addPreferredGap(LayoutStyle.ComponentPlacement
                                        .RELATED)
                                .addComponent(jTextField1)
                                .addPreferredGap(LayoutStyle.ComponentPlacement
                                        .RELATED)
                                .addComponent(notCheckBox)
                                .addPreferredGap(LayoutStyle.ComponentPlacement
                                        .RELATED)
                                .addComponent(caseSensitiveCheckBox)
                                .addPreferredGap(LayoutStyle.ComponentPlacement
                                        .RELATED)
                                .addComponent(regexCheckBox))
        );
        jPanel1Layout.setVerticalGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(0, 0, 0)
                                .addGroup(jPanel1Layout.createParallelGroup
                                        (GroupLayout.Alignment.BASELINE)
                                        .addComponent(jTextField1,
                                                GroupLayout
                                                        .PREFERRED_SIZE,
                                                GroupLayout
                                                        .DEFAULT_SIZE,
                                                GroupLayout
                                                        .PREFERRED_SIZE
                                        )
                                        .addComponent(jLabel3)
                                        .addComponent(regexCheckBox)
                                        .addComponent(
                                                caseSensitiveCheckBox)
                                        .addComponent(notCheckBox))
                                .addGap(0, 0, 0))
        );
        jMenu1.setText("File");
        jMenuItem1.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
        jMenuItem1.setText("Open");
        jMenuItem1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jMenuItem1ActionPerformed(e);
            }
        });
        jMenu1.add(jMenuItem1);
        jMenuItem4.setText("Get cvarlist");
        jMenuItem4.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jMenuItem4ActionPerformed(e);
            }
        });
        jMenu1.add(jMenuItem4);
        jMenuItem6.setText("Get differences");
        jMenuItem6.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jMenuItem6ActionPerformed(e);
            }
        });
        jMenu1.add(jMenuItem6);
        jMenuItem5.setText("Reset cvars");
        jMenuItem5.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jMenuItem5ActionPerformed(e);
            }
        });
        jMenu1.add(jMenuItem5);
        jMenuItem7.setText("Clear");
        jMenuItem7.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jMenuItem7ActionPerformed(e);
            }
        });
        jMenu1.add(jMenuItem7);
        jMenuBar1.add(jMenu1);
        jMenu2.setText("Edit");
        jMenuItem2.setText("Copy names");
        jMenuItem2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jMenuItem2ActionPerformed(e);
            }
        });
        jMenu2.add(jMenuItem2);
        jMenuItem3.setText("Copy markdown");
        jMenuItem3.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jMenuItem3ActionPerformed(e);
            }
        });
        jMenu2.add(jMenuItem3);
        jMenuBar1.add(jMenu2);
        setJMenuBar(jMenuBar1);
        @NotNull GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(statusBar1,
                                GroupLayout.DEFAULT_SIZE,
                                GroupLayout.DEFAULT_SIZE,
                                Short.MAX_VALUE)
                        .addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 908, Short.MAX_VALUE)
                        .addComponent(jPanel1,
                                GroupLayout.DEFAULT_SIZE,
                                GroupLayout.DEFAULT_SIZE,
                                Short.MAX_VALUE)
        );
        layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(jPanel1,
                                        GroupLayout.PREFERRED_SIZE,
                                        GroupLayout.DEFAULT_SIZE,
                                        GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, 0)
                                .addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 201, Short.MAX_VALUE)
                                .addGap(0, 0, 0)
                                .addComponent(statusBar1,
                                        GroupLayout.PREFERRED_SIZE,
                                        25,
                                        GroupLayout.PREFERRED_SIZE))
        );
        pack();
    }

    @NotNull
    private Map<String, CVar> analyze(@NotNull Scanner scanner) {
        @NotNull Map<String, CVar> map = CVarList.analyzeList(scanner, new HashMap<String, CVar>());
        @NotNull DefaultTableModel p = (DefaultTableModel) jTable1.getModel();
        @NotNull String[] columns = new String[p.getColumnCount()];
        for (int i = 0; i < columns.length; i++) {
            columns[i] = p.getColumnName(i);
        }
        return map;
    }

    private void jMenuItem1ActionPerformed(ActionEvent evt) {
        try {
            @Nullable final File[] f = new NativeFileChooser().setTitle("Select cvarlist").choose();
            if (f != null) {
                @NotNull SwingWorker<Void, Object[]> worker = new SwingWorker<Void, Object[]>() {
                    @Nullable
                    @Override
                    protected Void doInBackground() throws Exception {
                        @NotNull RandomAccessFile rf = new RandomAccessFile(f[0].getPath(), "r");
                        @NotNull Scanner scanner = new Scanner(rf.getChannel());
                        @NotNull Map<String, CVar> map = analyze(scanner);
                        for (@NotNull Map.Entry<String, CVar> entry : map.entrySet()) {
                            CVar var = entry.getValue();
                            publish(new Object[]{
                                    var.getName(),
                                    var.getValue(),
                                    var.getDefaultValue(),
                                    var.getMinimum(),
                                    var.getMaximum(),
                                    Arrays.toString(var.getTags().toArray(new String[var.getTags().size()])),
                                    var.getDesc()
                            });
                        }
                        return null;
                    }

                    @Override
                    protected void process(@NotNull List<Object[]> chunks) {
                        for (Object[] row : chunks) {
                            ((DefaultTableModel) jTable1.getModel()).addRow(row);
                        }
                        filter();
                    }
                };
                worker.execute();
            }
        } catch (IOException ex) {
            Logger.getLogger(CVarTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void regexCheckBoxActionPerformed(ActionEvent evt) {
        filter();
    }

    private void caseSensitiveCheckBoxActionPerformed(ActionEvent evt) {
        filter();
    }

    private void jMenuItem2ActionPerformed(ActionEvent evt) {
        @NotNull StringBuilder sb = new StringBuilder();
        for (int i = 0; i < jTable1.getModel().getRowCount(); i++) {
            int row = jTable1.convertRowIndexToModel(i);
            sb.append(jTable1.getModel().getValueAt(row, 0)).append('\n');
        }
        @NotNull StringSelection selection = new StringSelection(sb.toString());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
    }

    private void jMenuItem3ActionPerformed(ActionEvent evt) {
        TableModel m = jTable1.getModel();
        @NotNull StringBuilder sb = new StringBuilder();
        @NotNull String tab = "|";
        int col;
        int rows = m.getRowCount();
        int cols = m.getColumnCount();
        for (int i = 0; i < cols; i++) {
            col = jTable1.convertColumnIndexToModel(i);
            sb.append(tab).append(m.getColumnName(col));
        }
        @NotNull String line = "\n";
        sb.append(tab).append(line);
        for (int i = 0; i < cols; i++) {
            sb.append(tab).append("--");
        }
        sb.append(tab).append(line);
        for (int i = 0; i < rows; i++) {
            int row = jTable1.convertRowIndexToModel(i);
            for (int j = 0; j < cols; j++) {
                col = jTable1.convertColumnIndexToModel(j);
                Object obj = m.getValueAt(row, col);
                if (col == 0) {
                    obj = "[" + obj + "](/r/tf2scripthelp/wiki/" + obj + "#todo \"TODO\")";
                }
                sb.append(tab);
                if (obj != null) {
                    if (obj instanceof Object[]) {
                        @NotNull Object[] arr = (Object[]) obj;
                        sb.append(arr[0]);
                        for (int k = 1; k < arr.length; k++) {
                            sb.append(", ").append(arr[k]);
                        }
                    } else {
                        sb.append(obj);
                    }
                }
            }
            sb.append(tab).append(line);
        }
        @NotNull StringSelection selection = new StringSelection(sb.toString());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
    }

    private void jMenuItem4ActionPerformed(ActionEvent evt) {
        clear();
        @Nullable String ret = null;
        int limit = 5;
        for (int i = 0; ; i++) {
            @NotNull String temp = ExternalConsole.exec("cvarlist; echo --end of cvarlist--", "--end of cvarlist--");
            if (temp.equals(ret)) {
                break;
            }
            if (i == limit) {
                LOG.warning("Aborting inconsistency fixer");
                break;
            }
            ret = temp;
        }
        @NotNull StringSelection selection = new StringSelection(ret);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
        @NotNull Map<String, CVar> map = analyze(new Scanner(ret));
        for (@NotNull Map.Entry<String, CVar> entry : map.entrySet()) {
            ((DefaultTableModel) jTable1.getModel()).addRow(toRow(entry.getValue()));
        }
        filter();
    }

    private Object[] toRow(CVar var) {
        return new Object[]{
                        var.getName(),
                        var.getValue(),
                        var.getDefaultValue(),
                        var.getMinimum(),
                        var.getMaximum(),
                        Arrays.toString(var.getTags().toArray(new String[var.getTags().size()])),
                        var.getDesc()
                };
    }

    private void notCheckBoxActionPerformed(ActionEvent evt) {
        filter();
    }

    private void jMenuItem5ActionPerformed(ActionEvent evt) {
        @NotNull StringBuilder sb = new StringBuilder();
        sb.append("sv_cheats 1\n");
        ExternalConsole.exec("sv_cheats 1", null);
        TableModel m = jTable1.getModel();
        int rows = m.getRowCount();
        for (int i = 0; i < rows; i++) {
            int row = jTable1.convertRowIndexToModel(i);
            int j = 2;
            Object name = m.getValueAt(row, jTable1.convertColumnIndexToModel(0));
            if ("sv_cheats".equals(name.toString())) {
                continue;
            }
            Object val = m.getValueAt(row, jTable1.convertColumnIndexToModel(2));
            if (val != null) {
                @NotNull String strVal = "\"" + val + '"';
                sb.append(name).append(' ').append(strVal).append('\n');
                ExternalConsole.exec(name + " " + strVal, null);
            }
        }
        sb.append("sv_cheats 0\n");
        ExternalConsole.exec("sv_cheats 0", null);
        @NotNull StringSelection selection = new StringSelection(sb.toString());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
    }

    private void jMenuItem6ActionPerformed(ActionEvent evt) {
        clear();
        @NotNull String ret = ExternalConsole.exec("differences; echo --end of differences--", "--end of differences--");
        @NotNull Map<String, CVar> map = analyze(new Scanner(ret));
        for (@NotNull Map.Entry<String, CVar> entry : map.entrySet()) {
            ((DefaultTableModel) jTable1.getModel()).addRow(toRow(entry.getValue()));
        }
        filter();
    }

    private void jMenuItem7ActionPerformed(ActionEvent evt) {
        clear();
    }

    private void clear() {
        @NotNull DefaultTableModel dm = (DefaultTableModel) jTable1.getModel();
        int rowCount = dm.getRowCount();
        for (int i = 0; i < rowCount; i++) {
            dm.removeRow(0);
        }
        filter();
    }
}
