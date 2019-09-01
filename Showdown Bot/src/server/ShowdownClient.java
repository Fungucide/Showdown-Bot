package server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class ShowdownClient {

	private final String challstr;
	private WebSocket ws;

	public ShowdownClient(String username, String password) throws UnknownHostException, IOException {
		ws = new WebSocket("sim.smogon.com", "/showdown/websocket", 8000);
		ws.read(1);

		String response = ws.read();
		challstr = response.substring(response.indexOf("challstr") + 9).replaceFirst("\\|", "%7C");
		login(username, password);
	}

	public String getChallstr() {
		return challstr;
	}

	private String login(String username, String password) throws UnknownHostException, IOException {
		Socket socket = new Socket("play.pokemonshowdown.com", 80);
		BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
		String data = "act=login&name=" + username + "&pass=" + password + "&challstr=" + challstr;
		bw.write("POST /action.php HTTP/1.1\r\n" + "Host: play.pokemonshowdown.com\r\n"
				+ "Referer: https://play.pokemonshowdown.com/\r\n"
				+ "Content-Type: application/x-www-form-urlencoded; charset=UTF-8\r\n" + "Content-Length: "
				+ data.length() + "\r\n" + "Connection: close\n\n" + data + "\n");
		bw.flush();
		StringBuffer buffer = new StringBuffer();
		do {
			buffer.append((char) br.read());
		} while (br.ready());
		String assertion = buffer.toString();
		assertion = assertion.substring(assertion.indexOf("\"assertion\":") + 13, assertion.lastIndexOf("\""));
		System.out.println("[\"|/trn " + username + ",0," + assertion + "\"]");
		ws.write("[\"|/trn " + username + ",0," + assertion + "\"]");
		ws.flush();
		System.out.println(ws.read());
		return "";
	}
}
