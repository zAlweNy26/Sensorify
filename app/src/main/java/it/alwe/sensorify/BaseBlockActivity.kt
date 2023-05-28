package it.alwe.sensorify

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.ColorDrawable
import android.hardware.Sensor
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import net.cachapa.expandablelayout.ExpandableLayout
import java.io.IOException
import kotlin.math.abs

abstract class BaseBlockActivity : CommonActivity() {
    private var liveChart: LineChart? = null
    private var threadChart: Thread? = null
    private var screenshotIcon: AnimatedVectorDrawable? = null
    private var questionMark: AnimatedVectorDrawable? = null
    private var shareIcon: AnimatedVectorDrawable? = null
    private var bcRegistered = false
    private var lsRegistered = false
    var decimalPrecision: String? = "%.2f"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(contentView)

        setSupportActionBar(findViewById(R.id.toolBar))
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_home)

        screenshotIcon = ContextCompat.getDrawable(baseContext, R.drawable.screenshot) as AnimatedVectorDrawable
        questionMark = ContextCompat.getDrawable(baseContext, R.drawable.question_mark) as AnimatedVectorDrawable
        shareIcon = ContextCompat.getDrawable(baseContext, R.drawable.share) as AnimatedVectorDrawable

        if (findViewById<ImageButton>(R.id.captureButton) != null) {
            findViewById<ImageButton>(R.id.captureButton).setOnClickListener {
                if ((ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                    || Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    findViewById<ImageButton>(R.id.captureButton).setImageDrawable(screenshotIcon)
                    screenshotIcon?.start()

                    val rootView = window.decorView.findViewById<View>(R.id.blockScrollView)
                    val screenView: View = rootView.rootView
                    @Suppress("DEPRECATION")
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) screenView.isDrawingCacheEnabled = true
                    val bitmap = Bitmap.createBitmap(findViewById<ScrollView>(R.id.blockScrollView).getChildAt(0).width,
                        findViewById<ScrollView>(R.id.blockScrollView).getChildAt(0).height, Bitmap.Config.ARGB_8888)
                    val colorId = (findViewById<ScrollView>(R.id.blockScrollView).background as ColorDrawable).color
                    val bitmapCanvas = Canvas(bitmap)
                    bitmapCanvas.drawColor(Color.parseColor("#" + Integer.toHexString(colorId)))
                    findViewById<ScrollView>(R.id.blockScrollView).draw(bitmapCanvas)
                    @Suppress("DEPRECATION")
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) screenView.isDrawingCacheEnabled = false

                    val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                        else MediaStore.Images.Media.EXTERNAL_CONTENT_URI

                    val fileName = "${findViewById<TextView>(R.id.toolbarTitle).text}.jpg"

                    val contentValues  = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        put(MediaStore.Images.Media.WIDTH, bitmap.width)
                        put(MediaStore.Images.Media.HEIGHT, bitmap.height)
                    }

                    var imageUri: Uri? = null

                    try {
                        imageUri = contentResolver.insert(imageCollection, contentValues)?.also { uri ->
                            contentResolver.openOutputStream(uri).use { outputStream ->
                                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream))
                                    throw IOException("Couldn't save the bitmap !")
                            }
                        } ?: throw IOException("Couldn't create MediaStore entry !")
                    } catch(e: IOException) { e.printStackTrace() }

                    val shareIntent = Intent(Intent.ACTION_SEND)
                    shareIntent.setDataAndType(imageUri, "image/*")
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    shareIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "")
                    shareIntent.putExtra(Intent.EXTRA_TEXT, findViewById<TextView>(R.id.toolbarTitle).text)
                    shareIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
                    shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri)
                    startActivity(Intent.createChooser(shareIntent, getString(R.string.share_via)))
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
                    else ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
                }
            }
        }

        if (findViewById<ImageButton>(R.id.showInfoButton) != null) {
            findViewById<ImageButton>(R.id.showInfoButton).setOnClickListener {
                findViewById<ImageButton>(R.id.showInfoButton).setImageDrawable(questionMark)
                questionMark?.start()
                val infoLayout = findViewById<ExpandableLayout>(R.id.sensorInfoLayout)
                infoLayout.toggle()
            }
        }

        if (findViewById<ImageButton>(R.id.shareButton) != null) {
            findViewById<ImageButton>(R.id.shareButton).setOnClickListener {
                findViewById<ImageButton>(R.id.shareButton).setImageDrawable(shareIcon)
                shareIcon?.start()
                onShareButtonClick()
            }
        }

        addCode()
    }

    override fun onResume() {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(baseContext)
        decimalPrecision = "%.${sharedPrefs.getInt("precisionValue", 2)}f"
        super.onResume()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val mainIntent = Intent(this, MainActivity::class.java)
        mainIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(mainIntent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.home, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val settingIntent = Intent(this, SettingsActivity::class.java)
        settingIntent.putExtra("previousActivity", this::class.java.simpleName)
        when (item.itemId) {
            R.id.action_settings -> {
                startActivity(settingIntent)
                overridePendingTransition(R.anim.from_right, R.anim.to_left)
                finish()
            }
            else -> Log.d("Error", "Ah shit, here we go again...")
        }
        return super.onOptionsItemSelected(item)
    }

    fun setBroadCastRegistered(value: Boolean, blockReceiver: BroadcastReceiver, intentFilter: IntentFilter) {
        if (!bcRegistered && value) this.registerReceiver(blockReceiver, intentFilter)
        else if (bcRegistered && !value) this.unregisterReceiver(blockReceiver)
        bcRegistered = value
    }

    fun setListenerRegistered(value: Boolean, sensorManager: SensorManager, sensorEventListener: SensorEventListener, sensor: Sensor) {
        if (!lsRegistered && value) sensorManager.registerListener(sensorEventListener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        else if (lsRegistered && !value) sensorManager.unregisterListener(sensorEventListener)
        lsRegistered = value
    }

    fun convertSuperScript(str: String) : String {
        if (str.length >= 5) {
            val superScript = when (str.substring(str.length - 3, str.length)) {
                "E1" -> " × 10¹"
                "E2" -> " × 10²"
                "E3" -> " × 10³"
                "E4" -> " × 10⁴"
                "E5" -> " × 10⁵"
                "E6" -> " × 10⁶"
                "E7" -> " × 10⁷"
                "E8" -> " × 10⁸"
                "E9" -> " × 10⁹"
                "E-1" -> " × 10ˉ¹"
                "E-2" -> " × 10ˉ²"
                "E-3" -> " × 10ˉ³"
                "E-4" -> " × 10ˉ⁴"
                "E-5" -> " × 10ˉ⁵"
                "E-6" -> " × 10ˉ⁶"
                "E-7" -> " × 10ˉ⁷"
                "E-8" -> " × 10ˉ⁸"
                "E-9" -> " × 10ˉ⁹"
                else -> str.substring(str.length - 3, str.length)
            }
            return (decimalPrecision?.format(str.substring(0, str.length - 3).toFloat()) + superScript)
        } else return decimalPrecision?.format(str.toFloat())!!
    }

    private fun createSet(name: String, color: String, mode: LineDataSet.Mode) : ILineDataSet {
        val dataSet = LineDataSet(null, name)
        dataSet.axisDependency = YAxis.AxisDependency.LEFT
        dataSet.color = Color.parseColor(color)
        dataSet.lineWidth = 2f
        dataSet.isHighlightEnabled = false
        dataSet.setDrawValues(false)
        dataSet.setDrawCircles(false)
        dataSet.mode = mode
        dataSet.cubicIntensity = 0.2f
        return dataSet
    }

    fun toggleThread(value: Boolean) {
        if (threadChart == null && value) threadChart?.start()
        else if (threadChart != null && !value) threadChart?.interrupt()
    }

    fun startLiveChart() {
        if (threadChart != null) threadChart?.interrupt()

        threadChart = Thread {
            run {
                while (true) {
                    try {
                        Thread.sleep(1)
                    } catch (e: InterruptedException) {
                        Log.e("Error", e.toString())
                    }
                }
            }
        }

        threadChart?.start()
    }
    
    fun createChart(maxRange: Float, multiplier: Float, labelsCount: Int, legend: Boolean, negative: Boolean) {

        val dm = DisplayMetrics()

        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) this.display?.getRealMetrics(dm)
        else this.windowManager.defaultDisplay.getMetrics(dm)

        val liveChartMargin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30f, resources.displayMetrics).toInt()

        if (dm.widthPixels >= dm.heightPixels) {
            if (findViewById<ConstraintLayout>(R.id.blockLayout).getChildAt(2) != null) {
                findViewById<LinearLayout>(R.id.chartLayout).layoutParams.height = dm.heightPixels - liveChartMargin -
                        (findViewById<ConstraintLayout>(R.id.blockLayout).getChildAt(2).height * 2)
            } else findViewById<LinearLayout>(R.id.chartLayout).layoutParams.height = dm.heightPixels - liveChartMargin
        } else findViewById<LinearLayout>(R.id.chartLayout).layoutParams.height = dm.widthPixels - liveChartMargin

        liveChart = findViewById(R.id.liveChart)
        liveChart?.description?.isEnabled = false
        liveChart?.isDragEnabled = true
        liveChart?.setTouchEnabled(false)
        liveChart?.setScaleEnabled(false)
        liveChart?.setDrawGridBackground(false)
        liveChart?.isDoubleTapToZoomEnabled = false
        liveChart?.setPinchZoom(false)

        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(baseContext)
        var mainColor = Color.parseColor("#202020")
        if (sharedPrefs.getInt("themeMode", R.style.LightTheme) == R.style.LightTheme) mainColor = Color.parseColor("#202020")
        else if (sharedPrefs.getInt("themeMode", R.style.LightTheme) == R.style.DarkTheme) mainColor = Color.parseColor("#FFFFFF")

        val data = LineData()
        data.setValueTextColor(mainColor)
        liveChart?.data = data

        val l: Legend = liveChart?.legend!!
        l.form = Legend.LegendForm.LINE
        l.yEntrySpace = 10f
        l.textSize = 16f
        l.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        l.textColor = mainColor
        l.isEnabled = legend

        val axisX: XAxis = liveChart?.xAxis!!
        axisX.setAvoidFirstLastClipping(true)
        axisX.textColor = mainColor
        axisX.axisMaximum = maxRange * multiplier
        axisX.axisMinimum = 0f
        //axisX.granularity = 1f * multiplier
        axisX.setDrawGridLines(true)

        val axisY: YAxis = liveChart?.axisLeft!!
        axisY.textSize = 14f
        axisY.textColor = mainColor
        axisY.axisMaximum = maxRange
        axisY.axisMinimum = if (negative) -maxRange else 0f
        axisY.setLabelCount(if (labelsCount == 0) maxRange.toInt() else labelsCount, true)
        //axisY.granularity = 1f * multiplier
        axisY.setDrawGridLines(true)

        val rightAxis: YAxis = liveChart?.axisRight!!
        rightAxis.isEnabled = false
        axisX.isEnabled = false

        liveChart?.axisLeft!!.setDrawGridLines(true)
        liveChart?.xAxis!!.setDrawGridLines(true)
        liveChart?.setDrawBorders(false)
    }

    fun addEntry(mode: Int, vals: Int, values: FloatArray, names: Array<String>, colors: Array<String>) {
        val data = liveChart?.data
        var drawMode = LineDataSet.Mode.CUBIC_BEZIER

        if (mode == 1) drawMode = LineDataSet.Mode.CUBIC_BEZIER
        else if (mode == 2) drawMode = LineDataSet.Mode.STEPPED

        if (vals >= 1) {
            if (values[0] > liveChart?.axisLeft?.axisMaximum!!)
                liveChart?.axisLeft?.axisMaximum = values[0].toInt().toFloat() + 5f
            if (values[0] < liveChart?.axisLeft?.axisMinimum!!)
                liveChart?.axisLeft?.axisMinimum = if (mode == 1) -abs(values[0].toInt().toFloat() - 5f) else 0f
        }

        if (vals >= 2) {
            if (values[1] > liveChart?.axisLeft?.axisMaximum!!)
                liveChart?.axisLeft?.axisMaximum = values[1].toInt().toFloat() + 5f
            if (values[1] < liveChart?.axisLeft?.axisMinimum!!)
                liveChart?.axisLeft?.axisMinimum = if (mode == 1) -abs(values[1].toInt().toFloat() - 5f) else 0f
        }

        if (vals >= 3) {
            if (values[2] > liveChart?.axisLeft?.axisMaximum!!)
                liveChart?.axisLeft?.axisMaximum = values[2].toInt().toFloat() + 5f
            if (values[2] < liveChart?.axisLeft?.axisMinimum!!)
                liveChart?.axisLeft?.axisMinimum = if (mode == 1) -abs(values[2].toInt().toFloat() - 5f) else 0f
        }

        if (data != null) {
            var xDataSet = data.getDataSetByIndex(0)
            var yDataSet = data.getDataSetByIndex(1)
            var zDataSet = data.getDataSetByIndex(2)

            if (vals >= 1 && xDataSet == null) {
                xDataSet = createSet(names[0], colors[0], drawMode)
                data.addDataSet(xDataSet)
            }
            if (vals >= 2 && yDataSet == null) {
                yDataSet = createSet(names[1], colors[1], drawMode)
                data.addDataSet(yDataSet)
            }
            if (vals >= 3 && zDataSet == null) {
                zDataSet = createSet(names[2], colors[2], drawMode)
                data.addDataSet(zDataSet)
            }

            liveChart?.setVisibleXRangeMaximum(150f)

            if (vals >= 1) {
                if (xDataSet.entryCount > liveChart?.visibleXRange!!) {
                    xDataSet.removeFirst()
                    for (i in 0 until xDataSet.entryCount) {
                        val entryToChange = xDataSet.getEntryForIndex(i)
                        entryToChange.x = entryToChange.x - 1
                    }
                } else data.addEntry(Entry(xDataSet.entryCount.toFloat(), values[0]), 0)
            }

            if (vals >= 2) {
                if (yDataSet.entryCount > liveChart?.visibleXRange!!) {
                    yDataSet.removeFirst()
                    for (i in 0 until yDataSet.entryCount) {
                        val entryToChange = yDataSet.getEntryForIndex(i)
                        entryToChange.x = entryToChange.x - 1
                    }
                } else data.addEntry(Entry(yDataSet.entryCount.toFloat(), values[1]), 1)
            }

            if (vals >= 3) {
                if (zDataSet.entryCount > liveChart?.visibleXRange!!) {
                    zDataSet.removeFirst()
                    for (i in 0 until zDataSet.entryCount) {
                        val entryToChange = zDataSet.getEntryForIndex(i)
                        entryToChange.x = entryToChange.x - 1
                    }
                } else data.addEntry(Entry(zDataSet.entryCount.toFloat(), values[2]), 2)
            }

            data.notifyDataChanged()
            liveChart?.notifyDataSetChanged()
            liveChart?.moveViewToX(data.entryCount.toFloat())
            liveChart?.invalidate()
        }
    }

    protected abstract fun onShareButtonClick()

    protected abstract fun addCode()

    protected abstract val contentView: Int
}