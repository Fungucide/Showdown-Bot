package Showdown;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.json.JSONObject;

import server.Frame;
import server.WebSocket;

public class ShowdownClient {

	private String challstr;
	private WebSocket ws;
	private boolean loggedIn = false;
	private ArrayList<String> formats;
	private boolean autoAccept = true;
	private ArrayList<Challenge> pendingChallenges;
	private HashMap<String, Game> games;
	private final String username;
	private final String password;

	public ShowdownClient(String username, String password) throws UnknownHostException, IOException {
		this.username = username;
		this.password = password;
		pendingChallenges = new ArrayList<Challenge>();
		ws = new WebSocket("sim.smogon.com", "/showdown/websocket", 8000);
		handler();
	}

	private void handler() throws IOException {
		String[] input;
		Game game;
		while (true) {
			Frame frame = ws.read();
			if (frame.opcode == 0x9) {
				ws.write(frame.payload, 0xA);
				continue;
			}
			input = frame.payload.split("\n");
			for (String s : input) {
				if (!s.startsWith(">")) {
					process(s.substring(1, s.indexOf('|', 1)).strip(), s.substring(s.indexOf('|', 1) + 1).strip());
				} else {
					System.out.println(s);
				}
			}
		}
	}

	public void process(String dataType, String data) {
		try {
			Method handle = ShowdownClient.class.getDeclaredMethod(dataType + "Handle", String.class);
			handle.invoke(this, data);
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			e.printStackTrace();
			System.err.println("No function: " + dataType + " found");
			System.err.println(">" + data);
		}
	}

	public String getChallstr() {
		return challstr;
	}

	private void login() throws UnknownHostException, IOException {
		Socket socket = new Socket("play.pokemonshowdown.com", 80);
		BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
		String data = "act=login&name=" + username + "&pass=" + password + "&challstr=" + challstr;
		bw.write("POST /action.php HTTP/1.1\r\n" + "Host: play.pokemonshowdown.com\r\n"
				+ "Referer: https://play.pokemonshowdown.com/\r\n"
				+ "Content-Type: application/x-www-form-urlencoded; charset=UTF-8\r\n" + "Content-Length: "
				+ data.length() + "\r\n" + "Connection: close\n\n" + data + "\n");
		bw.flush();
		while (br.readLine().length() != 0);
		int length = Integer.parseInt(br.readLine(), 0x10);
		char[] buffer = new char[length];
		br.read(buffer);
		System.out.println(new String(buffer).substring(1));
		JSONObject json = new JSONObject(new String(buffer).substring(1));
		ws.write("|/trn " + username + ",0," + json.getString("assertion") + "|");
		ws.flush();
	}

	private void updateuserHandle(String data) {
		loggedIn = data.split("\\|")[0].equals(username);
		System.out.println("Logged In: " + loggedIn);
	}

	private void formatsHandle(String data) {
		formats = new ArrayList<String>();
		String[] parts = data.split("\\|");
		for (int i = 0; i < parts.length; i++) {
			if (parts[i].startsWith(",")) {
				i++;
			} else {
				parts[i] = parts[i].substring(0, parts[i].indexOf(',')).replaceAll("[^A-Za-z1-9]", "").toLowerCase();
				formats.add(parts[i]);
			}
		}
	}

	private void challstrHandle(String data) throws UnknownHostException, IOException {
		challstr = data.replaceFirst("\\|", "%7C");
		if (!loggedIn) {
			login();
		}
	}

	private void updatesearchHandle(String data) {
		// System.out.println("updatesearch|" + data);
	}

	private void updatechallengesHandle(String data) throws IOException {
		JSONObject json = new JSONObject(data).getJSONObject("challengesFrom");
		Iterator<String> iterator = json.keys();
		for (; iterator.hasNext(); )
			System.out.println(iterator.next());

		data = data.substring(data.indexOf(':') + 2, data.indexOf(',') - 1);
		if (data.length() != 0) {
			String[] usernames = data.split(",");
			for (String s : usernames) {
				String[] parts = s.replaceAll("\"", "").split(":");
				if (autoAccept) {
					ws.write("|/accept " + parts[0]);
				} else if (!pendingChallenges.contains(new Challenge(parts[0], parts[1]))) {
					System.out.println("Challenge from: " + parts[0] + " " + parts[1]);
					pendingChallenges.add(new Challenge(parts[0], parts[1]));
				}
			}
		}
	}
}
