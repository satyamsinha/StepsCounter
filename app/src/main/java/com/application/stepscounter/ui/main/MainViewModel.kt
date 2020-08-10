package com.application.stepscounter.ui.main

import android.content.Context
import android.util.Log
import androidx.annotation.UiThread
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.gms.fitness.result.DataReadResponse
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainViewModel : ViewModel() {
    var responseData = MutableLiveData<Task<DataReadResponse>>()

    fun getTaskData(context: Context):  MutableLiveData<Task<DataReadResponse>> {

         getWeeklyData(context)
        return responseData
    }

    private fun getWeeklyData(context: Context): MutableLiveData<Task<DataReadResponse>> {
        val cal = Calendar.getInstance()
        val now = Date()
        cal.time = now
        val endTime = cal.timeInMillis
        cal.add(Calendar.WEEK_OF_YEAR, -1)
        val startTime = cal.timeInMillis
        val dateFormat = DateFormat.getDateInstance()
        Log.i("TAG", "Range Start: " + dateFormat.format(startTime))
        Log.i("TAG", "Range End: " + dateFormat.format(endTime))
        val readRequest = DataReadRequest.Builder()
            .aggregate(
                DataType.TYPE_STEP_COUNT_DELTA,
                DataType.AGGREGATE_STEP_COUNT_DELTA
            )
            .bucketByTime(1, TimeUnit.SECONDS)
            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
            .build()

    //    GlobalScope.launch {
            responseData.value= Fitness.getHistoryClient(
                context,
                GoogleSignIn.getLastSignedInAccount(context)!!
            ).readData(readRequest)
        Thread.sleep(2000)

    return responseData
    }


}