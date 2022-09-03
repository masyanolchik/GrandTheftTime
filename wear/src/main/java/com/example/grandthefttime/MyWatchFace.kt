package com.example.grandthefttime

import android.content.ComponentName
import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.support.wearable.watchface.CanvasWatchFaceService
import android.support.wearable.watchface.WatchFaceService
import android.support.wearable.watchface.WatchFaceStyle
import android.util.Log
import android.view.SurfaceHolder
import android.widget.Toast
import androidx.health.services.client.ExerciseUpdateListener
import androidx.health.services.client.HealthServices
import androidx.health.services.client.PassiveMonitoringCallback
import androidx.health.services.client.data.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


/**
 * Updates rate in milliseconds for interactive mode. We update once a second to advance the
 * second hand.
 */
private const val INTERACTIVE_UPDATE_RATE_MS = 1000

/**
 * Handler message id for updating the time periodically in interactive mode.
 */
private const val MSG_UPDATE_TIME = 0

/**
 * Analog watch face with a ticking second hand. In ambient mode, the second hand isn"t
 * shown. On devices with low-bit ambient mode, the hands are drawn without anti-aliasing in ambient
 * mode. The watch face is drawn with less contrast in mute mode.
 *
 *
 * Important Note: Because watch face apps do not have a default Activity in
 * their project, you will need to set your Configurations to
 * "Do not launch Activity" for both the Wear and/or Application modules. If you
 * are unsure how to do this, please review the "Run Starter project" section
 * in the Google Watch Face Code Lab:
 * https://codelabs.developers.google.com/codelabs/watchface/index.html#0
 */
class MyWatchFace : CanvasWatchFaceService() {

    override fun onCreateEngine(): Engine {
        return Engine()
    }

    private class EngineHandler(reference: MyWatchFace.Engine) : Handler(Looper.myLooper()!!) {
        private val mWeakReference: WeakReference<MyWatchFace.Engine> = WeakReference(reference)

        override fun handleMessage(msg: Message) {
            val engine = mWeakReference.get()
            if (engine != null) {
                when (msg.what) {
                    MSG_UPDATE_TIME -> engine.handleUpdateTimeMessage()
                }
            }
        }
    }

    inner class Engine : CanvasWatchFaceService.Engine() {
        private var mCenterX: Float = 0F
        private var mCenterY: Float = 0F

        private lateinit var mTimePaintStroke: Paint
        private lateinit var mTimePaint: Paint
        private lateinit var mArmorBarPaintStroke: Paint
        private lateinit var mArmorBarPaint: Paint
        private lateinit var mHealthBarPaintStroke: Paint
        private lateinit var mLungsBarPaint: Paint
        private lateinit var mLungsBarPaintStroke: Paint
        private lateinit var mHealthBarPaint: Paint
        private lateinit var mMoneyPaintStroke: Paint
        private lateinit var mMoneyPaint: Paint

        private lateinit var mBackgroundPaint: Paint
        private lateinit var mFistBitmap: Bitmap
        private lateinit var mFistBitmapPaint: Paint

        private var mAmbient: Boolean = false
        private var mLowBitAmbient: Boolean = false
        private var mBurnInProtection: Boolean = false

        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)

            setWatchFaceStyle(
                WatchFaceStyle.Builder(this@MyWatchFace)
                    .setAcceptsTapEvents(true)
                    .build()
            )

            val dataTypes =
                setOf(DataType.HEART_RATE_BPM, DataType.TOTAL_CALORIES, DataType.STEPS)
            val listener = object : ExerciseUpdateListener {
                override fun onAvailabilityChanged(dataType: DataType, availability: Availability) {
                    //
                }

                override fun onExerciseUpdate(update: ExerciseUpdate) {
                    // Process the latest information about the exercise.
                    val exerciseStatus = update.state // e.g. ACTIVE, USER_PAUSED, etc.
                    val activeDuration = update.activeDuration // Duration
                    val latestMetrics = update.latestMetrics // Map<DataType, List<DataPoint>>
                    val latestAggregateMetrics = update.latestAggregateMetrics // Map<DataType, AggregateDataPoint>
                    val latestGoals = update.latestAchievedGoals // Set<AchievedExerciseGoal>
                    val latestMilestones = update.latestMilestoneMarkerSummaries // Set<MilestoneMarkerSummary>
                    Log.w("GRANDTHEFTTIME","$latestMetrics")
                }

                override fun onLapSummary(lapSummary: ExerciseLapSummary) {
                    //
                }
            }
            CoroutineScope(Dispatchers.Default).launch {
                HealthServices.getClient(applicationContext)
                    .exerciseClient
                    .apply {
                        setUpdateListener(listener).await()
                        val dataTypes = setOf(DataType.HEART_RATE_BPM)
                        val warmUpConfig = WarmUpConfig.Builder()
                            .setDataTypes(dataTypes)
                            .setExerciseType(ExerciseType.WALKING)
                            .build()
                        prepareExercise(warmUpConfig).await()

                    }

            }
            initializeBackground()
            initializeWatchFace()
        }

        private fun initializeBackground() {
            mBackgroundPaint = Paint().apply {
                color = Color.BLACK
            }
        }

        private fun initializeWatchFace() {
            val typeface = Typeface.createFromAsset(assets, "fonts/pricedown.ttf")
            mTimePaint = Paint().apply {
                color = resources.getColor(R.color.time_color, null)
                setTypeface(typeface)
                textSize = 60f
                letterSpacing = 0.03f
            }

            mArmorBarPaint = Paint().apply {
                color = resources.getColor(R.color.armor_color, null)
            }

            mLungsBarPaint = Paint().apply {
                color = resources.getColor(R.color.lungs_color, null)
            }

            mHealthBarPaint = Paint().apply {
                color = resources.getColor(R.color.health_color, null)
            }

            mMoneyPaint = Paint().apply {
                color = resources.getColor(R.color.money_color, null)
                setTypeface(typeface)
                letterSpacing = 0.045f
                textSize = 57.5f
            }

            mTimePaintStroke = Paint().apply {
                color = Color.BLACK
                strokeWidth = 8f
                letterSpacing = 0.03f
                style = Paint.Style.STROKE
                setTypeface(typeface)
                textSize = 60f
            }

            mArmorBarPaintStroke = Paint().apply {
                color = Color.BLACK
                strokeWidth = 4.9999f
                style = Paint.Style.STROKE
            }


            mLungsBarPaintStroke = Paint().apply {
                color = Color.BLACK
                strokeWidth = 4.9999f
                style = Paint.Style.STROKE
            }

            mHealthBarPaintStroke = Paint().apply {
                color = Color.BLACK
                strokeWidth = 4.9999f
                style = Paint.Style.STROKE
            }

            mMoneyPaintStroke = Paint().apply {
                color = Color.BLACK
                strokeWidth = 10f
                letterSpacing = 0.045f
                style = Paint.Style.STROKE
                setTypeface(typeface)
                textSize = 57.5f
            }

            mFistBitmap = BitmapFactory.decodeResource(resources, R.drawable.fist)
            mFistBitmapPaint = Paint().apply {
                color = Color.BLACK
            }
        }

        override fun onDestroy() {
            super.onDestroy()
        }

        override fun onPropertiesChanged(properties: Bundle) {
            super.onPropertiesChanged(properties)
            mLowBitAmbient = properties.getBoolean(
                WatchFaceService.PROPERTY_LOW_BIT_AMBIENT, false
            )
            mBurnInProtection = properties.getBoolean(
                WatchFaceService.PROPERTY_BURN_IN_PROTECTION, false
            )
        }

        override fun onTimeTick() {
            super.onTimeTick()
            invalidate()
        }

        override fun onAmbientModeChanged(inAmbientMode: Boolean) {
            super.onAmbientModeChanged(inAmbientMode)
            mAmbient = inAmbientMode
            if(mAmbient) {
                mTimePaint.color = Color.WHITE
            } else {
                mTimePaint.color = resources.getColor(R.color.time_color, null)
            }
            invalidate()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)

            /*
             * Find the coordinates of the center point on the screen, and ignore the window
             * insets, so that, on round watches with a "chin", the watch face is centered on the
             * entire screen, not just the usable portion.
             */
            mCenterX = width / 2f
            mCenterY = height / 2f

        }

        /**
         * Captures tap event (and tap type). The [WatchFaceService.TAP_TYPE_TAP] case can be
         * used for implementing specific logic to handle the gesture.
         */
        override fun onTapCommand(tapType: Int, x: Int, y: Int, eventTime: Long) {
            when (tapType) {
                WatchFaceService.TAP_TYPE_TOUCH -> {
                    // The user has started touching the screen.
                }
                WatchFaceService.TAP_TYPE_TOUCH_CANCEL -> {
                    // The user has started a different gesture or otherwise cancelled the tap.
                }
                WatchFaceService.TAP_TYPE_TAP ->
                    // The user has completed the tap gesture.
                    // TODO: Add code to handle the tap gesture.
                    Toast.makeText(applicationContext, R.string.message, Toast.LENGTH_SHORT)
                        .show()
            }
            invalidate()
        }

        override fun onDraw(canvas: Canvas, bounds: Rect) {
            drawBackground(canvas)
            drawWatchFace(canvas)
        }

        private fun drawBackground(canvas: Canvas) {
            if(!mAmbient) {
            val metrics = baseContext.resources.displayMetrics
            val w = metrics.widthPixels
            val h = metrics.heightPixels
            val mBackgroundBitmap =
                BitmapFactory.decodeResource(resources, R.drawable.sa_background)
            val src = Rect(0, 0, mBackgroundBitmap.width - 1, mBackgroundBitmap.height - 1)
            val dest = Rect(0, 0,  w - 1, h - 1)
            canvas.drawBitmap(mBackgroundBitmap, src, dest, null)
            } else {
                canvas.drawColor(Color.BLACK)
            }
        }

        private fun drawWatchFace(canvas: Canvas) {
            val FOMATTER_HH: DateTimeFormatter =
                DateTimeFormatter.ofPattern("HH")
            val FOMATTER_MM: DateTimeFormatter =
                DateTimeFormatter.ofPattern("mm")

            val currentTimeHH = FOMATTER_HH.format(LocalDateTime.now())
            val currentTimeMM = FOMATTER_MM.format(LocalDateTime.now())

            if(!mAmbient) {
                val src = Rect(0, 0, mFistBitmap.width - 1, mFistBitmap.height - 1)
                val dest = Rect(mCenterX.toInt()-105, mCenterY.toInt()-95, mCenterX.toInt()-10, mCenterY.toInt()+40)
                canvas.drawBitmap(mFistBitmap, src, dest, mFistBitmapPaint)

                canvas.drawText(
                    currentTimeHH[0].toString(),
                    mCenterX - 5,
                    mCenterY-50,
                    mTimePaintStroke
                )

                canvas.drawText(
                    currentTimeHH[1].toString(),
                    mCenterX+22,
                    mCenterY-50,
                    mTimePaintStroke
                )

                canvas.drawText(
                    ":",
                    mCenterX+54,
                    mCenterY-55,
                    mTimePaintStroke
                )

                canvas.drawText(
                    currentTimeMM[0].toString(),
                    mCenterX + 73,
                    mCenterY-50,
                    mTimePaintStroke
                )

                canvas.drawText(
                    currentTimeMM[1].toString(),
                    mCenterX + 100,
                    mCenterY-50,
                    mTimePaintStroke
                )

                canvas.drawRect(
                    mCenterX-5,
                    mCenterY - 38,
                    mCenterX + mTimePaint.textSize * Math.abs(("#####".length / 2) / 2)+68,
                    mCenterY - 24,
                    mArmorBarPaint
                )
                canvas.drawRect(
                    mCenterX-5,
                    mCenterY - 38,
                    mCenterX + mTimePaint.textSize * Math.abs(("#####".length / 2) / 2)+68,
                    mCenterY - 24,
                    mArmorBarPaintStroke
                )

                canvas.drawRect(
                    mCenterX-5,
                    mCenterY - 11,
                    mCenterX + mTimePaint.textSize +68,
                    mCenterY + 3,
                    mLungsBarPaint
                )
                canvas.drawRect(
                    mCenterX-5,
                    mCenterY - 11,
                    mCenterX + mTimePaint.textSize+68,
                    mCenterY + 3,
                    mLungsBarPaintStroke
                )

                canvas.drawRect(
                    mCenterX-5,
                    mCenterY + 16,
                    mCenterX + mTimePaint.textSize * Math.abs(("#####".length / 2) / 2)+68,
                    mCenterY + 30,
                    mHealthBarPaint
                )
                canvas.drawRect(
                    mCenterX-5,
                    mCenterY + 16,
                    mCenterX + mTimePaint.textSize * Math.abs(("#####".length / 2) / 2)+68,
                    mCenterY + 30,
                    mHealthBarPaintStroke
                )
                canvas.drawText(
                    "$00000000",
                    mCenterX-103,
                    mCenterY+78,
                    mMoneyPaintStroke
                )
                canvas.drawText(
                    "$00000000",
                    mCenterX-103,
                    mCenterY+78,
                    mMoneyPaint
                )
            }

            canvas.drawText(
                currentTimeHH[0].toString(),
                mCenterX - 5,
                mCenterY-50,
                mTimePaint.apply {
                    if(mAmbient) {
                        color = Color.WHITE
                    }
                }
            )

            canvas.drawText(
                currentTimeHH[1].toString(),
                mCenterX+22,
                mCenterY-50,
                mTimePaint
            )

            canvas.drawText(
                ":",
                mCenterX+54,
                mCenterY-55,
                mTimePaint
            )

            canvas.drawText(
                currentTimeMM[0].toString(),
                mCenterX + 73,
                mCenterY-50,
                mTimePaint
            )

            canvas.drawText(
                currentTimeMM[1].toString(),
                mCenterX + 100,
                mCenterY-50,
                mTimePaint
            )
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        fun handleUpdateTimeMessage() {
            invalidate()
        }
    }
}