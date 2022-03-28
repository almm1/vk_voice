package com.example.vk_voice

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.CountDownTimer
import android.text.format.DateUtils
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

private const val LOG_TAG = "AudioRecordTest"


class MainViewModel(private val context: Context) : ViewModel() {

    var mStartPlaying: SnapshotStateList<Boolean> = mutableStateListOf()
    var mStartRecording = mutableStateOf(false)
    val defaultPath = mutableStateOf("")

    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null

    val listRecords: SnapshotStateList<Record> = mutableStateListOf()

    val showDialog = mutableStateListOf<Boolean>()
    val timeRecord = mutableStateOf<Long>(0)

    private lateinit var timer: CountDownTimer
    private var time:Long = 0


    fun initRecordState() {
        mStartPlaying.clear()
        repeat(listRecords.size) {
            mStartPlaying.add(false)
        }
    }

    fun initTimer() {
        timer = object : CountDownTimer(3600000, 1000) {
            override fun onFinish() {
                stopPlaying()
                stopRecording()
            }
            override fun onTick(p0: Long) {
                time++
                timeRecord.value = time
            }
        }
    }

    private fun onRecord(isPlaying: Boolean) = if (isPlaying) {
        stopRecording()
    } else {
        startRecording()
    }

    private fun onPlay(start: Boolean, path: String) = if (!start) {
        if (mStartPlaying.any { it }) {
            initRecordState()
            stopPlaying()
        }
        startPlaying(path)
    } else {
        stopPlaying()
    }

    fun openDialog(index: Int) {
        showDialog[index] = true
    }

    fun onDialogConfirm(index: Int, path: String, name: String) {
        rename(path = path, newName = name)
        showDialog[index] = false
    }

    fun onDialogDismiss(index: Int) {
        showDialog[index] = false
    }

    private fun startPlaying(path: String) {
        player = MediaPlayer().apply {
            try {
                setDataSource(path)
                prepare()
                start()
            } catch (e: IOException) {
                Log.e(LOG_TAG, "prepare() failed")
            }
        }
        timer.start()
    }


    private fun stopPlaying() {
        timer.cancel()
        player?.stop()
        player = null
    }

    private fun startRecording() {
        recorder = MediaRecorder().apply {
            val fileName = getFileNAme()
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(fileName)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            try {
                prepare()
            } catch (e: IOException) {
                Log.e(LOG_TAG, "prepare() failed")
            }
            timer.start()
            start()
        }
    }

    private fun stopRecording() {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
        timer.cancel()
        time = 0
        initListRecords()
        initRecordState()
    }

    private fun getFileNAme(): String {

        val calendar = Calendar.getInstance().time.time
        val dateTime = getTimeFormating(format = "dd_MM_yyyy_HH_mm_ss", time = calendar)
        return "${defaultPath.value}/$dateTime.3gp"
    }


    fun clickPlay(path: String, i: Int) {
        onPlay(mStartPlaying[i], path)
        mStartPlaying[i] = !mStartPlaying[i]
    }

    override fun onCleared() {
        super.onCleared()
        recorder?.release()
        recorder = null
        player?.release()
        player = null
    }

    fun record() {
        onRecord(mStartRecording.value)
        mStartRecording.value = !mStartRecording.value
    }

    fun initListRecords() {
        listRecords.clear()
        val cacheFile = context.externalCacheDir?.listFiles()
        cacheFile?.forEach {
            val seconds = getDuration(it.toString())
            val time = getTimeFormating("dd.MM.yyyy в HH:mm", it.lastModified())
            val record =
                Record(
                    name = it.name,
                    time = time,
                    length = seconds,
                    path = it.path
                )
            listRecords.add(record)
            showDialog.add(false)
        }
        listRecords.sortByDescending { it.time }
    }

    private fun getTimeFormating(format: String, time: Long): String {
        val simpleDateFormat = SimpleDateFormat(format)
        return simpleDateFormat.format(time).toString()
    }

    private fun getDuration(path: String): String {
        val uri = Uri.parse(path)
        val mmr = MediaMetadataRetriever()
        mmr.setDataSource(context, uri)
        val durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val seconds = durationStr?.toLong()?.div(1000)
        val h = formatDuration(seconds!!)
        return h
    }

     fun formatDuration(seconds: Long): String =
        DateUtils.formatElapsedTime(seconds)


    fun delete(path: String) {
        File(path).delete()
        initListRecords()
    }

    private fun rename(path: String, newName: String) {
        val tmpFile = File("${defaultPath.value}/$newName")
        File(path).renameTo(tmpFile)
        initListRecords()
    }

    class ViewModelFactory(
        private val context: Context
    ) :
        ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(context = context) as T
        }
    }
}