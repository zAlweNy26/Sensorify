package it.alwe.sensorify.sensors

import android.app.ActivityManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Point
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.CamcorderProfile
import android.nfc.NfcManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Display
import it.alwe.sensorify.BaseBlockActivity
import it.alwe.sensorify.R
import kotlinx.android.synthetic.main.activity_system.*
import java.io.IOException
import java.io.InputStream
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow
import kotlin.math.sqrt

class SystemActivity : BaseBlockActivity() {
    private var totRAM: String? = null
    private var totStorage: String? = null
    private var timer: Timer? = null

    override val contentView: Int
        get() = R.layout.activity_system

    // TODO : USARE HardwarePropertiesManager per le informazioni in tempo reale riguardanti CPU e GPU
    // TODO : https://github.com/kamgurgul/cpu-info per info su CPU

    override fun addCode() {
        val androidName = when(Build.VERSION.SDK_INT) {
            Build.VERSION_CODES.LOLLIPOP, Build.VERSION_CODES.LOLLIPOP_MR1 -> "Lollipop"
            Build.VERSION_CODES.M -> "Marshmallow"
            Build.VERSION_CODES.N,  Build.VERSION_CODES.N_MR1 -> "Nougat"
            Build.VERSION_CODES.O,  Build.VERSION_CODES.O_MR1 -> "Oreo"
            Build.VERSION_CODES.P -> "Pie"
            Build.VERSION_CODES.Q -> "Q"
            Build.VERSION_CODES.R -> "R"
            else -> getString(R.string.unavailableValue)
        }

        androidVersion.text = "${Build.VERSION.RELEASE} ($androidName)"
        sdkVersion.text = Build.VERSION.SDK_INT.toString()

        if (Build.VERSION.SDK_INT >= 23) {
            val originalFormat: DateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
            val targetFormat: DateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ROOT)
            val date: Date = originalFormat.parse(Build.VERSION.SECURITY_PATCH)!!
            val formattedDate: String = targetFormat.format(date)
            secPatch.text = formattedDate
        } else secPatch.text = getString(R.string.unavailableValue)

        interfaceVersion.text = Build.VERSION.INCREMENTAL
        androidID.text = Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ANDROID_ID) ?: getString(R.string.unavailableValue)

        var supportedABIs = " "
        for (abi in Build.SUPPORTED_ABIS) supportedABIs += "$abi, "

        suppABIs.text = supportedABIs.substring(0, supportedABIs.length - 2)
        brand.text = Build.BRAND.replaceFirstChar { it.uppercase() }

        val internalStatFs = StatFs(Environment.getDataDirectory().absolutePath)
        val activityManager = applicationContext.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        val displayMetrics = DisplayMetrics()
        val realSize = Point()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val display = this.display
            display?.getRealMetrics(displayMetrics)
            Display::class.java.getMethod("getRealSize", Point::class.java).invoke(display, realSize)
        } else {
            @Suppress("DEPRECATION")
            val display = this.windowManager.defaultDisplay
            @Suppress("DEPRECATION")
            display.getMetrics(displayMetrics)
            Display::class.java.getMethod("getRealSize", Point::class.java).invoke(display, realSize)
        }

        val dpiX = (realSize.x / displayMetrics.xdpi).pow(2)
        val dpiY = (realSize.y / displayMetrics.ydpi).pow(2)
        val screenInches = "%.2f".format(sqrt(dpiX + dpiY))

        manufacturer.text = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        model.text = "${Build.MODEL.replaceFirstChar { it.uppercase() }} (${Build.DEVICE})"
        cpu.text = getCPUModel()
        cpuFreq.text = getCPUFrequencyCurrent()

        totRAM = getFromBytes(memoryInfo.totalMem)
        totStorage = getFromBytes((internalStatFs.availableBlocksLong * internalStatFs.blockSizeLong) + (internalStatFs.blockCountLong * internalStatFs.blockSizeLong))

        /*ram.text = "${getFromBytes(memoryInfo.totalMem - memoryInfo.availMem)} / ${getFromBytes(memoryInfo.totalMem)}"
        storage.text = "${getFromBytes(internalStatFs.blockCountLong * internalStatFs.blockSizeLong)} / " +
            "${getFromBytes((internalStatFs.availableBlocksLong * internalStatFs.blockSizeLong) + 
                    (internalStatFs.blockCountLong * internalStatFs.blockSizeLong))}"*/

        ramArc.progress = ((((memoryInfo.totalMem / 1048576.0) - (memoryInfo.availMem / 1048576.0)) / (memoryInfo.totalMem / 1048576.0)) * 100.0).toFloat()
        storageArc.progress = ((((internalStatFs.blockCountLong * internalStatFs.blockSizeLong) / 1048576.0)
            / (((internalStatFs.availableBlocksLong * internalStatFs.blockSizeLong) +
            (internalStatFs.blockCountLong * internalStatFs.blockSizeLong)) / 1048576.0)) * 100.0).toFloat()

        ramArc.belowText = getString(R.string.of, getFromBytes(memoryInfo.totalMem))
        storageArc.belowText = getString(R.string.of, getFromBytes((internalStatFs.availableBlocksLong * internalStatFs.blockSizeLong) +
            (internalStatFs.blockCountLong * internalStatFs.blockSizeLong)))

        nfc.text = if ((getSystemService(NFC_SERVICE) as NfcManager).defaultAdapter != null) getString(R.string.yesValue) else getString(R.string.noValue)
        biometric.text = if (packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)) getString(R.string.yesValue) else getString(R.string.noValue)

        if (packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
            || packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) bluetooth.text = getString(R.string.yesValue)
        else bluetooth.text = getString(R.string.noValue)

        val cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager

        val backCamera = cameraManager.getCameraCharacteristics(cameraManager.cameraIdList[0])
        val backCameraValues = backCamera.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)!!
        bcRes.text = "${(backCameraValues.height * backCameraValues.width) / 1000000} Mp\n(${backCameraValues.width} x ${backCameraValues.height}) px"

        val frontCamera = cameraManager.getCameraCharacteristics(cameraManager.cameraIdList[1])
        val frontCameraValues = frontCamera.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)!!
        fcRes.text = "${(frontCameraValues.height * frontCameraValues.width) / 1000000} Mp\n(${frontCameraValues.width} x ${frontCameraValues.height}) px"

        val backVideo = CamcorderProfile.get(0, CamcorderProfile.QUALITY_HIGH)
        bvRes.text = "${(backVideo.videoFrameWidth * backVideo.videoFrameHeight) / 1000000} Mp\n(${backVideo.videoFrameWidth} x ${backVideo.videoFrameHeight}) px"

        val frontVideo = CamcorderProfile.get(1, CamcorderProfile.QUALITY_HIGH)
        fvRes.text = "${(frontVideo.videoFrameWidth * frontVideo.videoFrameHeight) / 1000000} Mp\n(${frontVideo.videoFrameWidth} x ${frontVideo.videoFrameHeight}) px"

        screenSize.text = "$screenInches in"
        screenDensity.text = "${displayMetrics.densityDpi} dpi"
        screenResolution.text = "(${displayMetrics.widthPixels} x ${displayMetrics.heightPixels}) px"
    }

    private fun getFromBytes(bytes: Long): String {
        return when {
            (bytes / 1073741824.0) > 1 -> "%.2f".format(bytes / 1073741824.0).plus(" GB")
            (bytes / 1048576.0) > 1 -> "%.2f".format(bytes / 1048576.0).plus(" MB")
            (bytes / 1024.0) > 1 -> "%.2f".format(bytes / 1024.0).plus(" KB")
            else -> "%.2f".format(bytes).plus(" Bytes")
        }
    }

    private fun getCPUModel(): String {
        val processBuilder: ProcessBuilder
        var cpuDetails = ""
        val data = arrayOf("/system/bin/cat", "/proc/cpuinfo")
        val inputStream: InputStream?
        val process: Process
        val bArray = ByteArray(1024)
        try {
            processBuilder = ProcessBuilder(*data)
            process = processBuilder.start()
            inputStream = process.inputStream
            while (inputStream?.read(bArray)!! != -1) { cpuDetails += String(bArray) }
            inputStream.close()
        } catch (ex: IOException) { return getString(R.string.unavailableValue) }
        val cpuLines = cpuDetails.lines()
        var finalString = getString(R.string.unavailableValue)
        for (line in cpuLines)
            if (line.contains("Hardware"))
                finalString = line.substring(11)
        return finalString
    }

    private fun getCPUFrequencyCurrent(): String {
        val output = IntArray(Runtime.getRuntime().availableProcessors())
        var finalString = ""
        val freqCount = mutableMapOf<Int, Int>()
        var fileNull = false
        for (i in 0 until Runtime.getRuntime().availableProcessors()) {
            if (readSystemFileAsInt("/sys/devices/system/cpu/cpu$i/cpufreq/scaling_max_freq") != 0) {
                output[i] = readSystemFileAsInt("/sys/devices/system/cpu/cpu$i/cpufreq/scaling_max_freq")
                if (freqCount.contains(output[i])) freqCount[output[i]] = freqCount[output[i]]!! + 1
                else freqCount[output[i]] = 1
                fileNull = false
            } else fileNull = true
        }
        return if (!fileNull) {
            output.forEachIndexed { index, i ->
                if (index > 0) {
                    if (i != output[index - 1]) finalString += "${freqCount[output[index - 1]]} x ${"%.2f".format((i.toFloat() / (1000f * 1000f)))} GHz | "
                } else finalString += "${freqCount[output[index]]} x ${"%.2f".format((i.toFloat() / (1000f * 1000f)))} GHz | "
            }
            finalString.dropLast(3)
        } else finalString
    }

    private fun readSystemFileAsInt(pSystemFile: String): Int {
        val inputStream: InputStream?
        return try {
            val process = ProcessBuilder("/system/bin/cat", pSystemFile).start()
            inputStream = process.inputStream
            val content: String = readFully(inputStream)
            content.toInt()
        } catch (e: Exception) { 0 }
    }

    private fun readFully(pInputStream: InputStream?): String {
        val sb = StringBuilder()
        val sc = Scanner(pInputStream!!)
        while (sc.hasNextLine()) { sb.append(sc.nextLine()) }
        return sb.toString()
    }

    /*override fun onResume() {
        val internalStatFs = StatFs(Environment.getDataDirectory().absolutePath)
        val activityManager = applicationContext.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        timer = Timer()
        timer!!.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    val currentMem = ((((memoryInfo.totalMem / 1048576.0) - (memoryInfo.availMem / 1048576.0)) / (memoryInfo.totalMem / 1048576.0)) * 100.0).toFloat()
                    val currentStorage = ((((internalStatFs.blockCountLong * internalStatFs.blockSizeLong) / 1048576.0)
                            / (((internalStatFs.availableBlocksLong * internalStatFs.blockSizeLong) +
                            (internalStatFs.blockCountLong * internalStatFs.blockSizeLong)) / 1048576.0)) * 100.0).toFloat()
                    if ("%.2f".format(currentMem).replace(",", ".") != ramArc.progress.toString()) ramArc.progress = currentMem
                    if ("%.2f".format(currentStorage).replace(",", ".") != storageArc.progress.toString()) storageArc.progress = currentStorage
                }
            }
        }, 0, 1000)
        super.onResume()
    }

    override fun onPause() {
        try { timer?.cancel() } catch (e: IllegalStateException) { e.printStackTrace() }
        super.onPause()
    }*/

    override fun onShareButtonClick() {
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "text/plain"
        val shareBody = "${getString(R.string.softwareInfo)} :\n" +
            "${getString(R.string.androidVersion)} ${androidVersion.text}\n" +
            "${getString(R.string.sdkVersion)} ${sdkVersion.text}\n" +
            "${getString(R.string.secPatch)} ${secPatch.text}\n" +
            "${getString(R.string.interfaceVersion)} ${interfaceVersion.text}\n" +
            "${getString(R.string.androidID)} ${androidID.text}\n" +
            "${getString(R.string.brand)} ${brand.text}\n" +
            "${getString(R.string.suppABIs)} ${suppABIs.text}\n" +
            "\n${getString(R.string.hardwareInfo)} :\n" +
            "${getString(R.string.manufacturer)} ${manufacturer.text}\n" +
            "${getString(R.string.model)} ${model.text}\n" +
            "${getString(R.string.cpu)} ${cpu.text}\n" +
            "${getString(R.string.cpuFreq)} ${cpuFreq.text}\n" +
            "${getString(R.string.ram)} ${totRAM}\n" +
            "${getString(R.string.storage)} ${totStorage}\n" +
            "${getString(R.string.hasBluetooth)} ${bluetooth.text}\n" +
            "${getString(R.string.hasNFC)} ${nfc.text}\n" +
            "${getString(R.string.hasBiometric)} ${biometric.text}\n" +
            "${getString(R.string.screenSize)} ${screenSize.text}\n" +
            "${getString(R.string.screenDensity)} ${screenDensity.text}\n" +
            "${getString(R.string.screenResolution)} ${screenResolution.text}\n" +
            "\n${getString(R.string.cameraInfo)} :\n" +
            "${getString(R.string.frontResolution)} ${fcRes.text}\n" +
            "${getString(R.string.frontVideoResolution)} ${fvRes.text}\n" +
            "${getString(R.string.backResolution)} ${bcRes.text}\n" +
            "${getString(R.string.backVideoResolution)} ${bvRes.text}"
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.system_page))
        sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody)
        startActivity(Intent.createChooser(sharingIntent, getString(R.string.share_via)))
    }
}
