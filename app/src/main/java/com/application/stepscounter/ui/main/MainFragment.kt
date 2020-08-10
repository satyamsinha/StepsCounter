package com.application.stepscounter.ui.main

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Log.i
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.application.stepscounter.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.Scope
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataSet
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.gms.fitness.result.DataReadResponse
import com.google.android.gms.tasks.Task
import java.text.DateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class MainFragment : Fragment() {

    private var mGoogleApiClient: GoogleApiClient? = null
    private var account: GoogleSignInAccount? = null
    private val GOOGLE_FIT_PERMISSIONS_REQUEST_CODE: Int=1001
    private var steps: Long = 0
    companion object {
        fun newInstance() = MainFragment()
    }

    private lateinit var viewModel: MainViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

      /*  checkUser()

        if (activity?.let { ContextCompat.checkSelfPermission(it, Manifest.permission.ACTIVITY_RECOGNITION) }
            != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            activity?.let {
                ContextCompat.checkSelfPermission(
                    it,
                    Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }*/
        mGoogleApiClient = GoogleApiClient.Builder(requireContext())
            .addApi(Fitness.HISTORY_API)
            .addScope(Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
            //.addConnectionCallbacks(requireContext())
            //.enableAutoManage(context, 0, context)
            .build()

        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
       viewModel.getTaskData(requireContext()).observe(viewLifecycleOwner, androidx.lifecycle.Observer { response:Task<DataReadResponse> ->

               val dataSets: List<DataSet> = response.getResult()!!.getDataSets()
               for (dataSet in dataSets) {
                   dumpDataSet(dataSet)
               }


       })


    }
    val fitnessOptions = FitnessOptions.builder()
        .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
        .build()
    fun checkUser(){

        account = activity?.let { GoogleSignIn.getAccountForExtension(it, fitnessOptions) }

        if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
            GoogleSignIn.requestPermissions(
                this,
                GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                account,
                fitnessOptions);
        } else {
            accessGoogleFit();
        }

       /* activity?.let {
            Fitness.getRecordingClient(
                it,
                GoogleSignIn.getLastSignedInAccount(activity)!!
            )
                .subscribe(DataType.TYPE_ACTIVITY_SAMPLES)
                .addOnSuccessListener(object : OnSuccessListener<Void?> {
                    override fun onSuccess(aVoid: Void?) {
                        i("TAG", "Successfully subscribed!")
                    }
                })
                .addOnFailureListener(object : OnFailureListener {
                    override fun onFailure(p0: java.lang.Exception) {
                        TODO("Not yet implemented")
                    }
                })
        }*/

       /* activity?.let {
            Fitness.getRecordingClient(
                it,
                GoogleSignIn.getLastSignedInAccount(activity)!!
            )
                .listSubscriptions(DataType.TYPE_ACTIVITY_SAMPLES)
                .addOnSuccessListener { subscriptions ->
                    for (sc in subscriptions) {
                        val dt: DataType? = sc.dataType
                        if (dt != null) {
                            i(
                                "TAG",
                                "Active subscription for data type: " + dt.name
                            )
                        }
                    }
                }
        }*/
    }
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == GOOGLE_FIT_PERMISSIONS_REQUEST_CODE) {
                accessGoogleFit()
            }
        }
    }

    private fun accessGoogleFit() {
        val cal: Calendar = Calendar.getInstance()
        cal.setTime(Date())
        val endTime: Long = cal.getTimeInMillis()
        cal.add(Calendar.YEAR, -1)
        val startTime: Long = cal.getTimeInMillis()
        val readRequest = DataReadRequest.Builder()
            .aggregate(
                DataType.TYPE_STEP_COUNT_DELTA,
                DataType.AGGREGATE_STEP_COUNT_DELTA
            )
            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
            .bucketByTime(1, TimeUnit.DAYS)
            .build()
        val account = activity?.let {
            fitnessOptions?.let { it1 ->
                GoogleSignIn
                    .getAccountForExtension(it, it1)
            }
        }
        activity?.let {
            account?.let { it1 ->
                Fitness.getHistoryClient(it, it1)
                    .readData(readRequest)
                    .addOnSuccessListener({ response ->
                        Log.d("TAG", "OnSuccess()")
                    })
                    .addOnFailureListener({ e -> Log.d("TAG", "OnFailure()", e) })
            }
        }
    }

    private fun dumpDataSet(dataSet: DataSet) {
        i("TAG", "Data returned for Data type: " + dataSet.dataType.name)
        val dateFormat = DateFormat.getTimeInstance()
        for (dp in dataSet.dataPoints) {
            i("Data", "Data point:")
            i("TAG", "\tType: " + dp.dataType.name)
            i(
                "TAG",
                "\tStart: " + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS))
            )
            i(
                "TAG",
                "\tEnd: " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS))
            )
            for (field in dp.dataType.fields) {
                i(
                    "TAG",
                    "\tField: " + field.name + " Value: " + dp.getValue(field)
                )
            }
        }
    }

}