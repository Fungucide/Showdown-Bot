package server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Base64;
import java.util.Random;

public class WebSocket {

	private final Socket socket;
	private final BufferedReader reader;
	private final BufferedWriter writer;

	public WebSocket(String address, String location, int port) throws UnknownHostException, IOException {
		socket = new Socket(address, port);
		reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
		handshake(address, location, port);
	}

	private void handshake(String address, String location, int port) throws IOException {
		write("GET " + location + " HTTP/1.1\n");
		write("Host: " + address + ":" + port + "\n");
		write("Upgrade: websocket\n");
		write("Connection: Upgrade\n");
		byte[] key = new byte[15];
		new Random().nextBytes(key);
		write("Sec-WebSocket-Key: " + Base64.getEncoder().encodeToString(key) + "\n");
		write("Sec-WebSocket-Version: 13\n\n");
		flush();
		read(4);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			System.err.println("Unable to pause");
		}
	}

	public void write(String write) throws IOException {
		writer.write(write);
	}

	public boolean ready() {
		try {
			return reader.ready();
		} catch (IOException e) {
			return false;
		}
	}

	public void read(int n) throws IOException {
		for (int i = 0; i < n; i++)
			read();
	}

	public String read() throws IOException {
		while (!reader.ready())
			;
		StringBuffer sb = new StringBuffer();
		while (reader.ready()) {
			char c = (char) reader.read();
			if (c == '\r' || c == '\n')
				break;
			sb.append(c);
		}
		if (sb.length() == 0) {
			return read();
		} else {
			return sb.toString();
		}
	}

	public void flush() throws IOException {
		writer.flush();
	}

}
