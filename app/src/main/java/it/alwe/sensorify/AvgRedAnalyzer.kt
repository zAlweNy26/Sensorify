package it.alwe.sensorify

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

typealias AvgRedListener = (avgRed: Double) -> Unit
class AvgRedAnalyzer(private val listener: AvgRedListener) : ImageAnalysis.Analyzer {

    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()
        val data = ByteArray(remaining())
        get(data)
        return data
    }

    private fun getRGBfromYUV(image: ImageProxy): Triple<Double, Double, Double> {
        val planes = image.planes

        val height = image.height
        val width = image.width

        // Y
        val yArr = planes[0].buffer
        val yArrByteArray = yArr.toByteArray()
        val yPixelStride = planes[0].pixelStride
        val yRowStride = planes[0].rowStride

        // U
        val uArr = planes[1].buffer
        val uArrByteArray =uArr.toByteArray()
        val uPixelStride = planes[1].pixelStride
        val uRowStride = planes[1].rowStride

        // V
        val vArr = planes[2].buffer
        val vArrByteArray = vArr.toByteArray()
        val vPixelStride = planes[2].pixelStride
        val vRowStride = planes[2].rowStride

        val y = yArrByteArray[(height * yRowStride + width * yPixelStride) / 2].toInt() and 255
        val u = (uArrByteArray[(height * uRowStride + width * uPixelStride) / 4].toInt() and 255) - 128
        val v = (vArrByteArray[(height * vRowStride + width * vPixelStride) / 4].toInt() and 255) - 128

        val r = y + (1.370705 * v)
        val g = y - (0.698001 * v) - (0.337633 * u)
        val b = y + (1.732446 * u)

        return Triple(r,g,b)
    }

    private fun decodeYUV420SPtoRedSum(yuv420sp: ByteArray?, width: Int, height: Int): Int {
        if (yuv420sp == null) return 0
        val frameSize = width * height
        var sum = 0
        var j = 0
        var yp = 0
        while (j < height) {
            var uvp = frameSize + (j shr 1) * width
            var u = 0
            var v = 0
            var i = 0
            while (i < width) {
                var y = (0xff and yuv420sp[yp].toInt()) - 16
                if (y < 0) y = 0
                if (i and 1 == 0) {
                    v = (0xff and yuv420sp[uvp++].toInt()) - 128
                    u = (0xff and yuv420sp[uvp++].toInt()) - 128
                }
                val y1192 = 1192 * y
                var r = y1192 + 1634 * v
                var g = y1192 - 833 * v - 400 * u
                var b = y1192 + 2066 * u
                if (r < 0) r = 0 else if (r > 262143) r = 262143
                if (g < 0) g = 0 else if (g > 262143) g = 262143
                if (b < 0) b = 0 else if (b > 262143) b = 262143
                val pixel = -0x1000000 or (r shl 6 and 0xff0000) or (g shr 2 and 0xff00) or (b shr 10 and 0xff)
                val red = pixel shr 16 and 0xff
                sum += red
                i++
                yp++
            }
            j++
        }
        return sum
    }

    private fun decodeYUV420SPtoRedAvg(yuv420sp: ByteArray?, width: Int, height: Int): Int {
        if (yuv420sp == null) return 0
        val frameSize = width * height
        val sum = decodeYUV420SPtoRedSum(yuv420sp, width, height)
        return sum / frameSize
    }

    override fun analyze(image: ImageProxy) {
        /*val buffer = image.planes[0].buffer
        val data = buffer.toByteArray()
        val pixels = data.map { it.toInt() and 0xFF }
        val luma = pixels.average()*/

        listener(getRGBfromYUV(image).first)

        image.close()
    }
}