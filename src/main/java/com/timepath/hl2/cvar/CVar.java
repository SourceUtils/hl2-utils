package com.timepath.hl2.cvar;

import java.util.Collection;
import java.util.LinkedList;
import java.util.logging.Logger;

/**
 * @author TimePath
 */
public class CVar {

    private static final Logger LOG = Logger.getLogger(CVar.class.getName());
    private final Collection<String> tags = new LinkedList<>();
    private Object defaultValue;
    private String desc;
    private Object maximum;
    private Object minimum;
    private String name;
    /**
     * null if cmd
     */
    private Object value;

    CVar() {
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public Object getMaximum() {
        return maximum;
    }

    public void setMaximum(Object maximum) {
        this.maximum = maximum;
    }

    public Object getMinimum() {
        return minimum;
    }

    public void setMinimum(Object minimum) {
        this.minimum = minimum;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Collection<String> getTags() {
        return tags;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + ((defaultValue != null) ? defaultValue.hashCode() : 0);
        hash = 97 * hash + ((desc != null) ? desc.hashCode() : 0);
        hash = 97 * hash + ((maximum != null) ? maximum.hashCode() : 0);
        hash = 97 * hash + ((minimum != null) ? minimum.hashCode() : 0);
        hash = 97 * hash + ((name != null) ? name.hashCode() : 0);
        hash = 97 * hash + ((tags != null) ? tags.hashCode() : 0);
        hash = 97 * hash + ((value != null) ? value.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CVar) {
            return toString().equals(obj.toString());
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" : ").append(value).append(" : ");
        for (String tag : tags) {
            sb.append(", ").append('"').append(tag).append('"');
        }
        sb.append(" : ").append(desc);
        return sb.toString();
    }
}
