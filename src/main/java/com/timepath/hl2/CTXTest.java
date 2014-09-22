package com.timepath.hl2;

import com.timepath.hl2.io.CTX;
import com.timepath.plaf.x.filechooser.NativeFileChooser;
import org.jdesktop.swingx.JXTextArea;
import org.jdesktop.swingx.JXTextField;

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
public class CTXTest extends JFrame {

    private static final Logger LOG = Logger.getLogger(CTXTest.class.getName());
    protected JXTextField input;
    protected JXTextArea output;

    protected CTXTest() {
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        input = new JXTextField(CTX.TF2);
        input.setToolTipText("");
        output = new JXTextArea();
        output.setColumns(20);
        output.setRows(5);
        output.setTabSize(4);
        output.setEditable(false);
        JMenuBar jMenuBar1 = new JMenuBar();
        JMenu jMenu1 = new JMenu("File");
        JMenuItem jMenuItem1 = new JMenuItem("Open");
        jMenuItem1.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
        jMenuItem1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                open();
            }
        });
        jMenu1.add(jMenuItem1);
        jMenuBar1.add(jMenu1);
        this.setJMenuBar(jMenuBar1);
        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        JScrollPane jScrollPane2 = new JScrollPane(output);
        layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(input)
                .addComponent(jScrollPane2,
                        GroupLayout.Alignment.TRAILING,
                        GroupLayout.DEFAULT_SIZE,
                        477,
                        Short.MAX_VALUE));
        layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                        .addComponent(input,
                                GroupLayout.PREFERRED_SIZE,
                                GroupLayout.DEFAULT_SIZE,
                                GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane2,
                                GroupLayout.DEFAULT_SIZE,
                                325,
                                Short.MAX_VALUE)));
        this.pack();
        this.setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new CTXTest().setVisible(true);
            }
        });
    }

    protected void open() {
        try {
            File[] f = new NativeFileChooser().setTitle("Select CTX").setMultiSelectionEnabled(false).choose();
            if (f == null) return;
            BufferedReader br = new BufferedReader(new InputStreamReader(CTX.decrypt(input.getText().getBytes(),
                    new FileInputStream(f[0]))));
            output.setText("");
            for (String line; (line = br.readLine()) != null; ) output.append(line + '\n');
        } catch (IOException e) {
            LOG.log(Level.SEVERE, null, e);
        }
    }
}
