package com.timepath.hl2.cvars;

import java.util.ArrayList;
import java.util.logging.Logger;

/**
 *
 * @author TimePath
 */
public class CVar {

    private static final Logger LOG = Logger.getLogger(CVar.class.getName());

    private Object defaultValue;

    private String desc;

    private Object maximum;

    private Object minimum;

    private String name;

    private final ArrayList<String> tags = new ArrayList<String>();

    /**
     * null if cmd
     */
    private Object value;

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof CVar) {
            return toString().equals(obj.toString());
        }
        return false;
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

    public ArrayList<String> getTags() {
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
        hash = 97 * hash + (this.defaultValue != null ? this.defaultValue.hashCode() : 0);
        hash = 97 * hash + (this.desc != null ? this.desc.hashCode() : 0);
        hash = 97 * hash + (this.maximum != null ? this.maximum.hashCode() : 0);
        hash = 97 * hash + (this.minimum != null ? this.minimum.hashCode() : 0);
        hash = 97 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 97 * hash + (this.tags != null ? this.tags.hashCode() : 0);
        hash = 97 * hash + (this.value != null ? this.value.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" : ").append(value).append(" : ");
        for(String tag : tags) {
            sb.append(", ").append("\"").append(tag).append("\"");
        }
        sb.append(" : ").append(desc);
        return sb.toString();
    }

}
