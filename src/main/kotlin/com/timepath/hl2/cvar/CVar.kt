package com.timepath.hl2.cvar

import java.util.LinkedList
import java.util.logging.Logger

/**
 * @author TimePath
 */
public data class CVar(
        public val tags: MutableList<String> = LinkedList(),
        public var defaultValue: Any? = null,
        public var desc: String? = null,
        public var maximum: Any? = null,
        public var minimum: Any? = null,
        public var name: String? = null,
        /** null if cmd */
        public var value: Any? = null) {

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append(name).append(" : ").append(value).append(" : ")
        for (tag in tags) {
            sb.append(", ").append('"').append(tag).append('"')
        }
        sb.append(" : ").append(desc)
        return sb.toString()
    }

    companion object {

        private val LOG = Logger.getLogger(javaClass<CVar>().getName())
    }
}
