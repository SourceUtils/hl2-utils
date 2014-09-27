package com.timepath.hl2;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * http://www.perkin.org.uk/posts/how-to-fix-stdio-buffering.html
 *
 * @author TimePath
 */
@SuppressWarnings("serial")
public class ExternalConsole extends JFrame {

    private static final Logger LOG = Logger.getLogger(ExternalConsole.class.getName());
    private static final Pattern regex = Pattern.compile("(\\S+)\\s*[(]\\s*(\\S*)\\s*[)].*");
    @NotNull
    private final JTextField input;
    @NotNull
    private final JTextArea output;
    private ScriptEngine engine = initScriptEngine();
    private PrintWriter pw;
    private Socket sock;

    protected ExternalConsole() {
        output = new JTextArea();
        output.setFont(new Font("Monospaced", Font.PLAIN, 15));
        output.setEnabled(false);
        @NotNull DefaultCaret caret = (DefaultCaret) output.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        @NotNull JScrollPane jsp = new JScrollPane(output);
        jsp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jsp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        input = new JTextField();
        input.setEnabled(false);
        input.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (pw == null) {
                    return;
                }
                getOutput().append("] ");
                pw.println(input.getText());
                input.setText("");
            }
        });
        @NotNull JMenuBar jmb = new JMenuBar();
        setJMenuBar(jmb);
        @NotNull JMenu fileMenu = new JMenu("File");
        jmb.add(fileMenu);
        @NotNull JMenuItem reload = new JMenuItem("Reload script");
        fileMenu.add(reload);
        reload.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                engine = initScriptEngine();
            }
        });
        setTitle("External console");
        //        setAlwaysOnTop(true);
        //        setUndecorated(true);
        setPreferredSize(new Dimension(800, 600));
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (sock != null) {
                    try {
                        sock.close();
                    } catch (IOException ex) {
                        LOG.log(Level.SEVERE, null, ex);
                    }
                }
                dispose();
            }
        });
        getContentPane().add(jsp, BorderLayout.CENTER);
        getContentPane().add(input, BorderLayout.SOUTH); // TODO: work out better way of sending input
        pack();
    }

    @NotNull
    public static String exec(String cmd, @Nullable CharSequence breakline) {
        @NotNull StringBuilder sb = new StringBuilder();
        try {
            @NotNull Socket sock = new Socket(InetAddress.getByName(null), 12345);
            @NotNull PrintWriter pw = new PrintWriter(sock.getOutputStream(), true);
            @NotNull BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            pw.println(cmd);
            if (breakline != null) {
                in.readLine(); // first line is echoed
                String line;
                while ((line = in.readLine()) != null) {
                    sb.append(line).append('\n');
                    if (line.contains(breakline)) {
                        break;
                    }
                }
            }
            sock.close();
        } catch (IOException ex) {
            Logger.getLogger(ExternalConsole.class.getName()).log(Level.SEVERE, null, ex);
        }
        return sb.toString();
    }

    public static void main(String... args) throws Exception {
        @NotNull ExternalConsole ec = new ExternalConsole();
        ec.connect(12345);
        ec.setVisible(true);
    }

    public static void setErr(@NotNull final InputStream s) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    @NotNull BufferedReader in = new BufferedReader(new InputStreamReader(s));
                    String line;
                    while ((line = in.readLine()) != null) {
                        System.err.println(line);
                    }
                    System.err.println("Stopped reading stderr");
                } catch (IOException ex) {
                    Logger.getLogger(ExternalConsole.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }).start();
    }

    private ScriptEngine initScriptEngine() {
        @NotNull ScriptEngineManager factory = new ScriptEngineManager();
        ScriptEngine scriptEngine = factory.getEngineByName("JavaScript");
        //        Bindings bindings = engine.createBindings();
        //        bindings.put("loadTime", new Date());
        scriptEngine.getContext().setWriter(pw);
        try {
            scriptEngine.eval(new FileReader("extern.js"));
        } catch (@NotNull ScriptException | FileNotFoundException ex) {
            Logger.getLogger(ExternalConsole.class.getName()).log(Level.SEVERE, null, ex);
        }
        return scriptEngine;
    }

    /**
     * @return the output
     */
    @NotNull
    protected JTextArea getOutput() {
        return output;
    }

    protected void connect(int port) throws IOException {
        sock = new Socket(InetAddress.getByName(null), port);
        setIn(sock.getInputStream());
        setOut(sock.getOutputStream());
    }

    void setIn(@Nullable final InputStream s) {
        output.setEnabled(s != null);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    @NotNull BufferedReader in = new BufferedReader(new InputStreamReader(s));
                    String line;
                    while ((line = in.readLine()) != null) {
                        update(line);
                    }
                    System.err.println("Stopped reading stdout");
                } catch (IOException ex) {
                    Logger.getLogger(ExternalConsole.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }).start();
    }

    void update(@NotNull String str) {
        parse(str);
        appendOutput(str);
    }

    private void appendOutput(String str) {
        output.append(str + '\n');
    }

    protected void parse(@NotNull String in) {
        if (!in.startsWith(">>>")) {
            return;
        }
        @NotNull String str = in.substring(3);
        System.out.println("Matching " + str);
        @NotNull Matcher m = regex.matcher(str);
        if (!m.matches()) {
            System.out.println("Doesn't match");
            return;
        }
        String fn = m.group(1);
        System.out.println(fn);
        @NotNull Object args = m.group(2).split(",");
        System.out.println(System.currentTimeMillis());
        @NotNull Invocable inv = (Invocable) engine;
        try {
            inv.invokeFunction(fn, args);
        } catch (@NotNull ScriptException | NoSuchMethodException ex) {
            Logger.getLogger(ExternalConsole.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println(System.currentTimeMillis());
    }

    void setOut(@Nullable OutputStream s) {
        input.setEnabled(s != null);
        pw = new PrintWriter(s, true);
        engine.getContext().setWriter(pw);
    }

    /**
     * @return the engine
     */
    ScriptEngine getEngine() {
        return engine;
    }
}
