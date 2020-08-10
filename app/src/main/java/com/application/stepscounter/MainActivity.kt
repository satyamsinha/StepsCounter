package com.application.stepscounter

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Log.i
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.application.stepscounter.ui.main.MainFragment
import kotlinx.android.synthetic.main.main_activity.*
import kotlinx.android.synthetic.main.main_fragment.*

class MainActivity : AppCompatActivity() , SensorEventListener {

      override fun onCreate(savedInstanceState: Bundle?) {
          super.onCreate(savedInstanceState)
          setContentView(R.layout.new_main_activity)
          if (savedInstanceState == null) {
              supportFragmentManager.beginTransaction()
                      .replace(R.id.container, MainFragment.newInstance())
                      .commitNow()
          }
      }


    private var start: Boolean = false
    private var initialValue: Float = 0.0f

    var running = false
    var sensorManager: SensorManager? = null

/*    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        val permission = ContextCompat.checkSelfPermission(this,
            Manifest.permission.ACTIVITY_RECOGNITION)

        if (permission != PackageManager.PERMISSION_GRANTED) {
            Log.i("Permission", "Permission to record denied")
            return
        }
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        var stepsSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        btn_Start.setOnClickListener{
            start=true
            sensorManager?.registerListener(MainActivity@this, stepsSensor, SensorManager.SENSOR_DELAY_UI)
        }
    }*/

    override fun onResume() {
        super.onResume()
        running = true
        /*var stepsSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        if (stepsSensor == null) {
            Toast.makeText(this, "No Step Counter Sensor !", Toast.LENGTH_SHORT).show()
        } else {
            sensorManager?.registerListener(MainActivity@this, stepsSensor, SensorManager.SENSOR_DELAY_UI)
        }*/
    }

    override fun onPause() {
        super.onPause()
        running = false
        sensorManager?.unregisterListener(this)
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (running) {

            if(start){
                start=false
                initialValue=event.values[0]
            }

            i("listening","running"+initialValue+"--"+event.values[0])
            var toShow=event.values[0]-initialValue
            stepsValue.setText(""+initialValue +"--"+event.values[0]+"--" +( event.values[0]-initialValue))
        }
    }
}
