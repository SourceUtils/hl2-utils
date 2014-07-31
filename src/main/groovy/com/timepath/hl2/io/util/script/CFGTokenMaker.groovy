package com.timepath.hl2.io.util.script

import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import groovy.util.logging.Log
import org.fife.ui.rsyntaxtextarea.AbstractTokenMaker
import org.fife.ui.rsyntaxtextarea.Token
import org.fife.ui.rsyntaxtextarea.TokenMap
import org.fife.ui.rsyntaxtextarea.TokenTypes

import javax.swing.text.Segment
import java.util.logging.Level

/**
 * @author TimePath
 */
@CompileStatic
@TypeChecked
@Log('LOG')
class CFGTokenMaker extends AbstractTokenMaker {

    @Override
    public String[] getLineCommentStartAndEnd(int languageIndex) { ["//", null] as String[] }

    @Override
    public boolean getMarkOccurrencesOfTokenType(int type) { type == TokenTypes.VARIABLE }

    private int currentTokenStart, currentTokenType;

    @Override
    public Token getTokenList(Segment text, int startTokenType, int startOffset) {
        resetTokenList();

        char[] array = text.array;
        int offset = text.offset;
        int count = text.count;
        int end = offset + count;

        /**
         * When we find a token, its starting position is always of the form:
         * 'startOffset + (currentTokenStart-offset)'; but since startOffset and offset are constant,
         * tokens' starting positions become:
         * 'newStartOffset+currentTokenStart' for one less subtraction operation.
         */
        int newStartOffset = startOffset - offset;

        currentTokenStart = offset;
        currentTokenType = startTokenType;

        for (int i = offset; i < end; i++) {
            char c = array[i]
            switch (currentTokenType) {
                case TokenTypes.NULL: // Starting a new token
                    currentTokenStart = i
                    switch (c) {
                        case ' ':
                        case '\t':
                            currentTokenType = TokenTypes.WHITESPACE
                            break
                        default:
                            currentTokenType = TokenTypes.IDENTIFIER
                            break
                    }
                    break
                case TokenTypes.IDENTIFIER:
                case TokenTypes.WHITESPACE:
                    def next
                    switch (c) {
                        case ' ':
                        case '\t':
                            if (currentTokenType == TokenTypes.WHITESPACE) continue
                            next = TokenTypes.WHITESPACE
                            break
                        default:
                            if (currentTokenType == TokenTypes.IDENTIFIER) continue
                            next = TokenTypes.IDENTIFIER
                            break
                    }
                    // Context changed: add the previous token
                    addToken(text, currentTokenStart, i - 1, currentTokenType, newStartOffset + currentTokenStart)
                    currentTokenStart = i
                    currentTokenType = next
                    break
            }
        }
        if (currentTokenType != TokenTypes.NULL) {
            addToken(text, currentTokenStart, end - 1, currentTokenType, newStartOffset + currentTokenStart);
        }
        addNullToken();
        return firstToken
    }

    @Override
    public TokenMap getWordsToHighlight() {
        TokenMap tokenMap = new TokenMap(true);

        def reserved = TokenTypes.RESERVED_WORD
        tokenMap.put("bind", reserved);
        tokenMap.put("exec", reserved);
        tokenMap.put("alias", reserved);
        tokenMap.put("echo", reserved);

        return tokenMap;
    }

/**
 * {@inheritDoc}
 *
 * Checks the token to give it the exact ID it deserves before
 * being passed up to the super method.
 *
 * @param segment <code>Segment</code> to get text from.
 * @param start Start offset in <code>segment</code> of token.
 * @param end End offset in <code>segment</code> of token.
 * @param tokenType The token's type.
 * @param startOffset The offset in the document at which the token occurs.
 */
    @Override
    public void addToken(Segment segment, int start, int end, int tokenType, int startOffset) {
        switch (tokenType) {
            // Since reserved words, functions, and data types are all passed into here
            // as "identifiers," we have to see what the token really is...
            case TokenTypes.IDENTIFIER:
                int value = this.@wordsToHighlight.get(segment, start, end);
                if (value != -1) tokenType = value;
                break;
            case TokenTypes.WHITESPACE:
                break;
            default:
                LOG.log(Level.SEVERE, null, new Exception("Unknown TokenType: '" + tokenType + "'"))
                tokenType = TokenTypes.IDENTIFIER;
                break;
        }
        super.addToken(segment, start, end, tokenType, startOffset);
    }

}
