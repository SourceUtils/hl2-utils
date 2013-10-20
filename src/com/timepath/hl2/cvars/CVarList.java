package com.timepath.hl2.cvars;

import java.util.Arrays;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * http://gskinner.com/RegExr/
 *
 * @author TimePath
 */
public class CVarList {

    private static final Logger LOG = Logger.getLogger(CVarList.class.getName());

    public static Map<String, CVar> analyzeList(Scanner scanner, Map<String, CVar> map) {
        CVar c = null;
        Pattern cvarlist = Pattern.compile(
            "([\\S]*)[\\s]*:[\\s]*([\\S]*)[\\s]*:[^\\\"]*(.*)[\\s]*:[\\s]*(.*)");
        Pattern tag = Pattern.compile("\\\"([^\\\"]*)\\\"");
        Pattern kv = Pattern.compile("\\\"([^\"]*)\\\"[\\s]*=[\\s]*\\\"([^\"]*)\\\"[\\s]*(.*)");
        Pattern desc = Pattern.compile("^(?: - | )(.*)");
        Pattern defaultValue = Pattern.compile("\\([^\"]*\"(.*)\"[^\"]*\\)");
        Pattern minValue = Pattern.compile("(?:min\\.\\s)(.*?)(?:\\s|$)");
        Pattern maxValue = Pattern.compile("(?:max\\.\\s)(.*?)(?:\\s|$)");
        while(scanner.hasNext()) {
            String line = scanner.nextLine();
            if(line.trim().length() == 0) {
                continue;
            }
            LOG.fine(line);
            Matcher cvarlistMatcher = cvarlist.matcher(line);
            Matcher kvMatcher = kv.matcher(line);
            Matcher descMatcher = desc.matcher(line);
            if(kvMatcher.find()) {
//                    LOG.info("KV match");
                String name = kvMatcher.group(1);
                Object value = kvMatcher.group(2);
                if(!map.containsKey(name)) {
                    c = new CVar();
                    c.setName(name);
                    c.setValue(value);
                } else {
                    c = map.get(name);
                }
                String extra = kvMatcher.group(3);
                if(extra.length() > 0) {
                    Matcher defaultMatcher = defaultValue.matcher(extra);
                    if(defaultMatcher.find()) {
                        c.setDefaultValue(defaultMatcher.group(1));
                    }
                    Matcher minMatcher = minValue.matcher(extra);
                    if(minMatcher.find()) {
                        c.setMinimum(minMatcher.group(1));
                    }
                    Matcher maxMatcher = maxValue.matcher(extra);
                    if(maxMatcher.find()) {
                        c.setMaximum(maxMatcher.group(1));
                    }
                }
            } else if(cvarlistMatcher.find()) {
//                    LOG.info("cvarlist match");
                c = new CVar();
                c.setName(cvarlistMatcher.group(1));
                c.setValue(cvarlistMatcher.group(2));
                Matcher tagMatcher = tag.matcher(cvarlistMatcher.group(3));
                while(tagMatcher.find()) {
                    c.getTags().add(tagMatcher.group());
                }
                c.setDesc(cvarlistMatcher.group(4));
            } else if(descMatcher.find()) {
//                    LOG.info("Additional info match");
                if(c == null) {
                    LOG.warning("Data before a cvar");
                    continue;
                }
                String trimmed = descMatcher.group(1);
                boolean description = line.startsWith(" - ");
                if(description) {
                    c.setDesc(trimmed);
                } else {
                    c.getTags().addAll(Arrays.asList(trimmed.split(" ")));
                }
            } else {
                LOG.log(Level.INFO, "Unparsed: {0}", line);
                continue;
            }
            if(map.containsKey(c.getName())) {
                LOG.log(Level.INFO, "Duplicate: {0}", line);
            }
            map.put(c.getName(), c);
        }
        return map;
    }

    private CVarList() {
    }

}
