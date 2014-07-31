package com.timepath.hl2.io.util.script
import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import groovy.util.logging.Log
import org.fife.ui.autocomplete.*
import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory
import org.fife.ui.rsyntaxtextarea.ErrorStrip
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory
import org.fife.ui.rtextarea.Gutter
import org.fife.ui.rtextarea.RTextScrollPane

import javax.swing.*
import java.awt.*
/**
 * @author TimePath
 */
@CompileStatic
@TypeChecked
@Log('LOG')
class CFGEdit extends JDialog {
    public static void main(String[] args) {
        SwingUtilities.invokeLater({ new CFGEdit().visible = true })
    }

    public CFGEdit() {
        def atmf = TokenMakerFactory.defaultInstance as AbstractTokenMakerFactory;
        atmf.putMapping("text/cfg", CFGTokenMaker.class.name);

        def textArea = new RSyntaxTextArea(20, 60)
        textArea.addParser(new CFGParser())
        textArea.text = 'bind a "exec null"'
        textArea.syntaxEditingStyle = "text/cfg"
        textArea.codeFoldingEnabled = true

        def scrollPane = new RTextScrollPane(textArea)
        scrollPane.foldIndicatorEnabled = true
        scrollPane.iconRowHeaderEnabled = true
        scrollPane.lineNumbersEnabled = true

        Gutter gutter = scrollPane.gutter;
        gutter.foldIndicatorEnabled = true
        gutter.toggleBookmark(1)
        gutter.bookmarkingEnabled = true

        def cp = new JPanel(new BorderLayout())
        cp.add(scrollPane)
        cp.add(new ErrorStrip(textArea), BorderLayout.LINE_END);

        def provider = createCompletionProvider()
        def ac = new AutoCompletion(provider)
        ac.install(textArea)

        contentPane = cp
        title = "Text Editor Demo"
        defaultCloseOperation = DISPOSE_ON_CLOSE
        pack()
        locationRelativeTo = null
    }

    private CompletionProvider createCompletionProvider() {
        DefaultCompletionProvider provider = new DefaultCompletionProvider()

        provider.addCompletion(new BasicCompletion(provider, "alias"))
        provider.addCompletion(new BasicCompletion(provider, "exec"))
        provider.addCompletion(new BasicCompletion(provider, "bind"))

        def template = 'alias "${}" "${cursor}"'
        provider.addCompletion(new TemplateCompletion(provider, "al", "al", template, "shortdesc", "summary"));

        provider.addCompletion(new TemplateCompletion(provider, "for", "for-loop", 'for (int ${i} = 0; ${i} &lt; ${array}.length; ${i}++) {${cursor}'))

        return provider

    }

}
