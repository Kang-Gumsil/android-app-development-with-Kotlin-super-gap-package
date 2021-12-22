package com.example.recorder

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.recorder.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val requiredPermissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    private val recordingFilePath: String by lazy {
        "${externalCacheDir?.absolutePath}/recording.3gp"
    }

    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var state = State.BEFORE_RECORDING
        set(value) {
            field = value
            binding.resetButton.isEnabled = (value == State.AFTER_RECORDING) || (value == State.ON_PLAYING)
            binding.recordButton.updateIconWithState(value)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestAudioPermission()
        initViewBinding()
        initVariables()
    }


    private fun requestAudioPermission() {
        requestPermissions(requiredPermissions, REQUEST_RECORD_AUDIO_PERMISSION)
    }

    private fun initViewBinding() {
        binding = ActivityMainBinding.inflate(layoutInflater).apply {
            setContentView(root)

            soundVisualizerView.onRequestCurrentAmplitude = {
                recorder?.maxAmplitude ?: 0
            }

            recordButton.updateIconWithState(state)
            recordButton.setOnClickListener {
                when (state) {
                    State.BEFORE_RECORDING -> {
                        startRecording()
                    }
                    State.ON_RECORDING -> {
                        stopRecording()
                    }
                    State.AFTER_RECORDING -> {
                        startPlaying()
                    }
                    State.ON_PLAYING -> {
                        stopPlaying()
                    }
                }
            }

            resetButton.setOnClickListener {
                stopPlaying()
                soundVisualizerView.clearVisualization()
                recordTimeTextView.clearCountTime()
                state = State.BEFORE_RECORDING
            }
        }
    }

    private fun initVariables() {
        state = State.BEFORE_RECORDING
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        val audioRecordPermissionGranted =
            requestCode == REQUEST_RECORD_AUDIO_PERMISSION && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED

        if (!audioRecordPermissionGranted) {
            finish()
        }
    }

    private fun startRecording() {
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(recordingFilePath)
            prepare()
        }
        recorder?.start()
        binding.soundVisualizerView.startVisualizing(false)
        binding.recordTimeTextView.startCountUp()
        state = State.ON_RECORDING
    }

    private fun stopRecording() {
        recorder?.run {
            stop()
            release()
        }
        recorder = null
        binding.soundVisualizerView.stopVisualizing()
        binding.recordTimeTextView.stopCountUp()
        state = State.AFTER_RECORDING
    }

    private fun startPlaying() {
        player = MediaPlayer().apply {
            setDataSource(recordingFilePath)
            prepare()
        }
        player?.setOnCompletionListener {
            stopPlaying()
            state = State.AFTER_RECORDING
        }
        player?.start()
        binding.soundVisualizerView.startVisualizing(true)
        binding.recordTimeTextView.startCountUp()
        state = State.ON_PLAYING
    }

    private fun stopPlaying() {
        player?.release()
        player = null
        binding.soundVisualizerView.stopVisualizing()
        binding.recordTimeTextView.stopCountUp()
        state = State.AFTER_RECORDING
    }

    companion object {
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 201
    }
}