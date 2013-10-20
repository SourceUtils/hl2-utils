package com.timepath.hl2.cvars;

import com.timepath.hl2.ExternalConsole;
import com.timepath.hl2.cvars.CVarList.CVar;
import com.timepath.plaf.x.filechooser.NativeFileChooser;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

/**
 *
 * @author TimePath
 */
public class CVarTest extends javax.swing.JFrame {

    /**
     * Creates new form CVarTest
     */
    public CVarTest() {
        initComponents();
        sorter = new TableRowSorter<TableModel>(jTable1.getModel());
        Comparator<String> comparator = new Comparator<String>() {
            public int compare(String s1, String s2) {
                return s1.replaceFirst("\\+", "").replaceFirst("-", "").toLowerCase().compareTo(
                        s2.replaceFirst("\\+", "").replaceFirst("-", "").toLowerCase());
            }
        };
        sorter.setComparator(0, comparator);

        List<RowSorter.SortKey> sortKeys = new ArrayList<RowSorter.SortKey>();
        sortKeys.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
        sorter.setSortKeys(sortKeys);

        jTable1.setRowSorter(sorter);
        jTextField1.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent de) {
                filter();
            }

            public void removeUpdate(DocumentEvent de) {
                filter();
            }

            public void changedUpdate(DocumentEvent de) {
                filter();
            }
        });
    }

    SwingWorker filterWorker;

    private void filter() {
        jLabel1.setText(Integer.toString(sorter.getModelRowCount()));
        try {
            String str = jTextField1.getText();
            if(str.length() > 0) {
                if(!regexCheckBox.isSelected()) {
                    str = Pattern.quote(str);
                }
                if(!caseSensitiveCheckBox.isSelected()) {
                    str = "(?i)" + str;
                }
                RowFilter<TableModel, Object> rf = RowFilter.regexFilter(str, new int[] {0, 1, 2, 3,
                                                                                         4, 5, 6});
                if(notCheckBox.isSelected()) {
                    rf = RowFilter.notFilter(rf);
                }
                sorter.setRowFilter(rf);
            } else {
                sorter.setRowFilter(null);
            }
            jLabel5.setText(Integer.toString(sorter.getViewRowCount()));
            jTextField1.setForeground(Color.BLACK);
        } catch(PatternSyntaxException e) {
            jTextField1.setForeground(Color.RED);
        }
    }

    private final TableRowSorter<TableModel> sorter;

    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        statusBar1 = new com.timepath.swing.StatusBar();
        jLabel2 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JToolBar.Separator();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jTextField1 = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        regexCheckBox = new javax.swing.JCheckBox();
        caseSensitiveCheckBox = new javax.swing.JCheckBox();
        notCheckBox = new javax.swing.JCheckBox();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem4 = new javax.swing.JMenuItem();
        jMenuItem6 = new javax.swing.JMenuItem();
        jMenuItem5 = new javax.swing.JMenuItem();
        jMenuItem7 = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        jMenuItem2 = new javax.swing.JMenuItem();
        jMenuItem3 = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("CVar listing");

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
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
        regexCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                regexCheckBoxActionPerformed(evt);
            }
        });

        caseSensitiveCheckBox.setMnemonic('M');
        caseSensitiveCheckBox.setText("Match Case");
        caseSensitiveCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                caseSensitiveCheckBoxActionPerformed(evt);
            }
        });

        notCheckBox.setMnemonic('M');
        notCheckBox.setText("Not");
        notCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                notCheckBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextField1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(notCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(caseSensitiveCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(regexCheckBox))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3)
                    .addComponent(regexCheckBox)
                    .addComponent(caseSensitiveCheckBox)
                    .addComponent(notCheckBox))
                .addGap(0, 0, 0))
        );

        jMenu1.setText("File");

        jMenuItem1.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItem1.setText("Open");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem1);

        jMenuItem4.setText("Get cvarlist");
        jMenuItem4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem4ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem4);

        jMenuItem6.setText("Get differences");
        jMenuItem6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem6ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem6);

        jMenuItem5.setText("Reset cvars");
        jMenuItem5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem5ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem5);

        jMenuItem7.setText("Clear");
        jMenuItem7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem7ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem7);

        jMenuBar1.add(jMenu1);

        jMenu2.setText("Edit");

        jMenuItem2.setText("Copy names");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItem2);

        jMenuItem3.setText("Copy markdown");
        jMenuItem3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem3ActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItem3);

        jMenuBar1.add(jMenu2);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(statusBar1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 908, Short.MAX_VALUE)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 201, Short.MAX_VALUE)
                .addGap(0, 0, 0)
                .addComponent(statusBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private Map<String, CVar> analyze(Scanner scanner) {
        Map<String, CVar> map = CVarList.analyzeList(scanner,
                                                     new HashMap<String, CVar>());
        DefaultTableModel p = (DefaultTableModel) jTable1.getModel();
        String[] columns = new String[p.getColumnCount()];
        for(int i = 0; i < columns.length; i++) {
            columns[i] = p.getColumnName(i);
        }
        return map;
    }

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        try {
            final File f[] = new NativeFileChooser().setTitle("Select cvarlist").choose();
            if(f != null) {
                SwingWorker<Void, Object[]> worker = new SwingWorker<Void, Object[]>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        RandomAccessFile rf = new RandomAccessFile(f[0].getPath(), "r");
                        Scanner scanner = new Scanner(rf.getChannel());
                        Map<String, CVar> map = analyze(scanner);
                        for(Entry<String, CVar> entry : map.entrySet()) {
                            CVar var = entry.getValue();
                            this.publish(new Object[] {var.getName(), var.getValue(),
                                                       var.getDefaultValue(), var.getMinimum(),
                                                       var.getMaximum(), Arrays.toString(
                                var.getTags().toArray(new String[0])), var.getDesc()});
                        }
                        return null;
                    }

                    @Override
                    protected void process(List<Object[]> chunks) {
                        for(Object[] row : chunks) {
                            ((DefaultTableModel) jTable1.getModel()).addRow(row);
                        }
                        filter();
                    }
                };
                worker.execute();
            }
        } catch(IOException ex) {
            Logger.getLogger(CVarTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void regexCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_regexCheckBoxActionPerformed
        filter();
    }//GEN-LAST:event_regexCheckBoxActionPerformed

    private void caseSensitiveCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_caseSensitiveCheckBoxActionPerformed
        filter();
    }//GEN-LAST:event_caseSensitiveCheckBoxActionPerformed

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
        StringBuilder sb = new StringBuilder();
        int row;
        for(int i = 0; i < jTable1.getModel().getRowCount(); i++) {
            row = jTable1.convertRowIndexToModel(i);
            sb.append(jTable1.getModel().getValueAt(row, 0)).append("\n");
        }
        StringSelection selection = new StringSelection(sb.toString());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
    }//GEN-LAST:event_jMenuItem2ActionPerformed

    private void jMenuItem3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem3ActionPerformed
        TableModel m = jTable1.getModel();
        StringBuilder sb = new StringBuilder();
        String tab = "|";
        String line = "\n";
        int row = 0, rows = m.getRowCount(), col = 0, cols = m.getColumnCount();
        for(int i = 0; i < cols; i++) {
            col = jTable1.convertColumnIndexToModel(i);
            sb.append(tab).append(m.getColumnName(col));
        }
        sb.append(tab).append(line);
        for(int i = 0; i < cols; i++) {
            sb.append(tab).append("--");
        }
        sb.append(tab).append(line);
        for(int i = 0; i < rows; i++) {
            row = jTable1.convertRowIndexToModel(i);
            for(int j = 0; j < cols; j++) {
                col = jTable1.convertColumnIndexToModel(j);
                Object obj = m.getValueAt(row, col);
                if(col == 0) {
                    obj = "[" + obj + "](/r/tf2scripthelp/wiki/" + obj + "#todo \"TODO\")";
                }
                sb.append(tab);
                if(obj == null) {
                } else if(obj instanceof Object[]) {
                    Object[] arr = (Object[]) obj;
                    sb.append(arr[0]);
                    for(int k = 1; k < arr.length; k++) {
                        sb.append(", ").append(arr[k]);
                    }
                } else {
                    sb.append(obj);
                }
            }
            sb.append(tab).append(line);
        }
        StringSelection selection = new StringSelection(sb.toString());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
    }//GEN-LAST:event_jMenuItem3ActionPerformed

    private void jMenuItem4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem4ActionPerformed
        clear();
        String ret = null;
        int limit = 5;
        for(int i = 0;; i++) {
            String temp = ExternalConsole.exec("cvarlist; echo --end of cvarlist--",
                                               "--end of cvarlist--");
            if(temp.equals(ret)) {
                break;
            }
            if(i == limit) {
                LOG.warning("Aborting inconsistency fixer");
                break;
            }
            ret = temp;
        }

        StringSelection selection = new StringSelection(ret);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);

        Map<String, CVar> map = analyze(new Scanner(ret));
        for(Entry<String, CVar> entry : map.entrySet()) {
            CVar var = entry.getValue();
            Object[] chunks = new Object[] {var.getName(), var.getValue(),
                                            var.getDefaultValue(), var.getMinimum(),
                                            var.getMaximum(), Arrays.toString(
                var.getTags().toArray(new String[0])), var.getDesc()};
            ((DefaultTableModel) jTable1.getModel()).addRow(chunks);
        }
        filter();
    }//GEN-LAST:event_jMenuItem4ActionPerformed

    private void notCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_notCheckBoxActionPerformed
        filter();
    }//GEN-LAST:event_notCheckBoxActionPerformed

    private void jMenuItem5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem5ActionPerformed
        StringBuilder sb = new StringBuilder();
        sb.append("sv_cheats 1\n");
        ExternalConsole.exec("sv_cheats 1", null);
        TableModel m = jTable1.getModel();
        int row, rows = m.getRowCount();
        for(int i = 0; i < rows; i++) {
            row = jTable1.convertRowIndexToModel(i);
            int j = 2;
            Object name = m.getValueAt(row, jTable1.convertColumnIndexToModel(0));
            if(name.toString().equals("sv_cheats")) {
                continue;
            }
            Object val = m.getValueAt(row, jTable1.convertColumnIndexToModel(2));
            if(val != null) {
                String strVal = "\"" + val.toString() + "\"";
                sb.append(name.toString()).append(" ").append(strVal).append("\n");
                ExternalConsole.exec(name.toString() + " " + strVal, null);
            }
        }
        sb.append("sv_cheats 0\n");
        ExternalConsole.exec("sv_cheats 0", null);

        StringSelection selection = new StringSelection(sb.toString());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
    }//GEN-LAST:event_jMenuItem5ActionPerformed

    private void jMenuItem6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem6ActionPerformed
        clear();
        String ret = ExternalConsole.exec("differences; echo --end of differences--",
                                          "--end of differences--");
        Map<String, CVar> map = analyze(new Scanner(ret));
        for(Entry<String, CVar> entry : map.entrySet()) {
            CVar var = entry.getValue();
            Object[] chunks = new Object[] {var.getName(), var.getValue(),
                                            var.getDefaultValue(), var.getMinimum(),
                                            var.getMaximum(), Arrays.toString(
                var.getTags().toArray(new String[0])), var.getDesc()};
            ((DefaultTableModel) jTable1.getModel()).addRow(chunks);
        }
        filter();
    }//GEN-LAST:event_jMenuItem6ActionPerformed

    private void jMenuItem7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem7ActionPerformed
        clear();
    }//GEN-LAST:event_jMenuItem7ActionPerformed

    private void clear() {
        DefaultTableModel dm = (DefaultTableModel) jTable1.getModel();
        int rowCount = dm.getRowCount();
        for(int i = 0; i < rowCount; i++) {
            dm.removeRow(0);
        }
        filter();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String... args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new CVarTest().setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox caseSensitiveCheckBox;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JMenuItem jMenuItem4;
    private javax.swing.JMenuItem jMenuItem5;
    private javax.swing.JMenuItem jMenuItem6;
    private javax.swing.JMenuItem jMenuItem7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JTable jTable1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JCheckBox notCheckBox;
    private javax.swing.JCheckBox regexCheckBox;
    private com.timepath.swing.StatusBar statusBar1;
    // End of variables declaration//GEN-END:variables

    private static final Logger LOG = Logger.getLogger(CVarTest.class.getName());

}
