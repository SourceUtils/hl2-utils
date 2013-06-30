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
import java.net.InetAddress;
import java.net.Socket;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.script.*;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.DefaultCaret;

/**
 * http://www.perkin.org.uk/posts/how-to-fix-stdio-buffering.html
 * <p/>
 * @author timepath
 */
@SuppressWarnings("serial")
public class ExternalConsole extends JFrame {

    private static final Logger LOG = Logger.getLogger(ExternalConsole.class.getName());

    public static String exec(String cmd, String breakline) {
        StringBuilder sb = new StringBuilder();
        try {

            Socket sock = new Socket(InetAddress.getByName(null), 12345);
            PrintWriter pw = new PrintWriter(sock.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            pw.println(cmd);
            if(breakline != null) {
                String line;
                while((line = in.readLine()) != null) {
                    sb.append(line).append("\n");
                    if(line.contains(breakline)) {
                        break;
                    }
                }
            }
        } catch(IOException ex) {
            Logger.getLogger(ExternalConsole.class.getName()).log(Level.SEVERE, null, ex);
        }
        return sb.toString();
    }

    protected JTextArea output;

    private JTextField input;

    private JScrollPane jsp;

    public ExternalConsole() {
        output = new JTextArea();
        output.setFont(new Font("Monospaced", Font.PLAIN, 15));
        output.setEnabled(false);
        DefaultCaret caret = (DefaultCaret) output.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        jsp = new JScrollPane(output);
        jsp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jsp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        input = new JTextField();
        input.setEnabled(false);
        input.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(pw == null) {
                    return;
                }
                pw.println(input.getText());
                input.setText("");
            }
        });

        JMenuBar jmb = new JMenuBar();
        this.setJMenuBar(jmb);
        JMenu fileMenu = new JMenu("File");
        jmb.add(fileMenu);
        JMenuItem logFile = new JMenuItem("Open");
        fileMenu.add(logFile);
        JMenuItem reload = new JMenuItem("Reload script");
        fileMenu.add(reload);
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

        reload.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                engine = initScriptEngine();
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
//        System.out.println(str);
        parse(str);
        appendOutput(str);
    }

    private void appendOutput(String str) {
        output.append(str + '\n');
    }

    ScriptEngine engine = initScriptEngine();

    private ScriptEngine initScriptEngine() {
        ScriptEngineManager factory = new ScriptEngineManager();
        ScriptEngine engine = factory.getEngineByName("JavaScript");
//        Bindings bindings = engine.createBindings();
//        bindings.put("loadTime", new Date());
        engine.getContext().setWriter(pw);
        try {
            engine.eval(new FileReader("extern.js"));
        } catch(ScriptException ex) {
            Logger.getLogger(ExternalConsole.class.getName()).log(Level.SEVERE, null, ex);
        } catch(FileNotFoundException ex) {
            Logger.getLogger(ExternalConsole.class.getName()).log(Level.SEVERE, null, ex);
        }
        return engine;
    }

    private static final Pattern regex = Pattern.compile("(\\S+)\\s*[(]\\s*(\\S*)\\s*[)].*");

    protected void parse(String str) {
        if(str.startsWith(">>>")) {
            str = str.substring(3);
            System.out.println("Matching " + str);
            Matcher m = regex.matcher(str);
            if(!m.matches()) {
                System.out.println("Doesn't match");
                return;
            }
            String fn = m.group(1);
            System.out.println(fn);
            String[] args = m.group(2).split(",");
            System.out.println(System.currentTimeMillis());
            Invocable inv = (Invocable) engine;
            try {
                inv.invokeFunction(fn, (Object[]) args);
            } catch(ScriptException ex) {
                Logger.getLogger(ExternalConsole.class.getName()).log(Level.SEVERE, null, ex);
            } catch(NoSuchMethodException ex) {
                Logger.getLogger(ExternalConsole.class.getName()).log(Level.SEVERE, null, ex);
            }
            System.out.println(System.currentTimeMillis());
        }
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
                    System.err.println("Stopped reading stdout");
                } catch(IOException ex) {
                    Logger.getLogger(ExternalConsole.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }).start();
    }

    public void setErr(final InputStream s) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(s));
                    String line;
                    while((line = in.readLine()) != null) {
                        System.err.println(line);
                    }
                    System.err.println("Stopped reading stderr");
                } catch(IOException ex) {
                    Logger.getLogger(ExternalConsole.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }).start();
    }

    private PrintWriter pw;

    public void setOut(OutputStream s) {
        input.setEnabled(s != null);
        pw = new PrintWriter(s, true);
        engine.getContext().setWriter(pw);
    }

    public void connect(int port) throws IOException {
        Socket sock = new Socket(InetAddress.getByName(null), port);
        setIn(sock.getInputStream());
        setOut(sock.getOutputStream());
    }

    public static void main(String... args) throws Exception {
        ExternalConsole ec = new ExternalConsole();
        ec.connect(12345);
        ec.setVisible(true);
    }

}