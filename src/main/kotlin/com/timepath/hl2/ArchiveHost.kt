package com.timepath.hl2

import com.timepath.hl2.io.bsp.BSP
import java.io.IOException
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.platform.platformStatic

/**
 * @author TimePath
 */
object ArchiveHost {

    private val LOG = Logger.getLogger(javaClass<ArchiveHost>().getName())
    //
    public platformStatic fun main(args: Array<String>) {
        try {
            Class.forName(javaClass<BSP>().getName())
            //            SimpleVFile.registerMissingFileHandler(object : MissingFileHandler {
            //                override fun handle(parent: SimpleVFile, name: String): SimpleVFile? {
            //                    if (!name.endsWith(".png")) return null
            //                    val s = name.replace(".png", ".vtf")
            //                    val vtf = parent.get(s)
            //                    if (vtf == null) return null
            //                    return object : SimpleVFile() {
            //                        private var data = SoftReference<ByteArray>(null)
            //
            //                        override fun getName(): String {
            //                            return name
            //                        }
            //
            //                        override fun isDirectory(): Boolean {
            //                            return false
            //                        }
            //
            //                        override fun openStream(): InputStream? {
            //                            var arr = data.get()
            //                            if (arr == null) {
            //                                try {
            //                                    LOG.log(Level.INFO, "Converting {0}...", vtf)
            //                                    val v = VTF.load(vtf!!.openStream())
            //                                    if (v == null) {
            //                                        return null
            //                                    }
            //                                    val image = v!!.getImage(Math.min(1, v!!.getMipCount() - 1))
            //                                    if (image == null) {
            //                                        return null
            //                                    }
            //                                    val baos = ByteArrayOutputStream()
            //                                    ImageIO.write(image as RenderedImage, "png", baos)
            //                                    arr = baos.toByteArray()
            //                                    data = SoftReference<ByteArray>(arr)
            //                                } catch (e: IOException) {
            //                                    LOG.log(Level.SEVERE, null, e)
            //                                } finally {
            //                                    LOG.log(Level.INFO, "Converted {0}", vtf)
            //                                }
            //                            }
            //                            return ByteArrayInputStream(arr)
            //                        }
            //                    }
            //                }
            //            })
            //            val appID = 440
            //            val acf = ACF.fromManifest(appID)
            //            val files = acf.list()
            //            try {
            //                val http = HttpServer()
            //                http.addAll(files)
            //                Thread(http).start()
            //            } catch (ex: IOException) {
            //                LOG.log(Level.SEVERE, null, ex)
            //            }
            //
            //            try {
            //                val ftp = FtpServer()
            //                ftp.addAll(files)
            //                Thread(ftp).start()
            //            } catch (ex: IOException) {
            //                LOG.log(Level.SEVERE, null, ex)
            //            }
            //
            //            val fuse = FuseServer("test")
            //            fuse.addAll(files)
            //            Thread(fuse).start()
        } catch (ex: ClassNotFoundException) {
            LOG.log(Level.SEVERE, null, ex)
        } catch (ex: IOException) {
            LOG.log(Level.SEVERE, null, ex)
        }

        //        SwingUtilities.invokeLater(object : Runnable {
        //            override fun run() {
        //                JOptionPane.showMessageDialog(null, "Navigate to ftp://localhost:2121. The files will stop being hosted " + "when you close all running instances", "Files hosted", JOptionPane.INFORMATION_MESSAGE, null)
        //            }
        //        })
    }
}
