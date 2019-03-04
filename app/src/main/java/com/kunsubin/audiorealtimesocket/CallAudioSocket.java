package com.kunsubin.audiorealtimesocket;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;

public class CallAudioSocket extends Thread {
    private static final String TAG = "CallAudioSocket";
    private Socket mSocket;
    private OutputStream mOutputStream;
    private BufferedOutputStream mBufferedOutputStream;
    private final InetAddress mRemoteHost;
    private final int mRemotePort;
    private SendAudioRecord mSendAudioRecord;
    private ReceiverAudioRecord mReceiverAudioRecord;
    private static final int SAMPLE_RATE = 8000; // Hertz
    private static final int SAMPLE_INTERVAL = 20; // Milliseconds
    private static final int SAMPLE_SIZE = 2; // Bytes
    private static final int BUF_SIZE = SAMPLE_INTERVAL * SAMPLE_INTERVAL * SAMPLE_SIZE * 2; //Bytes
   //private static final int BUF_SIZE=240;
    
    public CallAudioSocket(InetAddress remoteHost, int remotePort) {
        super("CallAudioSocket");
        this.mRemoteHost = remoteHost;
        this.mRemotePort = remotePort;
    }
    
    @Override
    public void run() {
        try {
            mSocket = new Socket(mRemoteHost, mRemotePort);
            mSocket.setKeepAlive(true);
            mSocket.setSoTimeout(Configs.SOCKET_TIMEOUT);
            mOutputStream = mSocket.getOutputStream();
            mBufferedOutputStream = new BufferedOutputStream(mOutputStream);
            Log.e(TAG, "Socket creation successful!");
            
            //run thread
            mSendAudioRecord=new SendAudioRecord();
            mSendAudioRecord.start();
            mReceiverAudioRecord=new ReceiverAudioRecord();
            mReceiverAudioRecord.start();
            
            
        } catch (IOException e) {
            Log.e(TAG, "Socket creation failed - " + e.toString());
            mSocket = null;
            mOutputStream = null;
            mBufferedOutputStream = null;
        }
    }
    
    public void close() {
        if (mSocket != null) {
            try {
                mSocket.close();
                
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mSocket = null;
                mOutputStream = null;
                mBufferedOutputStream = null;
                if(mSendAudioRecord!=null){
                    mSendAudioRecord.interrupt();
                }
                if(mReceiverAudioRecord!=null){
                    mReceiverAudioRecord.interrupt();
                }
                this.interrupt();
            }
        }
      
    }
    
    
    
    public class SendAudioRecord extends Thread {
        
        @Override
        public void run() {
            // Create an instance of the AudioRecord class
            Log.d(TAG, "SendAudioRecord start");
            AudioRecord audioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                      AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                      AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                                AudioFormat.ENCODING_PCM_16BIT) * 10);
            int bytes_read;
            int bytes_sent = 0;
            byte[] buf = new byte[BUF_SIZE];
            try {
                // Create a socket and start recording
                audioRecorder.startRecording();
                while (mSocket!=null&&!mSocket.isClosed()) {
                    bytes_read = audioRecorder.read(buf, 0, BUF_SIZE);
                    bytes_sent += bytes_read;
                    
                    send(buf);
                    
                    Log.i(TAG, "Total bytes sent: " + bytes_sent);
                    Thread.sleep(SAMPLE_INTERVAL, 0);
                }
                // Stop recording and release resources
                audioRecorder.stop();
                audioRecorder.release();
              
            } catch (InterruptedException e) {
                Log.e(TAG, "InterruptedException: " + e.toString());
            } catch (IOException e){
                Log.e(TAG,"BufferedOutputStream error write.");
            }
        }
        
        public void send(final byte[] data) throws IOException {
            Log.d(TAG, "Sending");
            if (mSocket == null || mOutputStream == null) {
                Log.d(TAG, "Null object");
                return;
            }
            Log.d(TAG, "Not null object");
            
            if (mBufferedOutputStream != null) {
                mBufferedOutputStream.write(data, 0, data.length);
                mBufferedOutputStream.flush();
            } else {
                Log.d(TAG, "Null BufferedOutputStream");
            }
            
        }
        
    }
    
    public class ReceiverAudioRecord extends Thread {
        
        @Override
        public void run() {
            Log.d(TAG, "ReceiverAudioRecord start");
            
            AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE,
                      AudioFormat.CHANNEL_OUT_MONO,
                      AudioFormat.ENCODING_PCM_16BIT, BUF_SIZE, AudioTrack.MODE_STREAM);
            track.play();
            
            try {
                while (mSocket!=null&&!mSocket.isClosed()) {
                    InputStream inputStream = mSocket.getInputStream();
                    byte[] data = new byte[BUF_SIZE];
                    DataInputStream dataIS = new DataInputStream(inputStream);
                    dataIS.readFully(data);
                    Log.d(TAG, "ReceiverAudioRecord: " + Arrays.toString(data));
                    track.write(data,0, BUF_SIZE);
                }
                track.stop();
                track.flush();
                track.release();
            } catch (IOException e) {
                e.printStackTrace();
            }
            
        }
        
    }
}
