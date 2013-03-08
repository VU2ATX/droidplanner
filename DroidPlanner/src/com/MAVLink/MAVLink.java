package com.MAVLink;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import android.os.AsyncTask;
import android.util.Log;

import com.MAVLink.Messages.MAVLinkMessage;
import com.MAVLink.Messages.MAVLinkPacket;
import com.diydrones.droidplanner.helpers.FileManager;
import com.hoho.android.usbserial.driver.UsbSerialDriver;

public abstract class MAVLink {

	public static final int TCP = 0;
	public static final int USB = 1;

	int connectionType = TCP;

	private String serverIP;
	private int serverPort;
	private boolean logEnabled;

	boolean connected = false;
	private BufferedInputStream mavIn;
	public BufferedOutputStream logWriter;
	BufferedOutputStream mavOut;
	public int receivedCount = 0;
	private UsbSerialDriver driver;

	public abstract void onReceiveMessage(MAVLinkMessage msg);

	public abstract void onDisconnect();

	public abstract void onConnect();

	public class connectTask extends AsyncTask<String, MAVLinkMessage, String> {

		public Parser parser;
		Socket socket = null;
		byte[] buffer = new byte[4096];
		int numRead;

		@Override
		protected String doInBackground(String... message) {
			parser = new Parser();
			try {
				if (logEnabled) {
					logWriter = FileManager.getTLogFileStream();
				}

				switch (connectionType) {
				default:
				case TCP:
					getTCPStream();
					break;
				case USB:
					getUSBDriver();
					break;
				}

				MAVLinkMessage m;

				while (connected) {
					switch (connectionType) {
					default:
					case TCP:
						numRead = mavIn.read(buffer);
						break;
					case USB:
						numRead = driver.read(buffer, 1000);
						break;
					}
					if (numRead > 0) {
						for (int i = 0; i < numRead; i++) {
							if (logEnabled) {
								logWriter.write(buffer[i]);
							}
							m = parser.mavlink_parse_char(buffer[i] & 0xff);
							if (m != null) {
								receivedCount++;
								publishProgress(m);
							}
						}

					}
				}

			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					if (socket != null) {
						socket.close();
					}
					if (logEnabled) {
					if (driver != null) {
						driver.close();
					}
					if (logWriter != null) {
						logWriter.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return null;
		}

		private void getUSBDriver() throws IOException {
			if (driver != null) {
				driver.open();
				driver.setBaudRate(115200);
			}
		}

		private void getTCPStream() throws UnknownHostException, IOException {
			InetAddress serverAddr = InetAddress.getByName(serverIP);
			socket = new Socket(serverAddr, serverPort);
			mavOut = new BufferedOutputStream((socket.getOutputStream()));
			Log.d("TCP Client", "TCP connection started at: " + serverIP + ":"
					+ serverPort);
			// receive the message which the server sends back
			mavIn = new BufferedInputStream(socket.getInputStream());
		}

		@Override
		protected void onProgressUpdate(MAVLinkMessage... values) {
			super.onProgressUpdate(values);
			onReceiveMessage(values[0]);
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			Log.d("TCP IN", "Disconected");
			closeConnection();
		}
	}

	/**
	 * Format and send a Mavlink packet via the MAVlink stream
	 * 
	 * @param packet
	 *            MavLink packet to be transmitted
	 */
	public void sendMavPacket(MAVLinkPacket packet) {
		byte[] buffer = packet.encodePacket();
		sendBuffer(buffer);
	}

	/**
	 * Sends a buffer thought the MAVlink stream
	 * 
	 * @param buffer
	 *            Buffer with the data to be transmitted
	 */
	public void sendBuffer(byte[] buffer) {
		if (mavOut != null) {
			try {
				mavOut.write(buffer);
				mavOut.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/*
	 * Close the MAVlink Connection
	 */
	public void closeConnection() {
		Log.d("TCP IN", "closing TCP");
		connected = false;
		onDisconnect();
	}

	/**
	 * Start the MAVlink Connection
	 * 
	 * @param port
	 * @param serverIP
	 * @param logEnabled
	 * @param driver
	 */
	public void openConnection(String serverIP, int port, boolean logEnabled) {
		Log.d("TCP IN", "starting TCP");
		connected = true;
		connectionType = TCP;
		this.serverIP = serverIP;
		this.serverPort = port;
		this.logEnabled = logEnabled;
		new connectTask().execute("");
		onConnect();
	}

	public void openConnection(boolean logEnabled, UsbSerialDriver driver) {
		Log.d("TCP IN", "starting USB");
		connected = true;
		connectionType = USB;
		this.logEnabled = logEnabled;
		this.driver = driver;
		new connectTask().execute("");
		onConnect();
	}

	/**
	 * State of the MAVlink Connection
	 * 
	 * @return true for connected
	 */
	public boolean isConnected() {
		return connected;
	}

}
