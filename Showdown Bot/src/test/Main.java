package test;

import java.io.IOException;
import java.net.UnknownHostException;

import server.ShowdownClient;
import server.WebSocket;

public class Main {
	public static void main(String[] args) throws UnknownHostException, IOException {
		ShowdownClient sc = new ShowdownClient("custtest","custtest");
	}
}
