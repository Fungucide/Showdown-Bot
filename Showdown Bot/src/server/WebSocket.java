package server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Base64;
import java.util.Random;

public class WebSocket {

	private final Socket socket;
	private final DataInputStream reader;
	private final DataOutputStream writer;
	private int lastOpcode = -1;

	public WebSocket(String address, String location, int port) throws UnknownHostException, IOException {
		socket = new Socket(address, port);
		reader = new DataInputStream(socket.getInputStream());
		writer = new DataOutputStream(socket.getOutputStream());
		handshake(address, location, port);
	}

	private void handshake(String address, String location, int port) throws IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
		BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		writer.write("GET " + location + " HTTP/1.1\n");
		writer.write("Host: " + address + ":" + port + "\n");
		writer.write("Upgrade: websocket\n");
		writer.write("Connection: Upgrade\n");
		byte[] key = new byte[15];
		new Random().nextBytes(key);
		writer.write("Sec-WebSocket-Key: " + Base64.getEncoder().encodeToString(key) + "\n");
		writer.write("Sec-WebSocket-Version: 13\n\n");
		writer.flush();
		for (int i = 0; i < 5; i++) {
			reader.readLine();
		}
	}

	public Frame read() throws IOException {
		int tempByte = reader.readUnsignedByte();
		int fin = tempByte >> 4;
		int opcode = tempByte & 0xf;
		if (opcode == 0) {
			opcode = lastOpcode;
		}
		lastOpcode = opcode;
		tempByte = reader.readUnsignedByte();
		boolean mask = ((tempByte >> 7) & 1) == 1;
		int[] maskKey = new int[4];
		long payloadLength = tempByte & 0x7f;
		if (payloadLength == 126) {
			payloadLength = reader.readUnsignedByte() << 8;
			payloadLength += reader.readUnsignedByte();
		} else if (payloadLength == 127) {
			payloadLength = 0;
			for (int i = 0; i < 8; i++) {
				payloadLength += reader.readUnsignedByte() << ((7 - i) * 8);
			}
		}
		if (mask) {
			for (int i = 0; i < 4; i++) {
				maskKey[i] = reader.readUnsignedByte();
			}
		}
		StringBuffer buffer = new StringBuffer();
		for (long i = 0; i < payloadLength; i++) {
			tempByte = reader.readUnsignedByte();
			buffer.append((char) (tempByte ^ maskKey[(int) (i % 4)]));
		}
		String payload = buffer.toString();
		if (fin >> 3 != 1) {
			payload += read();
		}
		return new Frame(fin, opcode, payloadLength, payload);
	}

	public void write(String message) throws IOException {
		write(message, 1);
	}

	public void write(String message, int opcode) throws IOException {
		int length = message.length();
		writer.writeByte((1 << 7) + opcode);// FIN + RSV 1-4 + OPCODE
		if (length < 126) {
			writer.writeByte((1 << 7) + length);
		} else {
			writer.writeByte((1 << 7) + 126);
			writer.writeByte(length >> 8);
			writer.writeByte(length & 0xff);
		}
		byte[] mask = new byte[4];
		new Random().nextBytes(mask);
		for (int i = 0; i < 4; i++) {
			writer.writeByte(mask[i]);
		}
		byte[] bytes = message.getBytes();
		for (int i = 0; i < length; i++) {
			writer.writeByte(bytes[i] ^ mask[i % 4]);
		}
	}

	public void flush() throws IOException {
		writer.flush();
	}

}
