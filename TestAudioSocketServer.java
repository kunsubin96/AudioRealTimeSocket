package whiteboard;

import java.awt.Color;
import java.awt.MediaTracker;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.swing.plaf.SliderUI;

import com.google.gson.Gson;

public class TestAudioSocketServer {

	private static final int SAMPLE_RATE = 8000; // Hertz
	private static final int SAMPLE_SIZE = 2; // Bytes
	private static final int BUF_SIZE = 1600;

	// private static final int BUF_SIZE=240;
	public static void main(String[] args) {

		System.out.println("Listening on port 5908, CRTL-C to stop");
		try {
			@SuppressWarnings("resource")
			ServerSocket serverSocket = new ServerSocket(5908);
			Socket socket = serverSocket.accept();
			System.out.println("got a connection");
			OutputStream outputStream = socket.getOutputStream();
			BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);

			//reciver data
			Thread threadReciver = new Thread(new Runnable() {

				@Override
				public void run() {

					try {
						SourceDataLine speaker;
						AudioFormat format = new AudioFormat(SAMPLE_RATE, // Sample
																			// Rate
								16, // Size of SampleBits
								1, // Number of Channels
								true, // Is Signed?
								false // Is Big Endian?
						);

						// creating the DataLine Info for the speaker format
						DataLine.Info speakerInfo = new DataLine.Info(SourceDataLine.class, format);
						// getting the mixer for the speaker
						speaker = (SourceDataLine) AudioSystem.getLine(speakerInfo);
						speaker.open(format);
						speaker.start();

						while (socket != null && !socket.isClosed()) {

							InputStream inputStream = socket.getInputStream();

							byte[] data = new byte[BUF_SIZE];
							DataInputStream dataIS = new DataInputStream(inputStream);
							dataIS.readFully(data);

							System.out.println("DataReciver: " + Arrays.toString(data));

							speaker.write(data, 0, BUF_SIZE);

							// send(data,bufferedOutputStream);
						}
						speaker.drain();
						speaker.close();
					} catch (LineUnavailableException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}

				}
			});
			
			//send audio
			Thread threadSend=new Thread(new Runnable() {
				
				@Override
				public void run() {
					AudioFormat format = new AudioFormat(8000.0f, 16, 1, true, true);
				    TargetDataLine microphone;
				    SourceDataLine speakers;
				    try {
				        microphone = AudioSystem.getTargetDataLine(format);

				        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
				        microphone = (TargetDataLine) AudioSystem.getLine(info);
				        microphone.open(format);

				        ByteArrayOutputStream out = new ByteArrayOutputStream();
				        int numBytesRead;
				        byte[] data = new byte[BUF_SIZE];
				        microphone.start();

				        int bytesRead = 0;
				        DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, format);
				        speakers = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
				        speakers.open(format);
				        speakers.start();
				        while (socket != null && !socket.isClosed()) {
				            numBytesRead = microphone.read(data, 0, BUF_SIZE);
				            bytesRead += numBytesRead;
				            System.out.println("DataSend: " + Arrays.toString(data));
				            
				            if (bufferedOutputStream != null) {
								bufferedOutputStream.write(data, 0, data.length);
								bufferedOutputStream.flush();
							} else {
								System.out.println("Null BufferedOutputStream");
							}
				            
				        }
				        speakers.drain();
				        speakers.close();
				        microphone.close();
				    } catch (LineUnavailableException e) {
				        e.printStackTrace();
				    } catch (Exception e) {
						e.printStackTrace();
					}
					
				}
			});
			
			
			
			threadReciver.start();
			//threadSend.start();

		} catch (EOFException e) {

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static void send(byte[] data, BufferedOutputStream bufferedOutputStream) throws IOException {
		InputStream iput = new ByteArrayInputStream(data);
		int len = 0;
		byte[] b = new byte[1024];
		int dataLength = data.length;
		int bytesRead;
		while (len < dataLength) {
			bytesRead = iput.read(b, 0, Math.min(b.length, dataLength - len));
			len = len + bytesRead;
			if (bufferedOutputStream != null) {
				bufferedOutputStream.write(b, 0, bytesRead);
				bufferedOutputStream.flush();
			} else {
				System.out.println("Null BufferedOutputStream");
			}

		}
		System.out.println("Write " + data.length + "success!");
	}

	public static int byteArrayToInt(byte[] b) {
		return b[3] & 0xFF | (b[2] & 0xFF) << 8 | (b[1] & 0xFF) << 16 | (b[0] & 0xFF) << 24;
	}

}
