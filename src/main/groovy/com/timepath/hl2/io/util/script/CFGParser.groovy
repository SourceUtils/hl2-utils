package com.timepath.hl2.io.util.script

import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import groovy.util.logging.Log
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument
import org.fife.ui.rsyntaxtextarea.parser.*

import javax.swing.text.Element

/**
 * @author TimePath
 */
@CompileStatic
@TypeChecked
@Log('LOG')
class CFGParser extends AbstractParser {

    private DefaultParseResult result;

    public CFGParser() {
        result = new DefaultParseResult(this);
    }

    public ParseResult parse(RSyntaxDocument doc, String style) {
        result.clearNotices();
        Element root = doc.getDefaultRootElement();
        result.setParsedLines(0, root.getElementCount() - 1);

        if (doc.getLength() == 0) return result;

        DefaultParserNotice pn = new DefaultParserNotice(this, "Test", 0, 0, 3);
        pn.setLevel(ParserNotice.Level.INFO);
        result.addNotice(pn);

        result.addNotice(new DefaultParserNotice(this, "Whole", 0, -1, -1));

        return result;

    }

}

