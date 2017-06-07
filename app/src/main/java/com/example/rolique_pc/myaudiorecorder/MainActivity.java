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

import com.musicg.fingerprint.FingerprintSimilarity;
import com.musicg.wave.Wave;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = "AudioRecordTest";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final int REQUEST_PLAY_AUDIO_PERMISSION = 201;
    private static String mFileName1;
    private static String mNewFileName1;
    private static String mNewFileName2;
    private String mFileName2;

    private static final int RECORDER_BPP = 16;
    private static final int RECORDER_SAMPLERATE = 8000;

    private MediaRecorder mRecorder;

    private MediaPlayer mPlayer;

    TextView mResultsTextView;
    TextView mResults1TextView;

    // Requesting permission to RECORD_AUDIO
    private boolean permissionToRecordAccepted;
    private String[] permissions = {Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE};

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
                stopRecording();
                break;
            case 1:
                startRecording(1);
                break;
            case 0:
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

    private void startRecording(int fileNumber) {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(fileNumber == 0 ? mFileName1 : mFileName2);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }

        mRecorder.start();
    }

    private void stopRecording() {
        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        }
    }

    class RecordButton extends android.support.v7.widget.AppCompatButton {
        int mStartRecording = 0;

        OnClickListener clicker = new OnClickListener() {
            public void onClick(View v) {
                switch (mStartRecording % 3) {
                    case 2:
                        setText("Stop recording");
                        break;
                    case 1:
                        setText("Start recording 2");
                        stopRecording();
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
            setText("Start recording");
            setOnClickListener(clicker);
        }
    }

    class PlayButton extends android.support.v7.widget.AppCompatButton {
        boolean mStartPlaying = true;

        OnClickListener clicker = new OnClickListener() {
            public void onClick(View v) {
                ActivityCompat.requestPermissions(MainActivity.this, permissions, REQUEST_PLAY_AUDIO_PERMISSION);
                onPlay(mStartPlaying, mFileName1);
                if (mStartPlaying) {
                    setText("Stop playing1");
                } else {
                    setText("Start playing1");
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
                onPlay(mStartPlaying, mFileName2);
                if (mStartPlaying) {
                    setText("Stop playing music2");
                } else {
                    setText("Start playing music2");
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
                compareFile();
            }
        };

        public CompareButton(Context ctx) {
            super(ctx);
            setText("Compare");
            setOnClickListener(clicker);
        }
    }

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
        Button recordButton = new RecordButton(MainActivity.this);
        ll.addView(recordButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        0));
        Button play1 = new PlayButton(MainActivity.this);
        ll.addView(play1,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        0));
        Button play2 = new PlayMusicButton(MainActivity.this);
        ll.addView(play2,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        0));
        Button compareButton = new CompareButton(MainActivity.this);
        ll.addView(compareButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        0));
        mResultsTextView = new TextView(MainActivity.this);
        mResultsTextView.setText("Results ");
        ll.addView(mResultsTextView,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        100));
        mResults1TextView = new TextView(MainActivity.this);
        ll.addView(mResults1TextView,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        100));
        setContentView(ll);
    }

    private void compareFile() {
        copyWaveFile(mFileName1, mNewFileName1);
        File f1 = new File(mNewFileName1);

        copyWaveFile(mFileName2, mNewFileName2);
        File f2 = new File(mNewFileName2);

        Wave w1 = new Wave(f1.getAbsolutePath());
        Wave w2 = new Wave(f2.getAbsolutePath());


        try {
            FingerprintSimilarity fps = w1.getFingerprintSimilarity(w2);
            float score = fps.getScore();
            float sim = fps.getSimilarity();
            mResultsTextView.setText("Results: Score " + score + " Similarity " + sim);
            mResults1TextView.setText("Results: FramePosition " + fps.getMostSimilarFramePosition() + " TimePosition " + fps.getsetMostSimilarTimePosition());

            Log.d(LOG_TAG + " SIM ", sim + " buldum");
            Log.d(LOG_TAG + " SCORE ", score + " ");
            Log.d(" FramePosition ", fps.getMostSimilarFramePosition() + " ");
            Log.d(" TimePosition ", fps.getsetMostSimilarTimePosition() + " ");
        } catch (Exception e) {
            e.printStackTrace();
            mResultsTextView.setText("Results: Error");

        }
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

}
