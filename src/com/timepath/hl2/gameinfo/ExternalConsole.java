package com.timepath.hl2.gameinfo;

import com.timepath.plaf.x.filechooser.NativeFileChooser;
import essiembre.FileChangeListener;
import essiembre.FileMonitor;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 *
 * @author timepath
 */
@SuppressWarnings("serial")
public class ExternalConsole extends JFrame {

    private static final Logger LOG = Logger.getLogger(ExternalConsole.class.getName());

    protected JTextArea output;

    private JTextField input;

    private JScrollPane jsp;

    private void attachLinux() {
//        > gdb -p 'pidof hl2_linux'
//        > (gdb) call creat("/tmp/tf2out", 0600)
//        < $1 = 3
//        > (gdb) call dup2(3, 1)
//        < $2 = 1
//        Or maybe
//        strace -ewrite -p 'pidof hl2_linux'
//        Another
//        http://superuser.com/questions/473240/redirect-stdout-while-a-process-is-running-what-is-that-process-sending-to-d/535938#535938
    }

    public ExternalConsole() {
        output = new JTextArea();
        output.setFont(new Font("Monospaced", Font.PLAIN, 15));

        jsp = new JScrollPane(output);
        jsp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jsp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        input = new JTextField();
        input.setEditable(false);
        input.setEnabled(false);

        JMenuBar jmb = new JMenuBar();
        this.setJMenuBar(jmb);
        JMenu fileMenu = new JMenu("File");
        jmb.add(fileMenu);
        JMenuItem logFile = new JMenuItem("Open");
        fileMenu.add(logFile);
        logFile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                try {
                    File[] log = new NativeFileChooser().setTitle("Select logfile").choose();
                    if(log == null) {
                        return;
                    }
                    log(log[0]);
                } catch(IOException ex) {
                    Logger.getLogger(ExternalConsole.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
        });

        this.setTitle("External console");
//        setAlwaysOnTop(true);
//        setUndecorated(true);
        this.setPreferredSize(new Dimension(800, 600));

        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        this.getContentPane().add(jsp, BorderLayout.CENTER);
        this.getContentPane().add(input, BorderLayout.SOUTH); // TODO: work out better way of sending input

        this.pack();
    }

    private File log;

    private FileChangeListener fcl = new FileChangeListener() {
        public void fileChanged(File file) {
            update(file);
        }
    };

    private void log(File f) {
        output.setText("");
        FileMonitor.getInstance().removeFileChangeListener(fcl, f);
        log = f;
        try {
            FileMonitor.getInstance().addFileChangeListener(fcl, log, 500);
//            FTPWatcher.getInstance().addFileChangeListener(new FTPUpdateListener() {
//                public void fileChanged(String newLines) {
//                    appendOutput(newLines.substring(cursorPos));
//                    cursorPos = newLines.length();
//                }
//            });
        } catch(FileNotFoundException ex) {
            Logger.getLogger(ExternalConsole.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

//    private int cursorPos;

    private int currentUpdateLine;

    public void update(File file) {
        try {
            RandomAccessFile rf = new RandomAccessFile(file, "r");
            for(int i = 0; i < currentUpdateLine; i++) {
                rf.readLine();
            }
            StringBuilder sb = new StringBuilder();
            String str;
            while((str = rf.readLine()) != null) {
                sb.append(str).append("\n");
                currentUpdateLine++;
            }
            appendOutput(sb.toString());
        } catch(IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    public static void main(String... args) {
        new ExternalConsole().setVisible(true);
    }

    private void appendOutput(String str) {
        parse(str);

        JScrollBar vertical = jsp.getVerticalScrollBar();
        if(vertical.getValue() == vertical.getMaximum()) {
            output.setCaretPosition(output.getDocument().getLength());
        }
    }
    
    protected void parse(String str) {
         output.append(str);
    }
    
}