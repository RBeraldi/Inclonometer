package com.beraldi.inclonometer

import android.content.Context
import android.graphics.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener2
import android.hardware.SensorManager
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.graphics.withRotation


class MyView : View, SensorEventListener2 {

    private val redLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        strokeWidth = 10f
        style=Paint.Style.STROKE
    }


    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLUE
        strokeWidth = 100f
        textSize=200f
    }

    private var rollPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        strokeWidth = 1f
        style=Paint.Style.FILL_AND_STROKE

    }

    private var mPath = Path()
    private var roll = 0.0
    private var pitch = 0.0

    var accOK = false
    var magOK = false
    private var mLastAccelerometer = FloatArray(3)
    private var mLastMagnetometer = FloatArray(3)
    private var mOrientation = FloatArray(3)
    private var mRotMatrix = FloatArray(9)

    private var jeep : Bitmap
    private var jeepSideView : Bitmap

    constructor(context: Context) : super(context, null)
    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)
    constructor(context: Context, attributeSet: AttributeSet?, defStyle: Int) : super(
        context,
        attributeSet,
        defStyle
    )

    init {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
            SensorManager.SENSOR_DELAY_NORMAL
        )
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_NORMAL
        )

        jeep = BitmapFactory.
        decodeStream(context.assets.open("jeepBackView.png"))

        jeepSideView = BitmapFactory.
        decodeStream(context.assets.open("jeepSideView.png"))

    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)



        val height = canvas.height.toFloat()
        val width = canvas.width.toFloat()
        val centerX = width/2
        val centerY = 0.7f*height

        val rolldegree = (360*(roll/Math.PI/2)*100).toInt()/100
        val pitchdegree = (360*(pitch/Math.PI/2)*100).toInt()/100

        var rr = floatArrayOf( //roll region
            -width, 2 * height,//p0
            -width, centerY,//p1
            2 * width, centerY,//p3
            2 * width, 2 * height //p4
        )

        Matrix().apply {
            setRotate(rolldegree.toFloat(), centerX, centerY)
            mapPoints(rr)
        }

        mPath.apply {
            reset()
            moveTo(rr[0], rr[1])
            lineTo(rr[2], rr[3])
            lineTo(rr[4], rr[5])
            lineTo(rr[6], rr[7])
            close()
        }

        //Back view of the jeep

        val scala = 0.6f
        //Rotate and scale the jeep front view
        val M = Matrix().apply {
            val ar = jeep.height.toFloat()/jeep.width.toFloat() //aspect ratio
            val scale = scala*width/jeep.width
            val dx = centerX-jeep.width*scale*ar/2 //jeep icon translation
            val dy = centerY-jeep.height*scale*ar //jeep icon translation
            postScale(scale * ar, scale * ar)
            postTranslate(dx, dy)
            postRotate(rolldegree.toFloat(), centerX, centerY)
        }

        //Rotate and scale the jeep side view
        val MM = Matrix().apply {
            val ar = jeepSideView.height.toFloat()/jeepSideView.width.toFloat() //aspect ratio
            val scale = scala*width/jeepSideView.width
            val dx = centerX-jeepSideView.width*scale*ar/2 //jeep icon translation
            postScale(scale * ar, scale * ar)
            val centerY = jeepSideView.height*ar
            postTranslate(dx, 0f)
            postRotate(pitchdegree.toFloat(), centerX, centerY)

        }


        canvas.apply {
            drawRGB(255, 255, 255)
            drawPath(mPath,rollPaint)
            drawBitmap(jeep,M,null)
            drawBitmap(jeepSideView,MM,null)


        }
        canvas.withRotation (rolldegree.toFloat(),centerX,centerY){
            val mX=centerX
            val mY=centerY
            val radius = 600f
            val oval = RectF(mX - radius, mY - radius, mX + radius, mY + radius)
            drawArc(oval, -170f, 160f, false, redLine)
            drawLine(centerX,centerY-radius+30f,centerX,centerY-radius-30f,redLine)
            withRotation (-rolldegree.toFloat(),centerX,centerY){
                drawLine(centerX,centerY-radius-40f,centerX,centerY-radius-70f,redLine)
                drawText(""+rolldegree,centerX,centerY-radius-70f,textPaint)
            }
        }
    }


    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            accOK = true
            mLastAccelerometer = event.values.clone()
        } else if (event?.sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
            magOK = true
            mLastMagnetometer = event.values.clone()
        }
        if (accOK && magOK) {
            SensorManager.getRotationMatrix(
                mRotMatrix,
                null,
                mLastAccelerometer,
                mLastMagnetometer
            )
            accOK = false
            magOK = false
            SensorManager.getOrientation(mRotMatrix, mOrientation)
            pitch = mOrientation[1].toDouble()
            roll = mOrientation[2].toDouble()
            invalidate()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.i("TODO", "Not yet implemented")
    }

    override fun onFlushCompleted(sensor: Sensor?) {
        Log.i("TODO", "Not yet implemented")
    }

}