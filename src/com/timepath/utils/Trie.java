package com.timepath.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author TimePath
 */
public class Trie {

    private static final Logger LOG = Logger.getLogger(Trie.class.getName());

    public static void main(String[] args) {
        Trie t = new Trie();
        t.add("af");
        t.add("abd");
        t.add("abc");
        t.add("ace");
        t.add("abed");
        LOG.info(t.get("a", 3).toString());
    }

    public TrieMapping root = new TrieMapping();

    HashMap<String, List<String>> cache = new HashMap<String, List<String>>();

    public void add(String s) {
        TrieMapping n = root;
        for(int i = 0; i < s.length(); i++) {
            Character c = s.charAt(i);
            if(n.containsKey(c)) {
                n = n.get(c);
            } else {
                n.put(c, new TrieMapping());
                n = n.get(c);
            }
        }
        n.put(null, null); // Terminator
    }

    /**
     *
     * @param s
     * @param depth
     *              <p>
     * @return A list of strings, or null
     */
    public List<String> get(String s, int depth) {
        return get(s, depth, root);
    }

    public TrieMapping node(String s) {
        TrieMapping n = root;
        for(int i = 0; i < s.length(); i++) {
            Character c = s.charAt(i);
            if(!n.containsKey(c)) { // Return null if a letter is not present in the tree
                return null;
            }
            n = n.get(c);
        }
        return n;
    }

    public List<String> get(String s, int depth, TrieMapping n) {
        String path = s + n.toString();
        LOG.log(Level.INFO, "Searching for ''{0}'' {1} levels down in {2}", new Object[] {s, depth,
                                                                                             n});
        if(cache.containsKey(path)) {
            LOG.info("Fom cache");
            return cache.get(path);
        }
        ArrayList<String> results = new ArrayList<String>();
        int i = 0;
        Character c = null;
        for(i = 0; i < s.length(); i++) {
            c = s.charAt(i);
            if(!n.containsKey(c)) { // Return null if a letter is not present in the tree
                return null;
            }
            n = n.get(c);
        }
        LOG.log(Level.INFO, "Stopped at {0}, keys: {1}",
                new Object[] {s.substring(0, i), n.keySet()});

        depth += 1;
        ArrayList<TrieMapping> all = new ArrayList<TrieMapping>();
        all.add(n);
        ArrayList<TrieMapping> local = new ArrayList<TrieMapping>();
        local.addAll(n.values());
        LOG.log(Level.INFO, "  all {0}", all);
        for(int j = 1; j < depth; j++) {
            LOG.log(Level.INFO, "Depth {0}", j);
            LOG.log(Level.INFO, "  local {0}", local);
            ArrayList<TrieMapping> local2 = new ArrayList<TrieMapping>();
            for(TrieMapping tm : local) {
                if(tm == null) { // char mismatch
                    continue;
                }
                Set<Character> chars = tm.keySet();
                 LOG.log(Level.INFO, "    examining {0}, {1}", new Object[] {tm, chars});
                for(Character ch : chars) {
                    if(ch == null) { // exact match
                        results.add(tm.toString());
                    } else {
                        LOG.log(Level.INFO, "      {0}", ch);
                        TrieMapping tm2 = tm.get(ch);
                        LOG.log(Level.INFO, "        {0}", tm2);
                        local2.add(tm2);
                        if(j == depth - 1) {
                            results.add(tm.toString() + "...");
                        }
                    }
                }
            }
            local = new ArrayList<TrieMapping>(local2);
        }

        LOG.info(results.toString());
        cache.put(path, results);
        return results;
    }

    public boolean contains(String s) {
        TrieMapping n = root;
        for(int i = 0; i < s.length(); i++) {
            Character c = s.charAt(i);
            if(n.containsKey(c)) {
                n = n.get(c);
            } else {
                return false;
            }
        }
        return n.containsKey(null);
    }

    public class TrieMapping extends Mapping<Character, TrieMapping> {

        TrieMapping parent;

        Character key;

        @Override
        public TrieMapping put(Character key, TrieMapping value) {
            if(value != null) {
                value.parent = this;
                value.key = key;
            }
            return super.put(key, value);
        }

        @Override
        public String toString() {
            return (parent != null ? parent.toString() + "" + key : "");
        }

    }

    private class Mapping<A, B> extends HashMap<A, B> {
    }

}
