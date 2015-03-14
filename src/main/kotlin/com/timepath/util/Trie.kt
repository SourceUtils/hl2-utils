package com.timepath.util

import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.platform.platformStatic

/**
 * @author TimePath
 */
public class Trie {
    public val root: TrieMapping = TrieMapping()
    private val cache = HashMap<String, List<String>>()

    public fun add(s: String) {
        var n = root
        for (i in 0..s.length() - 1) {
            val c = s.charAt(i)
            if (n.containsKey(c)) {
                n = n.get(c)
            } else {
                n.put(c, TrieMapping())
                n = n.get(c)
            }
        }
        n.put(null, null) // Terminator
    }

    /**
     * @param s
     * @param depth
     *
     * @return A list of strings, or null
     */
    fun get(s: String, depth: Int): List<String>? {
        return get(s, depth, root)
    }

    public fun get(s: String, depth: Int, n: TrieMapping): List<String>? {
        var depth = depth
        var n = n
        val path = s + n
        LOG.log(Level.INFO, "Searching for ''{0}'' {1} levels down in {2}", array(s, depth, n))
        if (cache.containsKey(path)) {
            LOG.info("Fom cache")
            return cache.get(path)
        }
        val results = LinkedList<String>()
        var i = 0
        run {
            while (i < s.length()) {
                val c = s.charAt(i)
                if (!n.containsKey(c)) {
                    // Return null if a letter is not present in the tree
                    return null
                }
                n = n[c]
                i++
            }
        }
        LOG.log(Level.INFO, "Stopped at {0}, keys: {1}", array<Any>(s.substring(0, i), n.keySet()))
        depth += 1
        val all = LinkedList<TrieMapping>()
        all.add(n)
        var local: MutableCollection<TrieMapping> = LinkedList()
        local.addAll(n.values())
        LOG.log(Level.INFO, "  all {0}", all)
        for (j in 1..depth - 1) {
            LOG.log(Level.INFO, "Depth {0}", j)
            LOG.log(Level.INFO, "  local {0}", local)
            val local2 = LinkedList<TrieMapping>()
            for (tm in local) {
                if (tm == null) {
                    // char mismatch
                    continue
                }
                val chars = tm.keySet()
                LOG.log(Level.INFO, "    examining {0}, {1}", array<Any>(tm, chars))
                for (ch in chars) {
                    if (ch == null) {
                        // exact match
                        results.add(tm.toString())
                    } else {
                        LOG.log(Level.INFO, "      {0}", ch)
                        val tm2 = tm.get(ch)
                        LOG.log(Level.INFO, "        {0}", tm2)
                        local2.add(tm2)
                        if (j == (depth - 1)) {
                            results.add(tm.toString() + "...")
                        }
                    }
                }
            }
            local = LinkedList(local2)
        }
        LOG.info(results.toString())
        cache.put(path, results)
        return results
    }

    public fun contains(s: String): Boolean {
        var n = root
        for (i in 0..s.length() - 1) {
            val c = s.charAt(i)
            if (n.containsKey(c)) {
                n = n.get(c)
            } else {
                return false
            }
        }
        return n.containsKey(null)
    }

    public fun node(s: String): TrieMapping? {
        var n = root
        for (i in 0..s.length() - 1) {
            val c = s.charAt(i)
            if (!n.containsKey(c)) {
                // Return null if a letter is not present in the tree
                return null
            }
            n = n.get(c)
        }
        return n
    }

    private open class Mapping<A, B>() : HashMap<A, B>()

    SuppressWarnings("serial")
    public class TrieMapping : Mapping<Char, TrieMapping>() {

        var key: Char? = null
        var parent: TrieMapping? = null

        override fun put(key: Char?, value: TrieMapping?): TrieMapping? {
            if (value != null) {
                value.parent = this
                value.key = key
            }
            return super.put(key, value)
        }

        override fun toString(): String {
            return if ((parent != null)) (parent!!.toString() + key) else ""
        }
    }

    class object {

        private val LOG = Logger.getLogger(javaClass<Trie>().getName())

        public platformStatic fun main(args: Array<String>) {
            val t = Trie()
            t.add("af")
            t.add("abd")
            t.add("abc")
            t.add("ace")
            t.add("abed")
            LOG.info(t.get("a", 3).toString())
        }
    }
}
