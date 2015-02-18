package com.timepath.swing

import com.timepath.util.Trie

import javax.swing.text.JTextComponent
import java.util.Collections
import java.util.logging.Logger

/**
 * @author TimePath
 */
public class StringAutoCompleter private(comp: JTextComponent, private val trie: Trie, private val depth: Int) : AutoCompleter(comp) {
    private var cached: Boolean = false
    private var last = ""
    private var n: Trie.TrieMapping? = null

    {
        n = trie.root
    }

    override fun updateListData(): Boolean {
        val text = textComponent.getText().toLowerCase()
        if (last == text) {
            return cached
        }
        cached = false
        last = text
        if (text.isEmpty()) {
            return false
        }
        n = trie.node(text.substring(0, text.length() - 1))
        if (n == null) {
            n = trie.root
        }
        val strings = trie.get(java.lang.String.valueOf(text.charAt(text.length() - 1)), depth, n!!)
        if (strings == null) {
            return false
        }
        if (strings.size() <= 1) {
            if (strings.isEmpty()) {
                return false
            } else if (strings.get(0).toLowerCase() == text) {
                return false
            }
        }
        Collections.sort<String>(strings)
        list.setListData(strings.copyToArray())
        list.setSelectedIndex(0)
        cached = true
        return true
    }

    override fun acceptedListItem(selected: String) {
        textComponent.setCaretPosition(0)
        textComponent.setText(selected)
        textComponent.setCaretPosition(selected.length())
    }

    class object {

        private val LOG = Logger.getLogger(javaClass<StringAutoCompleter>().getName())
    }
}