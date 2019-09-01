package test;

import java.io.IOException;
import java.net.UnknownHostException;

import Showdown.ShowdownClient;
import server.WebSocket;

public class Main {
	public static void main(String[] args) throws UnknownHostException, IOException {
		// WebSocket ws = new WebSocket("localhost", "/", 8000);
		ShowdownClient sc = new ShowdownClient("custtest", "custtest");
	}
}
