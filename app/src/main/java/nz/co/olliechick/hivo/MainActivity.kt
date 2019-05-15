package nz.co.olliechick.hivo

import android.media.*
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.toast

import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import android.media.AudioTrack
import java.io.*
import android.media.AudioFormat.CHANNEL_OUT_STEREO
import android.media.AudioFormat.ENCODING_PCM_16BIT
import android.media.AudioAttributes




/**
 * Sample that demonstrates how to record a device's microphone using [AudioRecord].
 */
class MainActivity : AppCompatActivity() {

    /**
     * Signals whether a recording is in progress (true) or not (false).
     */
    private val recordingInProgress = AtomicBoolean(false)

    private var recorder: AudioRecord? = null

    private var recordingThread: Thread? = null

    private var startButton: Button? = null

    private var stopButton: Button? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startButton = findViewById(R.id.btnStart) as Button
        startButton!!.setOnClickListener {
            startRecording()
            startButton!!.isEnabled = false
            stopButton!!.isEnabled = true
        }

        stopButton = findViewById<View>(R.id.btnStop) as Button
        stopButton!!.setOnClickListener {
            stopRecording()
            startButton!!.isEnabled = true
            stopButton!!.isEnabled = false
        }

        btnStream.setOnClickListener {
            playRecord(Environment.getExternalStorageDirectory(), "recording.pcm")
        }
    }

    override fun onResume() {
        super.onResume()

        startButton!!.isEnabled = true
        stopButton!!.isEnabled = true
    }

    override fun onPause() {
        super.onPause()

//        stopRecording()
    }

    private fun startRecording() {
        toast("starting recording")
        recorder = AudioRecord(
            MediaRecorder.AudioSource.DEFAULT, SAMPLING_RATE_IN_HZ,
            CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE
        )

        recorder!!.startRecording()

        recordingInProgress.set(true)

        recordingThread = Thread(RecordingRunnable(), "Recording Thread")
        recordingThread!!.start()
    }

    private fun stopRecording() {
        toast("stopping recording")
        if (null == recorder) {
            return
        }

        recordingInProgress.set(false)

        recorder!!.stop()

        recorder!!.release()

        recorder = null

        recordingThread = null
    }

    private fun playRecord(filepath: File, filename: String) {

        var audioTrack: AudioTrack? = null

        val file = File(filepath, filename)

        val shortSizeInBytes = java.lang.Short.SIZE / java.lang.Byte.SIZE

        val bufferSizeInBytes = (file.length() / shortSizeInBytes).toInt()

        val audioData = ShortArray(bufferSizeInBytes)

        try {
            val inputStream = FileInputStream(file)
            val bufferedInputStream = BufferedInputStream(inputStream)
            val dataInputStream = DataInputStream(bufferedInputStream)

            var j = 0
            while (dataInputStream.available() > 0) {
                audioData[j] = dataInputStream.readShort()
                j++

            }

            dataInputStream.close()

//            val player = AudioTrack.Builder()
//                .setAudioAttributes(
//                    AudioAttributes.Builder()
//                        .setUsage(AudioAttributes.USAGE_ALARM)
//                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
//                        .build()
//                )
//                .setAudioFormat(
//                    AudioFormat.Builder()
//                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
//                        .setSampleRate(44100)
//                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
//                        .build()
//                )
//                .setBufferSizeInBytes(minBuffSize)
//                .build()


            audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                44100,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSizeInBytes,
                AudioTrack.MODE_STREAM
            )

            audioTrack.play()
            audioTrack.write(audioData, 0, bufferSizeInBytes)


        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    private fun stream() {
        var bufsize = AudioTrack.getMinBufferSize(
            44100,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        var audio = AudioTrack(
            AudioManager.STREAM_MUSIC,
            44100, //sample rate
            AudioFormat.CHANNEL_OUT_STEREO, //2 channel
            AudioFormat.ENCODING_PCM_16BIT, // 16-bit
            bufsize,
            AudioTrack.MODE_STREAM
        )
        audio.play()
    }

    private inner class RecordingRunnable : Runnable {

        override fun run() {
            val file = File(Environment.getExternalStorageDirectory(), "recording.pcm")
            val buffer = ByteBuffer.allocateDirect(BUFFER_SIZE)

            try {
                FileOutputStream(file).use { outStream ->
                    while (recordingInProgress.get()) {
                        val result = recorder!!.read(buffer, BUFFER_SIZE)
                        if (result < 0) {
                            throw RuntimeException(
                                "Reading of audio buffer failed: " + getBufferReadFailureReason(
                                    result
                                )
                            )
                        }
                        outStream.write(buffer.array(), 0, BUFFER_SIZE)
                        buffer.clear()
                    }
                }
            } catch (e: IOException) {
                throw RuntimeException("Writing of recorded audio failed", e)
            }

        }

        private fun getBufferReadFailureReason(errorCode: Int): String {
            when (errorCode) {
                AudioRecord.ERROR_INVALID_OPERATION -> return "ERROR_INVALID_OPERATION"
                AudioRecord.ERROR_BAD_VALUE -> return "ERROR_BAD_VALUE"
                AudioRecord.ERROR_DEAD_OBJECT -> return "ERROR_DEAD_OBJECT"
                AudioRecord.ERROR -> return "ERROR"
                else -> return "Unknown ($errorCode)"
            }
        }
    }

    companion object {

        private const val SAMPLING_RATE_IN_HZ = 44100

        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO

        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

        /**
         * Factor by that the minimum buffer size is multiplied. The bigger the factor is the less
         * likely it is that samples will be dropped, but more memory will be used. The minimum buffer
         * size is determined by [AudioRecord.getMinBufferSize] and depends on the
         * recording settings.
         */
        private const val BUFFER_SIZE_FACTOR = 2

        /**
         * Size of the buffer where the audio data is stored by Android
         */
        private val BUFFER_SIZE = AudioRecord.getMinBufferSize(
            SAMPLING_RATE_IN_HZ,
            CHANNEL_CONFIG, AUDIO_FORMAT
        ) * BUFFER_SIZE_FACTOR
    }
}