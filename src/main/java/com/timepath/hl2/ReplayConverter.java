package com.timepath.hl2;

import com.timepath.hl2.io.demo.DemoHeader;
import com.timepath.hl2.io.demo.HL2DEM;
import com.timepath.hl2.io.demo.Message;
import com.timepath.hl2.io.demo.MessageType;
import com.timepath.io.OrderedOutputStream;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteOrder;
import java.util.List;
import java.util.ListIterator;

/**
 * .block conversion:
 * (for f in *.block; do bzcat $f; done) > join.dem
 *
 * @author TimePath
 */
public class ReplayConverter {

    public static void main(String[] args) throws Exception {
        if(args.length < 2) {
            System.err.println("Usage: java ... input.dem output.dem");
            System.exit(1);
        }
        String input = args[0];
        String output = args[1];
        patch(input, output);
    }

    public static void patch(String input, String output) throws Exception {
        HL2DEM demo = HL2DEM.load(new File(input), false);
        List<Message> messages = demo.getFrames();
        Message last = messages.get(messages.size() - 1);
        if(last.type != MessageType.Stop) { // Insert artificial stop
            Message stop = new Message(demo, MessageType.Stop, last.tick);
            messages.add(stop);
        }

        boolean flag = false; // Injected
        int ticks = last.tick; // Highest tick
        int frames = 0; // Number of MessageType.Packet messages
        for(ListIterator<Message> iterator = messages.listIterator(); iterator.hasNext(); ) {
            Message message = iterator.next();
            if(message.type == MessageType.Packet) {
                frames++;
            } else if(message.type == MessageType.Synctick && !flag) {
                Message injection = new Message(demo, MessageType.ConsoleCmd, message.tick);
                injection.setData("tv_transmitall 1".getBytes());
                iterator.add(injection);
                flag = true;
            }
        }
        DemoHeader header = demo.getHeader();
        header.ticks = ticks;
        header.frames = frames;
        header.playbackTime = ticks / ( 66f + ( 2f / 3f ) );
        OrderedOutputStream out = new OrderedOutputStream(new FileOutputStream(output));
        out.order(ByteOrder.LITTLE_ENDIAN);
        out.writeStruct(header);
        for(Message message : demo.getFrames()) {
            message.write(out);
        }
    }
}
