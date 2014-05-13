package com.timepath.hl2;

import com.timepath.hl2.io.CTX;
import com.timepath.plaf.x.filechooser.NativeFileChooser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author TimePath
 */
@SuppressWarnings("serial")
class CTXTest extends JFrame {

    private static final Logger LOG = Logger.getLogger(CTXTest.class.getName());
    private JTextArea  jTextArea1;
    private JTextField jTextField1;

    /**
     * Creates new form CTXTest
     */
    private CTXTest() {
        initComponents();
    }

    private void initComponents() {
        jTextField1 = new JTextField();
        JScrollPane jScrollPane2 = new JScrollPane();
        jTextArea1 = new JTextArea();
        JMenuBar jMenuBar1 = new JMenuBar();
        JMenu jMenu1 = new JMenu();
        JMenuItem jMenuItem1 = new JMenuItem();
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        jTextField1.setText(CTX.TF2);
        jTextField1.setToolTipText("");
        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jTextArea1.setTabSize(4);
        jScrollPane2.setViewportView(jTextArea1);
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
        jMenuBar1.add(jMenu1);
        setJMenuBar(jMenuBar1);
        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(jTextField1)
                                        .addComponent(jScrollPane2,
                                                      GroupLayout.Alignment.TRAILING,
                                                      GroupLayout.DEFAULT_SIZE,
                                                      477,
                                                      Short.MAX_VALUE)
                                 );
        layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                      .addGroup(layout.createSequentialGroup()
                                                      .addComponent(jTextField1,
                                                                    GroupLayout.PREFERRED_SIZE,
                                                                    GroupLayout.DEFAULT_SIZE,
                                                                    GroupLayout.PREFERRED_SIZE)
                                                      .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                      .addComponent(jScrollPane2, GroupLayout.DEFAULT_SIZE, 325, Short.MAX_VALUE))
                               );
        pack();
    }

    private void jMenuItem1ActionPerformed(ActionEvent evt) {
        try {
            File[] f = new NativeFileChooser().setTitle("Select CTX").setMultiSelectionEnabled(false).choose();
            if(f == null) {
                return;
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(CTX.decrypt(jTextField1.getText().getBytes(),
                                                                                     new FileInputStream(f[0]))));
            jTextArea1.setText("");
            String line;
            while(( line = br.readLine() ) != null) {
                jTextArea1.append(line + '\n');
            }
        } catch(IOException ex) {
            Logger.getLogger(CTXTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * @param args
     *         the command line arguments
     */
    public static void main(String... args) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new CTXTest().setVisible(true);
            }
        });
    }
}
