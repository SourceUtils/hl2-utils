package com.timepath.ui.swing;

import com.timepath.utils.Trie;
import com.timepath.utils.Trie.TrieMapping;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.text.JTextComponent;

/**
 *
 * @author TimePath
 */
public class StringAutoCompleter extends AutoCompleter {

    private static final Logger LOG = Logger.getLogger(StringAutoCompleter.class.getName());

    private boolean cached = false;

    private final int depth;

    private String last = "";

    private TrieMapping n;

    private final Trie trie;

    public StringAutoCompleter(JTextComponent comp, Trie trie, int depth) {
        super(comp);
        this.trie = trie;
        this.n = trie.root;
        this.depth = depth;
    }

    protected void acceptedListItem(String selected) {
        if(selected == null) {
            return;
        }
        
        textComponent.setCaretPosition(0);
        textComponent.setText(selected);
        textComponent.setCaretPosition(selected.length());
    }

    protected boolean updateListData() {
        String text = textComponent.getText().toLowerCase();
        if(last.equals(text)) {
            return cached;
        }
        cached = false;
        last = text;
        if(text.length() == 0) {
            return false;
        }
        n = trie.node(text.substring(0, text.length() - 1));
        if(n == null) {
            n = trie.root;
        }

        List<String> strings = trie.get(text.charAt(text.length() - 1) + "", depth, n);
        if(strings == null) {
            return false;
        }
        if(strings.size() <= 1) {
            if(strings.isEmpty()) {
                return false;
            } else if(strings.get(0).toLowerCase().equals(text)) {
                return false;
            }
        }
        Collections.sort(strings);
        list.setListData(strings.toArray(new String[strings.size()]));
        list.setSelectedIndex(0);
        cached = true;
        return true;
    }

}
