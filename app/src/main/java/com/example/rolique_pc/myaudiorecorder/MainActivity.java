package com.example.rolique_pc.myaudiorecorder;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.MissingFormatArgumentException;

public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = "AudioRecordTest";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final int REQUEST_PLAY_AUDIO_PERMISSION = 201;
    private static String mFileName1;
    private static String mNewFileName1;
    private static String mNewFileName2;
    private String mFileName2;
    boolean mIsRecording;

    private AudioRecord mRecorder;
    private int bufferSize = 0;
    private Thread mRecordingThread;
    private Thread mComparingThread;
    List<Double> mFirstSoundFrequency = new ArrayList<>();
    List<Double> mSecondSoundFrequency = new ArrayList<>();
    int mStartRecording = 0;

    private static final int RECORDER_BPP = 16;
    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_STEREO;

    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private MediaPlayer mPlayer;

    TextView mResultsTextView;
    TextView mResults1TextView;

    // Requesting permission to RECORD_AUDIO
    private boolean permissionToRecordAccepted;
    private String[] permissions = {Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    //    private void startRecording(int fileNumber) {
//        mRecorder = new MediaRecorder();
//        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
//        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
//        mRecorder.setOutputFile(fileNumber == 0 ? mFileName1 : mFileName2);
//        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
//
//        try {
//            mRecorder.prepare();
//        } catch (IOException e) {
//            Log.e(LOG_TAG, "prepare() failed");
//        }
//
//        mRecorder.start();
//    private void stopRecording() {
//        if (mRecorder != null) {
//            mRecorder.stop();
//            mRecorder.release();
//            mRecorder = null;
//        }
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        // Record to the external cache directory for visibility
        mFileName1 = getExternalCacheDir().getAbsolutePath() + "/audiorecordtest1.3gp";
        mNewFileName1 = getExternalCacheDir().getAbsolutePath() + "/audiorecordWithHeader.wav";
        mNewFileName2 = getExternalCacheDir().getAbsolutePath() + "/musicWithHeader.wav";
        mFileName2 = getExternalCacheDir().getAbsolutePath() + "/audiorecordtest2.3gp";

        ActivityCompat.requestPermissions(MainActivity.this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        LinearLayout ll = new LinearLayout(MainActivity.this);
        ll.setOrientation(LinearLayout.VERTICAL);

        LinearLayout llh = new LinearLayout(MainActivity.this);
        llh.setOrientation(LinearLayout.HORIZONTAL);
        Button recordButton = new RecordButton(MainActivity.this);
        llh.addView(recordButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        10));
        Button record2Button = new RecordSecondButton(MainActivity.this);
        llh.addView(record2Button,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        10));
        ll.addView(llh);

        LinearLayout llhPlay = new LinearLayout(MainActivity.this);
        llhPlay.setOrientation(LinearLayout.HORIZONTAL);
        Button play1 = new PlayButton(MainActivity.this);
        llhPlay.addView(play1,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        10));
        Button play2 = new PlayMusicButton(MainActivity.this);
        llhPlay.addView(play2,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        10));
        ll.addView(llhPlay);

        Button compareButton = new CompareButton(MainActivity.this);
        ll.addView(compareButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        10));
        mResultsTextView = new TextView(MainActivity.this);
        mResultsTextView.setText("Results ");
        ll.addView(mResultsTextView,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        50));
        mResults1TextView = new TextView(MainActivity.this);
        ll.addView(mResults1TextView,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        50));
        setContentView(ll);

        bufferSize = AudioRecord.getMinBufferSize
                (RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
            case REQUEST_PLAY_AUDIO_PERMISSION:
                permissionToRecordAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted) finish();

    }

    private void onRecord(int startRecording) {
        switch (startRecording % 3) {
            case 2:
                stopRecording(2);
                break;
            case 1:
                mSecondSoundFrequency.clear();
                startRecording(1);
                break;
            case 0:
                mFirstSoundFrequency.clear();
                startRecording(0);
                break;
        }
    }

    private void onPlay(boolean start, String path) {
        if (start) {
            startPlaying(path);
        } else {
            stopPlaying();
        }
    }

    private void startPlaying(String path) {
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(path);
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    private void stopPlaying() {
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
    }

//    }


//    }

    private void startRecording(final int fileNumber) {
        mRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, bufferSize);

        mRecorder.startRecording();

        mIsRecording = true;

        mRecordingThread = new Thread(new Runnable() {

            public void run() {
                writeAudioDataToFile(fileNumber);
            }
        }, "AudioRecorder Thread");

        mRecordingThread.start();
    }

    private void writeAudioDataToFile(int fileNumber) {
        byte data[] = new byte[bufferSize];
        String filename = fileNumber == 0 ? mFileName1 : mFileName2;
        FileOutputStream os = null;

        try {
            os = new FileOutputStream(filename);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Log.d(LOG_TAG, "mRecorderThread");
        int read = 0;
        scoreSum = 0.0;
        if (null != os) {
            while (mIsRecording) {
                read = mRecorder.read(data, 0, bufferSize);

                if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                    try {
                        os.write(data);

                        Double[] transformedData = calculateFFT(data);
                        if (fileNumber == 0)
                            mFirstSoundFrequency.addAll(Arrays.asList(transformedData));
                        if (fileNumber == 1) {
                            mSecondSoundFrequency.addAll(Arrays.asList(transformedData));
                            if (mComparingThread == null) {
                                mComparingThread = new Thread(new Runnable() {

                                    public void run() {
                                        Log.e(LOG_TAG, "mComparingThread");
                                        compareFile();
                                    }
                                }, "Comparing Thread");
                                mComparingThread.start();
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void stopRecording(int fileNumber) {
        if (mRecorder == null) return;
        mIsRecording = false;

        mRecorder.stop();
        mRecorder.release();

        mRecorder = null;

        if (mRecordingThread != null)
            mRecordingThread.interrupt();
        mRecordingThread = null;

        if (fileNumber == 1)
            copyWaveFile(mFileName1, mNewFileName1);
        if (fileNumber == 2)
            copyWaveFile(mFileName2, mNewFileName2);

        // deleteTempFile();
    }

    class RecordButton extends android.support.v7.widget.AppCompatButton {

        OnClickListener clicker = new OnClickListener() {
            public void onClick(View v) {
                switch (mStartRecording % 3) {
                    case 2:
                        setText("Stop recording");
                        break;
                    case 1:
                        setText("Start recording 2");
                        stopRecording(1);
                        break;
                    case 0:
                        setText("Start recording 1");
                        break;
                }
                onRecord(mStartRecording);
                mStartRecording++;
            }
        };
        public RecordButton(Context ctx) {
            super(ctx);
            setText("Start recording 1");
            setOnClickListener(clicker);
        }

    }

    class RecordSecondButton extends android.support.v7.widget.AppCompatButton {

        OnClickListener clicker = new OnClickListener() {
            public void onClick(View v) {
                switch (mStartRecording % 3) {
                    case 2:
                        setText("Stop recording");
                        break;
                    case 0:
                        mStartRecording++;
                    case 1:
                        stopRecording(1);
                        setText("Start recording 2");
                        break;
                }
                onRecord(mStartRecording);
                mStartRecording++;
            }
        };
        public RecordSecondButton(Context ctx) {
            super(ctx);
            setText("Start recording 2");
            setOnClickListener(clicker);
        }

    }
    class PlayButton extends android.support.v7.widget.AppCompatButton {

        boolean mStartPlaying = true;

        OnClickListener clicker = new OnClickListener() {
            public void onClick(View v) {
                ActivityCompat.requestPermissions(MainActivity.this, permissions, REQUEST_PLAY_AUDIO_PERMISSION);
                onPlay(mStartPlaying, mNewFileName1);
                if (mStartPlaying) {
                    setText("Stop playing 1");
                } else {
                    setText("Start playing 1");
                }
                mStartPlaying = !mStartPlaying;
            }
        };
        public PlayButton(Context ctx) {
            super(ctx);
            setText("Start playing1");
            setOnClickListener(clicker);
        }

    }
    class PlayMusicButton extends android.support.v7.widget.AppCompatButton {

        boolean mStartPlaying = true;

        OnClickListener clicker = new OnClickListener() {
            public void onClick(View v) {
                ActivityCompat.requestPermissions(MainActivity.this, permissions, REQUEST_PLAY_AUDIO_PERMISSION);
                onPlay(mStartPlaying, mNewFileName2);
                if (mStartPlaying) {
                    setText("Stop playing 2");
                } else {
                    setText("Start playing 2");
                }
                mStartPlaying = !mStartPlaying;
            }
        };
        public PlayMusicButton(Context ctx) {
            super(ctx);
            setText("Start playing music1");
            setOnClickListener(clicker);
        }

    }
    class CompareButton extends android.support.v7.widget.AppCompatButton {

        OnClickListener clicker = new OnClickListener() {
            public void onClick(View v) {
                if (mComparingThread != null) {
                    mComparingThread.interrupt();
                    if (!mComparingThread.isInterrupted())
                        mComparingThread.interrupt();
                }
                mComparingThread = null;
                scoreSum = 0.0;
                mFirstSoundFrequency.clear();
                mSecondSoundFrequency.clear();
                byte[] data = new byte[bufferSize];
                try {
                    FileInputStream in1 = new FileInputStream(mNewFileName1);
                    while (in1.read(data) != -1)
                        mFirstSoundFrequency.addAll(Arrays.asList(calculateFFT(data)));
                    FileInputStream in2 = new FileInputStream(mNewFileName2);
                    while (in2.read(data) != -1)
                        mSecondSoundFrequency.addAll(Arrays.asList(calculateFFT(data)));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                compareFile();
            }
        };
        public CompareButton(Context ctx) {
            super(ctx);
            setText("Compare");
            setOnClickListener(clicker);
        }
    }

    double scoreSum = 0.0;

    private void compareFile() {
        //TODO: calculate only new part of the list
        final int SCORE_CONSTANT = 50;
        String numbers = "";
        String score = "";

        int minLength = Math.min(mFirstSoundFrequency.size(), mSecondSoundFrequency.size());
        Log.e(LOG_TAG + " min", minLength+" ");
        numbers = calculateScore(numbers, minLength, 100);

        Log.d(LOG_TAG + " coef ", numbers);
        Log.d(LOG_TAG + " score ", score);
        final double d = scoreSum;
        if (Thread.currentThread().equals(mComparingThread)) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mResultsTextView.setText("Score" + d);
                }
            });
        } else {
            mResultsTextView.setText("Score" + d);
        }

        numbers = "";
        for (int i = 0; i < minLength; i += 50)
            numbers += mFirstSoundFrequency.get(i) + " ";
        Log.d(LOG_TAG + "First", numbers);
        numbers = "";
            for (int i = 0; i < minLength; i += 50)
                numbers += mSecondSoundFrequency.get(i) + " ";
        Log.d(LOG_TAG + "Second", numbers);

        if (mComparingThread != null) {
            mComparingThread.interrupt();
            if (!mComparingThread.isInterrupted())
                mComparingThread.interrupt();
        }
        mComparingThread = null;
    }

    private String calculateScore(String numbers, int smallestLength, int coefI) {
        for (int i = 0; i < smallestLength; i += coefI) {
            double dh = Math.abs(mFirstSoundFrequency.get(i) - mSecondSoundFrequency.get(i));
            double coef = 1 - (Math.abs(dh / mFirstSoundFrequency.get(i)) > 1 ?
                    1 : (mFirstSoundFrequency.get(i) == 0.0 ? 1 : Math.abs(dh / mFirstSoundFrequency.get(i))));
            numbers += (coef) + " ";
            Log.d(LOG_TAG + " N ", i+"");
//                double scoreD = coef * SCORE_CONSTANT * Math.pow(10, Math.log(mFirstSoundFrequency.get(i) / 15));
            scoreSum += coef;//scoreD;
//                score += scoreD + " ";
        }
        return numbers;
    }


    @Override
    public void onStop() {
        super.onStop();
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }

        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
    }

    private void copyWaveFile(String inFilename, String outFilename) {
        int bufferSize = AudioRecord.getMinBufferSize(8000,
                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = RECORDER_SAMPLERATE;
        int channels = 2;
        long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels / 8;

        byte[] data = new byte[bufferSize];

        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;

            Log.d(LOG_TAG, "File size: " + totalDataLen);

            WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                    longSampleRate, channels, byteRate);

            while (in.read(data) != -1) {
                out.write(data);
            }

            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void WriteWaveFileHeader(
            FileOutputStream out, long totalAudioLen,
            long totalDataLen, long longSampleRate, int channels,
            long byteRate) throws IOException {

        byte[] header = new byte[44];

        header[0] = 'R';  // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';  // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;  // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8);  // block align
        header[33] = 0;
        header[34] = RECORDER_BPP;  // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        out.write(header, 0, 44);

    }


    public Double[] calculateFFT(byte[] signal) {
        final int mNumberOfFFTPoints = 1024;
        double mMaxFFTSample;

        double temp;
        Complex[] y;
        Complex[] complexSignal = new Complex[mNumberOfFFTPoints];
        Double[] absSignal = new Double[mNumberOfFFTPoints / 2];

        for (int i = 0; i < mNumberOfFFTPoints; i++) {
            temp = (double) ((signal[2 * i] & 0xFF) | (signal[2 * i + 1] << 8)) / 32768.0F;
            complexSignal[i] = new Complex(temp, 0.0);
        }

        y = FFT.fft(complexSignal); // --> Here I use FFT class

//        mMaxFFTSample = 0.0;
//        mPeakPos = 0;
        for (int i = 0; i < (mNumberOfFFTPoints / 2); i++) {
            absSignal[i] = Math.sqrt(Math.pow(y[i].re(), 2) + Math.pow(y[i].im(), 2));
//            if(absSignal[i] > mMaxFFTSample)
//            {
//                mMaxFFTSample = absSignal[i];
//                mPeakPos = i;
//            }
        }

        return absSignal;

    }
}
