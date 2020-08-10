package com.application.stepscounter.stepjava

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.application.stepscounter.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.ResultCallback
import com.google.android.gms.common.api.Scope
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.data.*
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.gms.fitness.request.DataSourcesRequest
import com.google.android.gms.fitness.request.OnDataPointListener
import com.google.android.gms.fitness.request.SensorRequest
import com.google.android.gms.fitness.result.DataReadResult
import com.google.android.gms.fitness.result.DataSourcesResult
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import kotlinx.android.synthetic.main.main_activity.*
import org.jetbrains.anko.doAsync
import java.text.DateFormat
import java.util.*
import java.util.concurrent.TimeUnit


public class StepListCounterActivity : AppCompatActivity(), OnDataPointListener,
    GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private val REQUEST_OAUTH = 1
    private val AUTH_PENDING = "auth_state_pending"
    private var authInProgress = false
    private var mApiClient: GoogleApiClient? = null

    private var mApiClients: GoogleApiClient? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        if (savedInstanceState != null) {
            authInProgress = savedInstanceState.getBoolean(AUTH_PENDING)
        }
        mApiClient = GoogleApiClient.Builder(this)
            .addApi(Fitness.SENSORS_API)
            .addScope(Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
            .addConnectionCallbacks(StepListCounterActivity@this)
            .addOnConnectionFailedListener(StepListCounterActivity@this)
            .build()

        mApiClients = GoogleApiClient.Builder(this)
            .addApi(Fitness.HISTORY_API)
            .addScope(Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
            .addConnectionCallbacks(this)
            .enableAutoManage(this, 0, this)
            .build()

        GoogleSignIn.getLastSignedInAccount(this)?.let {
            Fitness.getRecordingClient(
                this,
                it
            )
                .subscribe(DataType.TYPE_ACTIVITY_SAMPLES)
                .addOnSuccessListener(object : OnSuccessListener<Void?> {
                    override fun onSuccess(aVoid: Void?) {
                        Log.i("FragmentActivity.TAG", "Successfully subscribed!")
                    }
                })
                .addOnFailureListener(object : OnFailureListener {


                    override fun onFailure(p0: java.lang.Exception) {
                        Log.i("FragmentActivity.TAG", "There was a problem subscribing.")                    }
                })
        }
        btn_Start.setOnClickListener{
            doAsync {

                displayLastWeeksData(mApiClient)
            }
        }
    }
    override fun onStart() {
        super.onStart()
        mApiClient!!.connect()
    }

   override fun onDataPoint(dataPoint: DataPoint): Unit {
       for (field in dataPoint.dataType.fields) {
           val value: Value = dataPoint.getValue(field)
           runOnUiThread {
               Toast.makeText(
                   applicationContext,
                   "Field: " + field.name.toString() + " Value: " + value,
                   Toast.LENGTH_SHORT
               ).show()
               stepsValue.text=""+value
           }
       }
   }

    override fun onConnectionSuspended(p0: Int) {
    }

    override fun onConnected(bundle: Bundle?) {
        val dataSourceRequest = DataSourcesRequest.Builder()
            .setDataTypes(DataType.TYPE_STEP_COUNT_CUMULATIVE)
            .setDataSourceTypes(DataSource.TYPE_RAW)
            .build()
        val dataSourcesResultCallback: ResultCallback<DataSourcesResult?> =
            object : ResultCallback<DataSourcesResult?>{
                override fun onResult(dataSourcesResult: DataSourcesResult) {
                    for (dataSource in dataSourcesResult.dataSources) {
                        if (DataType.TYPE_STEP_COUNT_CUMULATIVE.equals(dataSource.dataType)) {
                            registerFitnessDataListener(
                                dataSource,
                                DataType.TYPE_STEP_COUNT_CUMULATIVE
                            )
                        }
                    }
                }
            }
        Fitness.SensorsApi.findDataSources(
            mApiClient,
            dataSourceRequest
        )
            .setResultCallback(dataSourcesResultCallback)
    }
    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        if (!authInProgress) {
            try {
                authInProgress = true
                connectionResult.startResolutionForResult(this@StepListCounterActivity, REQUEST_OAUTH)
            } catch (e: IntentSender.SendIntentException) {
            }
        } else {
            Log.e("GoogleFit", "authInProgress")
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_OAUTH) {
            authInProgress = false
            if (resultCode == Activity.RESULT_OK) {
                if (!mApiClient!!.isConnecting && !mApiClient!!.isConnected) {
                    mApiClient!!.connect()
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Log.e("GoogleFit", "RESULT_CANCELED")
            }
        } else {
            Log.e("GoogleFit", "requestCode NOT request_oauth")
        }
    }
    private fun registerFitnessDataListener(
        dataSource: DataSource,
        dataType: DataType
    ) {
        val request = SensorRequest.Builder()
            .setDataSource(dataSource)
            .setDataType(dataType)
            .setSamplingRate(3, TimeUnit.SECONDS)
            .build()
        Fitness.SensorsApi.add(mApiClient, request, this)
            .setResultCallback { status ->
                if (status.isSuccess) {
                    Log.e("GoogleFit", "SensorApi successfully added")
                }
            }
    }
    override fun onStop() {
        super.onStop()
        Fitness.SensorsApi.remove(mApiClient, this)
            .setResultCallback { status ->
                if (status.isSuccess) {
                    mApiClient!!.disconnect()
                }
            }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(AUTH_PENDING, authInProgress)
    }

   /* var mApiClients= GoogleApiClient.Builder(this)
        .addApi(Fitness.HISTORY_API)
        .addScope( Scope(Scopes.FITNESS_ACTIVITY_READ))
        .addScope(Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
        .addConnectionCallbacks(this)
        .enableAutoManage(this, 10, this)
        .build()*/

    private fun displayLastWeeksData(mApiClient: GoogleApiClient?) {
        val cal: Calendar = Calendar.getInstance()
        val now = Date()
        cal.setTime(now)
        val endTime: Long = cal.getTimeInMillis()
        cal.add(Calendar.WEEK_OF_YEAR, -1)
        val startTime: Long = cal.getTimeInMillis()

        val dateFormat = DateFormat.getDateInstance()
        Log.e("History", "Range Start: " + dateFormat.format(startTime))
        Log.e("History", "Range End: " + dateFormat.format(endTime))

        val readRequest: DataReadRequest = DataReadRequest.Builder()
            .aggregate(
                DataType.TYPE_STEP_COUNT_DELTA,
                DataType.AGGREGATE_STEP_COUNT_DELTA
            )
            .bucketByTime(1, TimeUnit.DAYS)
            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
            .build()

        val dataReadResult: DataReadResult =
            Fitness.HistoryApi.readData(
                mApiClients,
                readRequest
            ).await(1, TimeUnit.MINUTES)
        if (dataReadResult.buckets.size > 0) {
            Log.e(
                "History",
                "Number of buckets: " + dataReadResult.buckets.size
            )
            for (bucket in dataReadResult.buckets) {
                val dataSets: List<DataSet> = bucket.dataSets
                for (dataSet in dataSets) {
                    showDataSet(dataSet)
                }
            }
        } else if (dataReadResult.dataSets.size > 0) {
            Log.e(
                "History",
                "Number of returned DataSets: " + dataReadResult.dataSets.size
            )
            for (dataSet in dataReadResult.dataSets) {
                showDataSet(dataSet)
            }
        }
    }
    private fun showDataSet(dataSet: DataSet) {
        Log.e(
            "History",
            "Data returned for Data type: " + dataSet.dataType.name
        )
        val dateFormat = DateFormat.getDateInstance()
        val timeFormat = DateFormat.getTimeInstance()
        for (dp in dataSet.dataPoints) {
            Log.e("History", "Data point:")
            Log.e("History", "\tType: " + dp.dataType.name)
            Log.e(
                "History",
                "\tStart: " + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)) + " " + timeFormat.format(
                    dp.getStartTime(TimeUnit.MILLISECONDS)
                )
            )
            Log.e(
                "History",
                "\tEnd: " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)) + " " + timeFormat.format(
                    dp.getStartTime(TimeUnit.MILLISECONDS)
                )
            )
            for (field in dp.dataType.fields) {
                Log.e(
                    "History", "\tField: " + field.name.toString() +
                            " Value: " + dp.getValue(field)
                )
            }
        }
    }
}