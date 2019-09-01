package server;

public class Frame {

	public final int fin, opcode;
	public final long payloadLength;
	public final String payload;

	public Frame(int fin, int opcode, long payloadLength, String payload) {
		this.fin = fin;
		this.opcode = opcode;
		this.payloadLength = payloadLength;
		this.payload = payload;
	}
}
