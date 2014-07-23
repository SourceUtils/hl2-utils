package com.timepath.hl2;

import com.timepath.steam.io.VDF;
import com.timepath.steam.io.VDFNode;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author TimePath
 */
class VDFDiffTest extends JFrame {

    private static final Logger LOG = Logger.getLogger(VDFDiffTest.class.getName());
    protected JTextArea text1, text2;

    protected VDFDiffTest() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        add(new JSplitPane() {{
            setResizeWeight(.5);
            setLeftComponent(new JScrollPane(text1 = new JTextArea() {{
                setTabSize(4);
                setText("\"A\" {\n" +
                        "\t\"Modified\" {\n" +
                        "\t\t\"Same\"\t\"yes\"\n" +
                        "\t\t\"Similar\"\t\"one\"\n" +
                        "\t\t\"Removed\"\t\"yes\"\n" +
                        "\t}\n" +
                        "\t\"Removed\" {}\n" +
                        "\t\"Same\" {}\n" +
                        "}\n");
            }}));
            setRightComponent(new JScrollPane(text2 = new JTextArea() {{
                setTabSize(4);
                setText("\"B\" {\n" +
                        "\t\"Modified\" {\n" +
                        "\t\t\"Same\"\t\"yes\"\n" +
                        "\t\t\"Similar\"\t\"two\"\n" +
                        "\t\t\"Added\"\t\"yes\"\n" +
                        "\t}\n" +
                        "\t\"New\" {}\n" +
                        "\t\"Same\" {}\n" +
                        "}\n");
            }}));
        }});
        add(new JButton("Diff") {{
            addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    try {
                        VDFNode n1 = VDF.load(new ByteArrayInputStream(text1.getText()
                                                                            .getBytes(StandardCharsets.UTF_8)));
                        VDFNode n2 = VDF.load(new ByteArrayInputStream(text2.getText()
                                                                            .getBytes(StandardCharsets.UTF_8)));
                        n1.getNodes().get(0).rdiff2(n2.getNodes().get(0));
                    } catch(IOException ex) {
                        LOG.log(Level.SEVERE, null, ex);
                    }
                }
            });
        }}, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() { new VDFDiffTest().setVisible(true); }
        });
    }
}
