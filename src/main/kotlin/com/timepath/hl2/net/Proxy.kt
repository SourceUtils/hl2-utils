package com.timepath.hl2.net

import com.timepath.with
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.concurrent.thread

fun main(args: Array<String>) {
    require(args.size() == 3, "Usage: <proxyPort> <serverAddr> <serverPort>")
    // bind to localhost instead of broadcast, can break TF2 otherwise
    val proxyAddr = InetAddress.getLocalHost();
    val proxyPort = args[0].toInt()
    val serverAddr = args[1].let { InetAddress.getByName(it) }
    val serverPort = args[2].toInt()
    // getHostAddress to remove hostname, easier to read
    println("Proxy listening on ${proxyAddr.getHostAddress()}:$proxyPort, forwarding to ${serverAddr.getHostAddress()}:$serverPort")
    setupRoutes(DatagramSocket(proxyPort, proxyAddr), serverAddr, serverPort, {})
}

fun setupRoutes(proxy: DatagramSocket, serverAddr: InetAddress, serverPort: Int, handle: (DatagramPacket) -> Unit) {
    val writerS = "proxyclient"
    val readerS = "proxyserver"
    val clientS = "client"
    val serverS = "server"

    val server = DatagramSocket() with { connect(serverAddr, serverPort) }
    var client: DatagramSocket

    val packet = packet()
    route(packet, "$writerS <- $clientS", proxy, "$writerS -> $serverS", server) {
        println("client is ${it.getAddress() to it.getPort()}")
        client = DatagramSocket() with { connect(it.getSocketAddress()) }
        handle(it)
        it.setAddress(null)
    }
    routeThread(writerS, packet, clientS, proxy, serverS, server) {
        handle(it)
        it.setAddress(null)
    }
    routeThread(readerS, packet(), serverS, server, clientS, proxy) {
        it.setSocketAddress(client.getRemoteSocketAddress())
    }
}

val SOURCE_MAX_BYTES = 1400

fun packet() = ByteArray(SOURCE_MAX_BYTES) let { DatagramPacket(it, it.size()) }

inline fun routeThread(name: String, packet: DatagramPacket,
                 fromS: String, from: DatagramSocket,
                 toS: String, to: DatagramSocket,
                 @inlineOptions(InlineOption.ONLY_LOCAL_RETURN) handler: (DatagramPacket) -> Unit) {
    thread(name = "$fromS -> $name -> $toS") {
        while (true) {
            route(packet, "$name <- $fromS", from, "$name -> $toS", to, handler)
        }
    }
}

inline fun route(packet: DatagramPacket,
                 fromS: String, from: DatagramSocket,
                 toS: String, to: DatagramSocket,
                 handler: (DatagramPacket) -> Unit) {
    from.receive(packet)
    handler(packet)
    to.send(packet)
}

inline fun log(msg: String, f: () -> Unit) = run { println(msg + "...");  f();  println(msg) }
