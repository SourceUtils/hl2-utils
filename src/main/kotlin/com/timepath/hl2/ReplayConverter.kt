package com.timepath.hl2

import com.timepath.hl2.io.demo.DemoHeader
import com.timepath.hl2.io.demo.HL2DEM
import com.timepath.hl2.io.demo.Message
import com.timepath.hl2.io.demo.MessageType
import com.timepath.io.OrderedOutputStream

import java.io.File
import java.io.FileOutputStream
import java.nio.ByteOrder
import kotlin.platform.platformStatic

/**
 * .block conversion:
 * bzcat *.block > joined.dem
 *
 * @author TimePath
 */
class ReplayConverter {
    class object {

        throws(javaClass<Exception>())
        public platformStatic fun main(args: Array<String>) {
            if (args.size() < 2) {
                System.err.println("Usage: java ... input.dem output.dem")
                System.exit(1)
            }
            val input = args[0]
            val output = args[1]
            patch(File(input), File(output))
        }

        throws(javaClass<Exception>())
        public fun patch(input: File, output: File) {
            val demo = HL2DEM.load(input, false)
            val messages = demo.frames
            val last = messages.last()
            if (last.type != MessageType.Stop) {
                // Insert artificial stop
                val stop = Message(demo, MessageType.Stop, last.tick)
                messages.add(stop)
            }

            var flag = false // Injected
            val ticks = last.tick // Highest tick
            var frames = 0 // Number of MessageType.Packet messages
            run {
                val iterator = messages.listIterator()
                while (iterator.hasNext()) {
                    val message = iterator.next()
                    if (message.type == MessageType.Packet) {
                        frames++
                    } else if (message.type == MessageType.Synctick && !flag) {
                        val injection = Message(demo, MessageType.ConsoleCmd, message.tick)
                        injection.setData("tv_transmitall 1".getBytes())
                        iterator.add(injection)
                        flag = true
                    }
                }
            }
            val header = demo.header
            header.ticks = ticks
            header.frames = frames
            header.playbackTime = ticks.toFloat() / (66f + (2f / 3f))
            val out = OrderedOutputStream(FileOutputStream(output))
            out.order(ByteOrder.LITTLE_ENDIAN)
            out.writeStruct<DemoHeader>(header)
            for (message in demo.frames) {
                message.write(out)
            }
        }
    }
}
