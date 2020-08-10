package com.application.stepscounter.stepjava

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import com.application.stepscounter.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataSet
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.gms.fitness.result.DataReadResponse
import kotlinx.android.synthetic.main.main_activity.*
import org.jetbrains.anko.doAsync
import java.text.DateFormat
import java.text.DateFormat.getTimeInstance
import java.util.*
import java.util.concurrent.TimeUnit


public class WeekStepCounter :AppCompatActivity(),
    GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{
    private val GOOGLE_FIT_PERMISSIONS_REQUEST_CODE: Int = 1001
    private var mGoogleApiClient: GoogleApiClient? = null

    var  fitnessOptions = FitnessOptions.builder()
        .addDataType(
            DataType.TYPE_STEP_COUNT_DELTA,
            FitnessOptions.ACCESS_READ
        )
        .addDataType(
            DataType.AGGREGATE_STEP_COUNT_DELTA,
            FitnessOptions.ACCESS_READ
        )
        .build()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.main_activity)


        val account = GoogleSignIn.getAccountForExtension(this, fitnessOptions)

        if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
            GoogleSignIn.requestPermissions(
                this, // your activity
                GOOGLE_FIT_PERMISSIONS_REQUEST_CODE, // e.g. 1
                account,
                fitnessOptions)
        } else {
            accessGoogleFit();
        }


        btn_Start.setOnClickListener{
            doAsync {
                accessGoogleFit()
                //displayLastWeeksData()
            }
            //  ViewWeekStepCountTask(mApiClient).execute()
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == GOOGLE_FIT_PERMISSIONS_REQUEST_CODE) {
                accessGoogleFit()
            }
        }
    }

    private fun accessGoogleFit() {
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
        val account = GoogleSignIn
            .getAccountForExtension(this, fitnessOptions)
       /* Fitness.getHistoryClient(this, account).readData(readRequest)

            .addOnSuccessListener { response: DataReadResponse? ->
                // Use response data here
                Log.d("TAG", "OnSuccess()" + response.toString())
            }
            .addOnFailureListener { e: Exception? ->
                Log.d(
                    "TAG",
                    "OnFailure()",
                    e
                )
            }*/

        val response =
            Fitness.getHistoryClient(
                this,
                GoogleSignIn.getLastSignedInAccount(this)!!
            ).readData(readRequest)
        .addOnSuccessListener { response: DataReadResponse? ->
                // Use response data here
                Log.d("TAG", "OnSuccess()" + response.toString())
                val dataSets: List<DataSet> = response!!.dataSets
                for( dataset in dataSets){
                    dumpDataSet(dataset)
                }
            }.addOnFailureListener { e: Exception? ->
                    Log.d(
                        "TAG",
                        "OnFailure()",
                        e
                    )
            }


        /*val dataSets: List<DataSet> = response.getResult()!!.getDataSets()
            for( dataset in dataSets){
            dumpDataSet(dataset)
        }*/

    }

    private fun displayLastWeeksData() {
        val cal: Calendar = Calendar.getInstance()
        val now = Date()
        cal.setTime(now)
        val endTime: Long = cal.getTimeInMillis()
        cal.add(Calendar.WEEK_OF_YEAR, -1)
        val startTime: Long = cal.getTimeInMillis()

        val dateFormat = DateFormat.getDateInstance()
        Log.e("History", "Range Start: " + dateFormat.format(startTime))
        Log.e("History", "Range End: " + dateFormat.format(endTime))

//Check how many steps were walked and recorded in the last 7 days

//Check how many steps were walked and recorded in the last 7 days
        val readRequest: DataReadRequest = DataReadRequest.Builder()
            .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
            .bucketByTime(1, TimeUnit.DAYS)
            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
            .build()
        val dataReadResult =
            Fitness.HistoryApi.readData(
                mGoogleApiClient,
                readRequest
            ).await(1, TimeUnit.MINUTES)

        if (dataReadResult.buckets.size > 0) {
            Log.e("History", "Number of buckets: " + dataReadResult.buckets.size)
            for (bucket in dataReadResult.buckets) {
                val dataSets = bucket.dataSets
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
                "\tEnd: " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)) + " " + timeFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS))
            )
            for (field in dp.dataType.fields) {
                Log.e(
                    "History", "\tField: " + field.name.toString() +
                            " Value: " + dp.getValue(field)
                )
            }
        }
    }
    private fun dumpDataSet(dataSet: DataSet) {
        Log.i(
            "Tag",
            "Data returned for Data type: " + dataSet.dataType.name
        )
        val dateFormat: DateFormat = getTimeInstance()
        for (dp in dataSet.dataPoints) {
            Log.i("TAG", "Data point:")
            Log.i("TAG", "\tType: " + dp.dataType.name)
            Log.i(
                "TAG",
                "\tStart: " + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS))
            )
            Log.i(
                "TAG",
                "\tEnd: " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS))
            )
            for (field in dp.dataType.fields) {
                Log.i(
                    "TAG",
                    "\tField: " + field.name.toString() + " Value: " + dp.getValue(field)
                )
            }
        }
    }
    override fun onConnected(p0: Bundle?) {
    }

    override fun onConnectionSuspended(p0: Int) {
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
    }

}