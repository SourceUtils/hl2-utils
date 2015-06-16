package com.timepath.hl2.cvar

import java.util.Arrays
import java.util.Scanner
import java.util.logging.Level
import java.util.logging.Logger
import java.util.regex.Pattern

/**
 * @author TimePath
 */
object CVarList {

    private val LOG = Logger.getLogger(javaClass<CVarList>().getName())

    public fun analyzeList(scanner: Scanner, map: MutableMap<String, CVar>): Map<String, CVar> {
        var c: CVar? = null
        val cvarlist = Pattern.compile("([\\S]*)[\\s]*:[\\s]*([\\S]*)[\\s]*:[^\"]*(.*)[\\s]*:[\\s]*(.*)")
        val tag = Pattern.compile("\"([^\"]*)\"")
        val kv = Pattern.compile("\"([^\"]*)\"[\\s]*=[\\s]*\"([^\"]*)\"[\\s]*(.*)")
        val desc = Pattern.compile("^(?: - | )(.*)")
        val defaultValue = Pattern.compile("\\([^\"]*\"(.*)\"[^\"]*\\)")
        val minValue = Pattern.compile("(?:min\\.\\s)(.*?)(?:\\s|$)")
        val maxValue = Pattern.compile("(?:max\\.\\s)(.*?)(?:\\s|$)")
        while (scanner.hasNext()) {
            val line = scanner.nextLine()
            if (line.trim().isEmpty()) {
                continue
            }
            LOG.fine(line)
            val cvarlistMatcher = cvarlist.matcher(line)
            val kvMatcher = kv.matcher(line)
            val descMatcher = desc.matcher(line)
            if (kvMatcher.find()) {
                //                    LOG.info("KV match");
                val name = kvMatcher.group(1)
                val value = kvMatcher.group(2)
                if (!map.containsKey(name)) {
                    c = CVar()
                    c.name = name
                    c.value = value
                } else {
                    c = map[name]
                }
                val extra = kvMatcher.group(3)
                if (!extra.isEmpty()) {
                    val defaultMatcher = defaultValue.matcher(extra)
                    if (defaultMatcher.find()) {
                        c!!.defaultValue = defaultMatcher.group(1)
                    }
                    val minMatcher = minValue.matcher(extra)
                    if (minMatcher.find()) {
                        c!!.minimum = minMatcher.group(1)
                    }
                    val maxMatcher = maxValue.matcher(extra)
                    if (maxMatcher.find()) {
                        c!!.maximum = maxMatcher.group(1)
                    }
                }
            } else if (cvarlistMatcher.find()) {
                //                    LOG.info("cvarlist match");
                c = CVar()
                c.name = cvarlistMatcher.group(1)
                c.value = cvarlistMatcher.group(2)
                val tagMatcher = tag.matcher(cvarlistMatcher.group(3))
                while (tagMatcher.find()) {
                    c.tags.add(tagMatcher.group())
                }
                c.desc = cvarlistMatcher.group(4)
            } else if (descMatcher.find()) {
                //                    LOG.info("Additional info match");
                if (c == null) {
                    LOG.warning("Data before a cvar")
                    continue
                }
                val trimmed = descMatcher.group(1)
                val description = line.startsWith(" - ")
                if (description) {
                    c.desc = trimmed
                } else {
                    c.tags.addAll(trimmed.splitBy(" "))
                }
            } else {
                LOG.log(Level.INFO, "Unparsed: {0}", line)
                continue
            }
            if (map.containsKey(c!!.name)) {
                LOG.log(Level.INFO, "Duplicate: {0}", line)
            }
            map.put(c.name!!, c)
        }
        return map
    }
}
