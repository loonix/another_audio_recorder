package com.loonix.another_audio_recorder;

/*
~ Nilesh Deokar @nieldeokar on 09/17/18 8:11 AM
*/

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;

public class AACRecordThread extends RecordThread {
    private static final String TAG = AACRecordThread.class.getSimpleName();

    private static final int SAMPLE_RATE = 44100;
    private static final int SAMPLE_RATE_INDEX = 4;
    private static final int CHANNELS = 1;
    private static final int BIT_RATE = 32000;

    private MediaCodec mediaCodec = null;
    private AudioRecord audioRecord = null;
    private FileOutputStream fileOutputStream = null;
    private double peakPower = -120;
    private double averagePower = -120;
    private Thread recordingThread = null;
    private long dataSize = 0;


    AACRecordThread(int sampleRate, String filePath, String extension) {
        super(sampleRate, filePath, extension);

        this.sampleRate = SAMPLE_RATE;
    }

    @Override
    public void start() throws IOException {
        this.audioRecord = createAudioRecord(this.bufferSize);
        this.mediaCodec = createMediaCodec(this.bufferSize);

        fileOutputStream = new FileOutputStream(this.filePath);

        this.mediaCodec.start();

        try {
            audioRecord.startRecording();
        } catch (Exception e) {
            Log.w(TAG, e);
            mediaCodec.release();
            throw new IOException(e);
        }

        status = "recording";
        startThread();
    }

    @Override
    public void pause() {
        status = "paused";
        peakPower = -120;
        averagePower = -120;
        audioRecord.stop();
        recordingThread = null;
    }

    @Override
    public void resume() {
        status = "recording";
        audioRecord.startRecording();
        startThread();
    }

    @Override
    public HashMap<String, Object> stop() {
        status = "stopped";

        // Return Recording Object
        HashMap<String, Object> currentResult = new HashMap<>();
        currentResult.put("duration", getDuration() * 1000);
        currentResult.put("path", filePath);
        currentResult.put("audioFormat", extension);
        currentResult.put("peakPower", peakPower);
        currentResult.put("averagePower", averagePower);
        currentResult.put("isMeteringEnabled", true);
        currentResult.put("status", status);


        resetRecorder();
        recordingThread = null;

        mediaCodec.stop();
        audioRecord.stop();

        mediaCodec.release();
        audioRecord.release();

        try {
            fileOutputStream.close();
        } catch (IOException e) {
            Log.e(TAG, "Error closing file output stream", e);
        }

        return currentResult;
    }

    @Override
    public int getDuration() {
        long duration = dataSize / ((long) sampleRate * 2);
        return (int) duration;
    }

    @Override
    public double getPeakPower() {
        return peakPower;
    }

    @Override
    public double getAveragePower() {
        return averagePower;
    }




    @Override

    public void run() {
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        ByteBuffer[] codecInputBuffers = mediaCodec.getInputBuffers();
        ByteBuffer[] codecOutputBuffers = mediaCodec.getOutputBuffers();

        while ("recording".equals(status)) {
            boolean success = handleCodecInput(audioRecord, mediaCodec, codecInputBuffers, Thread.currentThread().isAlive());
            if (success) {
                try {
                    handleCodecOutput(mediaCodec, codecOutputBuffers, bufferInfo, fileOutputStream);
                } catch (IOException e) {
                    Log.e(TAG, "Error handling codec output", e);
                    break; // Exit the loop on exception to avoid continuous failure
                }
            }
        }
    }


    private void startThread() {
        recordingThread = new Thread(this, "Audio Processing Thread");
        recordingThread.start();
    }

    private void resetRecorder() {
        peakPower = -120;
        averagePower = -120;
        dataSize = 0;
    }

    private synchronized boolean handleCodecInput(AudioRecord audioRecord, MediaCodec mediaCodec, ByteBuffer[] codecInputBuffers, boolean running) {
        if ((mediaCodec == null) || !status.equals("recording") ) {
            return false; // Early exit if codec is not in the correct state
        }
        byte[] audioRecordData = new byte[bufferSize];
        int length = audioRecord.read(audioRecordData, 0, audioRecordData.length);
        if (length > 0) {
            updatePowers(audioRecordData);
            try {
                int codecInputBufferIndex = mediaCodec.dequeueInputBuffer(10000);
                if (codecInputBufferIndex >= 0) {
                    ByteBuffer codecBuffer = codecInputBuffers[codecInputBufferIndex];
                    codecBuffer.clear();
                    codecBuffer.put(audioRecordData);
                    mediaCodec.queueInputBuffer(codecInputBufferIndex, 0, length, 0, running ? 0 : MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                }
                return true;
            } catch (IllegalStateException e) {
                Log.e(TAG, "IllegalStateException handling codec input", e);
                return false;
            } catch (Exception e) {
                Log.e(TAG, "Exception handling codec input", e);
                return false;
            }
        }
        return false;
    }


    private void handleCodecOutput(MediaCodec mediaCodec, ByteBuffer[] codecOutputBuffers, MediaCodec.BufferInfo bufferInfo, OutputStream outputStream) throws IOException {
        int codecOutputBufferIndex;
        try {
            codecOutputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
            while (codecOutputBufferIndex != MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (codecOutputBufferIndex >= 0) {
                    ByteBuffer encoderOutputBuffer = codecOutputBuffers[codecOutputBufferIndex];

                    encoderOutputBuffer.position(bufferInfo.offset);
                    encoderOutputBuffer.limit(bufferInfo.offset + bufferInfo.size);

                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                        byte[] header = createAdtsHeader(bufferInfo.size - bufferInfo.offset);
                        outputStream.write(header);

                        byte[] data = new byte[encoderOutputBuffer.remaining()];
                        if (encoderOutputBuffer.remaining() >= data.length) {


                            encoderOutputBuffer.get(data);
                            outputStream.write(data);
                        } else {
                            // Buffer does not have enough data
                            Log.e(TAG, "Buffer underflow error!");
                        }

                    }

                    encoderOutputBuffer.clear();
                    mediaCodec.releaseOutputBuffer(codecOutputBufferIndex, false);
                } else if (codecOutputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    codecOutputBuffers = mediaCodec.getOutputBuffers();
                }
                codecOutputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
            }
        } catch (IllegalStateException | IOException e) {
            Log.e(TAG, "Failed handling codec output due to an exception", e);

            try {
                // Attempt to stop and release the codec
                if (mediaCodec != null) {
                    mediaCodec.stop();
                    mediaCodec.release();
                    mediaCodec = null; // recreate before using
                }
                // Close the output stream if it's not null
                if (outputStream != null) {
                    outputStream.close();
                    outputStream = null; // Reset outputStream to ensure no further usage without proper initialization
                }

                status = "error"; //  handle in thread management

            } catch (Exception cleanupException) {
                Log.e(TAG, "Error during cleanup after an initial exception", cleanupException);
            }
        }
    }


    private void updatePowers(byte[] bdata) {
        short[] data = byte2short(bdata);
        
        // Find maximum amplitude and calculate sum for RMS
        double sum = 0;
        short maxSample = 0;
        
        for (short sample : data) {
            short absValue = (short) Math.abs(sample);
            sum += absValue * absValue;
            if (absValue > maxSample) {
                maxSample = absValue;
            }
        }
        
        String[] escapeStatusList = new String[]{"paused", "stopped", "initialized", "unset"};
        
        if (data.length == 0 || maxSample == 0 || Arrays.asList(escapeStatusList).contains(status)) {
            peakPower = -120;
            averagePower = -120; // to match iOS silent case
        } else {
            // Calculate RMS (Root Mean Square)
            double rms = Math.sqrt(sum / data.length);
            
            // iOS uses a dB scale that typically ranges from -160 to 0
            // We'll adjust our scale to match
            // The 0.25 factor helps to match iOS values
            double iOSFactor = 0.25;
            
            // Convert to dB scale (20 * log10(value/max_value))
            peakPower = 20 * Math.log10(maxSample / 32768.0) * iOSFactor;
            averagePower = 20 * Math.log10(rms / 32768.0) * iOSFactor;
        }
        
        // Track data size for duration calculation
        dataSize += data.length;
        
        // Uncomment for debugging
        // Log.d(TAG, "Peak: " + peakPower + " average: " + averagePower);
    }

    private short[] byte2short(byte[] bData) {
        short[] out = new short[bData.length / 2];
        ByteBuffer.wrap(bData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(out);
        return out;
    }

    private byte[] createAdtsHeader(int length) {
        int frameLength = length + 7;
        byte[] adtsHeader = new byte[7];

        adtsHeader[0] = (byte) 0xFF; // Sync Word
        adtsHeader[1] = (byte) 0xF1; // MPEG-4, Layer (0), No CRC
        adtsHeader[2] = (byte) ((MediaCodecInfo.CodecProfileLevel.AACObjectLC - 1) << 6);
        adtsHeader[2] |= (((byte) SAMPLE_RATE_INDEX) << 2);
        adtsHeader[2] |= (((byte) CHANNELS) >> 2);
        adtsHeader[3] = (byte) (((CHANNELS & 3) << 6) | ((frameLength >> 11) & 0x03));
        adtsHeader[4] = (byte) ((frameLength >> 3) & 0xFF);
        adtsHeader[5] = (byte) (((frameLength & 0x07) << 5) | 0x1f);
        adtsHeader[6] = (byte) 0xFC;

        return adtsHeader;
    }

    private AudioRecord createAudioRecord(int bufferSize) {
        AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize * 10);

        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.d(TAG, "Unable to initialize AudioRecord");
            throw new RuntimeException("Unable to initialize AudioRecord");
        }

        if (android.media.audiofx.NoiseSuppressor.isAvailable()) {
            android.media.audiofx.NoiseSuppressor noiseSuppressor = android.media.audiofx.NoiseSuppressor
                    .create(audioRecord.getAudioSessionId());
            if (noiseSuppressor != null) {
                noiseSuppressor.setEnabled(true);
            }
        }


        if (android.media.audiofx.AutomaticGainControl.isAvailable()) {
            android.media.audiofx.AutomaticGainControl automaticGainControl = android.media.audiofx.AutomaticGainControl
                    .create(audioRecord.getAudioSessionId());
            if (automaticGainControl != null) {
                automaticGainControl.setEnabled(true);
            }
        }


        return audioRecord;
    }

    private MediaCodec createMediaCodec(int bufferSize) throws IOException {
        MediaCodec mediaCodec = MediaCodec.createEncoderByType("audio/mp4a-latm");
        MediaFormat mediaFormat = new MediaFormat();

        mediaFormat.setString(MediaFormat.KEY_MIME, "audio/mp4a-latm");
        mediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, sampleRate);
        mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, CHANNELS);
        mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, bufferSize);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
        mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);

        try {
            mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (Exception e) {
            Log.w(TAG, e);
            mediaCodec.release();
            throw new IOException(e);
        }

        return mediaCodec;
    }
}
