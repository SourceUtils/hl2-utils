package com.timepath.hl2;

import com.timepath.plaf.x.filechooser.NativeFileChooser;
import essiembre.FileChangeListener;
import essiembre.FileMonitor;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
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
        output.setEnabled(false);

        jsp = new JScrollPane(output);
        jsp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jsp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        input = new JTextField();
        input.setEnabled(false);
        input.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(ps == null) {
                    return;
                }
                ps.println(input.getText());
                ps.flush();
                input.setText("");
            }
        });

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
                    watch(log[0]);
                    output.setText("");
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
                update(sb.toString());
            } catch(IOException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        }
    };

    public void watch(File f) {
        output.setEnabled(f != null);
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

    public void update(String str) {
        parse(str);
        appendOutput(str);
    }

    public static void main(String... args) {
        new ExternalConsole().setVisible(true);
    }

    private void appendOutput(String str) {
        output.append(str + '\n');

        JScrollBar vertical = jsp.getVerticalScrollBar();
        if(vertical.getValue() == vertical.getMaximum()) {
            output.setCaretPosition(output.getDocument().getLength());
        }
    }

    protected void parse(String str) {
    }

    public void setIn(final InputStream s) {
        output.setEnabled(s != null);
        new Thread(new Runnable() {
            public void run() {
                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(s));
                    String line;
                    while((line = in.readLine()) != null) {
                        update(line);
                    }
                } catch(IOException ex) {
                    Logger.getLogger(ExternalConsole.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }).start();
    }
    
    private PrintStream ps;

    public void setOut(OutputStream s) {
        input.setEnabled(s != null);
        ps = new PrintStream(s);
    }

}