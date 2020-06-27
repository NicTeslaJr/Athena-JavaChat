// Made by Tesla
package athena.javachat;

// Import Statements.
import java.net.ServerSocket;
import java.net.Socket;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.IOException;

// Class ChatRoomData used to store some data useful in managing the ChatRoom.
class ChatRoomData extends Thread {
	
	/*Private Members*/
	// Set the total number of concurrent clients here:
	private static int totalConcurrentClients = 25;
	private static int[] possibleClientIDs = new int[totalConcurrentClients];
	// totalActiveClients tracks how many clients are active.
	private static int totalActiveClients;
	// activeClients array consists of clientIDs that are connected to the ChatRoom, -1 indicates no connection.
	private static int[] activeClients = new int[totalConcurrentClients];
	
	/*Protected Members*/
	// Sockets to connect to Clients, one socket to receive messages, one to broadcast them.
	protected static Socket[] clientSocket = new Socket[totalConcurrentClients];
	
	// Array to manage client nicknames.
	// Note: I have not made set and unset methods for nicknames as I think it is not needed. I could be wrong.
	protected static String[] nickname = new String[totalConcurrentClients];
	protected static int[] colorCode = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
	
	/*Methods*/
	// Method to initialize the possibleClientsIDs array.
	static void initPossibleClientIDs() {
		
		// clientsIDs start from 0. these clientIDs also act as the indexes of the clientSocket arrays.
		for(int i = 0 ; i < totalConcurrentClients ; i++) {
			possibleClientIDs[i] = i;
		}
		
//		// DEBUG-HELPER: Print the possibleClientsIDs array:
//		System.out.println("\n---START-DEGUG-HELPER---");
//		System.out.println("Printing possibleClientsIDs array after initialization:\npossibleClientsIDs[indexNo]: clientID");
//		for(int i = 0 ; i < totalConcurrentClients ; i++) {
//			System.out.println("possibleClientsIDs[" + i + "]: " + possibleClientIDs[i]);
//		}
//		System.out.println("---END-DEBUG-HELPER---\n");
		
	}
	
	static void initActiveClients() {
		
		// -1 indicates no connection.
		for(int i = 0 ; i < totalConcurrentClients ; i++) {
			activeClients[i] = -1;
		}
		totalActiveClients = 0;
		
//		// DEBUG-HELPER: Print the activeClients array:
//		System.out.println("\n---START-DEGUG-HELPER---");
//		System.out.println("totalActiveClients after initialization: " + totalActiveClients);
//		System.out.println("Printing activeClients array after initialization:\nactiveClients[indexNo]: clientID");
//		for(int i = 0 ; i < totalConcurrentClients ; i++) {
//			System.out.println("activeClients[" + i + "]: " + activeClients[i]);
//		}
//		System.out.println("---END-DEBUG-HELPER---\n");
		
	}
	
	static int queryActiveClients() {
		
		// Future-Tip: Remove this check if already handled by below Socket Connection code.
		if(totalActiveClients < totalConcurrentClients) {
			for(int i = 0 ; i < totalConcurrentClients ; i++) {
				if(activeClients[i] == -1) {
					activeClients[i] = -3;
					
//					// DEBUG-HELPER: Print the activeClients array:
//					System.out.println("\n---START-DEGUG-HELPER---");
//					System.out.println("totalActiveClients after setActiveClients(): " + totalActiveClients);
//					System.out.println("Printing activeClients array after setActiveClients:\nactiveClients[indexNo]: clientID");
//					for(int j = 0 ; j < totalConcurrentClients ; j++) {
//						System.out.println("activeClients[" + j + "]: " + activeClients[j]);
//					}
//					System.out.println("---END-DEBUG-HELPER---\n");
					
					return possibleClientIDs[i];
				}
			}
			return 0;
		}
		else {
			return -2;
		}
		
	}
	
	static int setActiveClients() {
		for(int i = 0 ; i < totalConcurrentClients ; i++) {
			if(activeClients[i] == -3) {
				activeClients[i] = possibleClientIDs[i];
				totalActiveClients++;
				return activeClients[i];
			}
		}
		return -2;
	}
	
	static int unsetActiveClients(int clientID) {
		
		for(int i = 0 ; i < totalConcurrentClients ; i++) {
			if(activeClients[i] == clientID) {
				activeClients[i] = -1;
				totalActiveClients--;
				
//				// DEBUG-HELPER: Print the activeClients array:
//				System.out.println("\n---START-DEGUG-HELPER---");
//				System.out.println("totalActiveClients after unsetActiveClients(): " + totalActiveClients);
//				System.out.println("Printing activeClients array after unsetActiveClients:\nactiveClients[indexNo]: clientID");
//				for(int j = 0 ; j < totalConcurrentClients ; j++) {
//					System.out.println("activeClients[" + j + "]: " + activeClients[j]);
//				}
//				System.out.println("---END-DEBUG-HELPER---\n");
				
				return clientID;
			}
		}
		return -2;
		
	}
	
	static int[] getActiveClientsArray() {
		return activeClients;
	}
	
	static int getTotalConcurrentClients() {
		return totalConcurrentClients;
	}
	
	static int getTotalActiveClients() {
		return totalActiveClients;
	}
	
}

// MessageHandler Class
class MessageHandlerServer extends ChatRoomData {
	
	/*Private Members*/
	// sendingClientID will contain clientID of the client sending a message.
	private int sendingClientID;
	private BufferedReader br;
	private static PrintStream[] ps = new PrintStream[getTotalConcurrentClients()];
	
	/*Constructor*/
	MessageHandlerServer(int id) {
		this.sendingClientID = id;
		try {
			// For receiving messages.
			this.br = new BufferedReader(new InputStreamReader(clientSocket[this.sendingClientID].getInputStream()));
			// For broadcasting messages.
			ps[this.sendingClientID] = new PrintStream(clientSocket[this.sendingClientID].getOutputStream());
		} catch(IOException ioe) {
//			ioe.printStackTrace();
		}
	}
	
	/*Methods*/
	void broadcastMsg(String outMsg) {
		
		int[] lastActiveClients = getActiveClientsArray();
		for(int i = 0 ; i < lastActiveClients.length ; i++) {
			if(lastActiveClients[i] == -1 || lastActiveClients[i] == -3) {
				continue;
			}
			else {
				try {
					ps[i].println(outMsg);
				} catch(NullPointerException npe) {
//					System.out.println(lastActiveClients[i]);
//					System.out.println(i);
				}
			}
		}
		
	}
	
	void userCommandHandler(String cmds) {
		
		int[] lastActiveClients = getActiveClientsArray();
		String cmdMsg = "";
		if(cmds.equals("/help")) {
			ps[this.sendingClientID].println("2:10:<Server>:Available Commands are: /help, /showActive,");
			ps[this.sendingClientID].println("5:10:          /changeColor#<Color_Code> and /showColorCodes");
		}
		else if(cmds.equals("/showActive")) {
			cmdMsg = "";
			for(int j = 0 ; j < lastActiveClients.length ; j++) {
				if(lastActiveClients[j] != -1 && lastActiveClients[j] != -3 ) {
					cmdMsg = cmdMsg + nickname[j] + " ";
				}
			}
//			System.out.print(cmdMsg);
			ps[this.sendingClientID].println("2:10:<Server>:Connected Clients are: " + cmdMsg);
		}
		else if(cmds.startsWith("/changeColor#")) {
			String[] changeColorArr;
			int colorNum = -1;
			try {
				changeColorArr = cmds.split("#");
				colorNum = Integer.parseInt(changeColorArr[1]);
			} catch (NumberFormatException nfe) {
//				nfe.printStackTrace();
			} catch (ArrayIndexOutOfBoundsException aiobe) {
//				aiobe.printStackTrace();
			}
			if(colorNum >= 0 && colorNum < 10) {
				colorCode[this.sendingClientID] = colorNum;
				this.broadcastMsg("4:10:<Server>:User: " + nickname[this.sendingClientID] + " has changed his color.");
			}
		}
		else if(cmds.equals("/showColorCodes")) {
			ps[this.sendingClientID].println("2:10:<Server>:Enter the Number corresponding to the Color:");
			ps[this.sendingClientID].println("5:10:           0 - Red; 1 - Yellow; 2 - Green; 3 - Blue;");
			ps[this.sendingClientID].println("5:10:           4 - Purple; 5 - Orange; 6 - Aqua; 7 - P!nk;");
			ps[this.sendingClientID].println("5:10:           8 - Parrot Green; 9 - White.");
		}
		else if(cmds.equals("/exit")) {
			// Does nothing for now. /exit condition is already handled in run() method.
			// The below code does nothing, just a placeholder;
			cmdMsg = "";
		}
		else {
			ps[this.sendingClientID].println("2:10:<Server>:Not a recognised command.");
		}
		
	}
	
	void clientBackgroundCommand(String clientCommand) {
		
		int[] lastActiveClients = getActiveClientsArray();
		String cmdMsg ="";
		if(clientCommand.equals("%ActivUsr")) {
			for(int j = 0 ; j < lastActiveClients.length ; j++) {
				if(lastActiveClients[j] != -1 && lastActiveClients[j] != -3) {
					cmdMsg = cmdMsg + colorCode[j] + ":" + nickname[j] + ";";
				}
			}
			ps[this.sendingClientID].println(cmdMsg);
		}
		
	}
	
	boolean initHandshake() {
		try {
		// This will receive the nickname for this client.
		nickname[this.sendingClientID] = br.readLine();
		ps[this.sendingClientID].println("2:10:<Server>:Welcome to Tesla's Server.");
		ps[this.sendingClientID].println("2:10:<Server>:Servers commands can be viewed by typing");
		ps[this.sendingClientID].println("5:10:          '/help' in the chat.");
		this.broadcastMsg("4:10:<Server>:User: " + nickname[this.sendingClientID] + " has entered the ChatRoom");
		} catch(IOException ioe) {
//			ioe.printStackTrace();
		}
		return true;
	}
	
	// Thread to receive messages from clients.
	@Override
	public void run() {
		
		try {
			
			String inMsg = "";
			
			for(int noOfTries = 0 ; !initHandshake() && noOfTries < 5 ; noOfTries++) {
				initHandshake();
			}
			
			// This will continue receiving messages from this client.
			while((inMsg = br.readLine()) != null) {
//				System.out.println("Client[" + this.sendingClientID + "] | <" + nickname[this.sendingClientID] + ">: " + inMsg);
				if(!inMsg.isEmpty()) {
					if(inMsg.charAt(0) == '/') {
						this.userCommandHandler(inMsg);
					} else if(inMsg.charAt(0) == '%') {
						this.clientBackgroundCommand(inMsg);
					}
					else {
						this.broadcastMsg("1:"+ colorCode[this.sendingClientID] +":<" + nickname[this.sendingClientID] + ">:" + inMsg);
					}
				}
				// Exit condition:
				if(inMsg.equals("/exit")) {
					this.broadcastMsg("4:10:<Server>:User: " + nickname[this.sendingClientID] + " has left the ChatRoom.");
//					System.out.println("***Client[" + this.sendingClientID + "] | <" + nickname[this.sendingClientID] + ">: has left the ChatRoom (/exit Command).***");
					break;
				}
			}
			
			// LEAVE THIS TWO LINES COMMENTED.
//			this.broadcastMsg("^^^User: " + nickname[this.sendingClientID] + " has abruptly left the ChatRoom^^^");
//			System.out.println("^^^Client[" + this.sendingClientID + "] | <" + nickname[this.sendingClientID] + ">: has abruptly left the ChatRoom^^^");
			unsetActiveClients(this.sendingClientID);
			this.br.close();
			ps[this.sendingClientID].close();
			clientSocket[this.sendingClientID].close();
			return;
			
		} catch(IOException ioe) {
//			ioe.printStackTrace();
			try {
				this.broadcastMsg("2:10:<Server>:User: " + nickname[this.sendingClientID] + " has left the ChatRoom.");
//				System.out.println("^^^Client[" + this.sendingClientID + "] | <" + nickname[this.sendingClientID] + ">: has abruptly left the ChatRoom.^^^");
				unsetActiveClients(this.sendingClientID);
				this.br.close();
				ps[this.sendingClientID].close();
				clientSocket[this.sendingClientID].close();
				return;
			} catch(IOException ioen) {
//				ioen.printStackTrace();
			}
		}
		
	}
	
}

// Main Class
public class Server extends ChatRoomData {
	
	/*Private Members*/
	private static int ssPort;
	private static ServerSocket ss;
	
	/*Constructor*/
	Server(int msgip) {
		ssPort = msgip;
		initPossibleClientIDs();
		initActiveClients();
	}
	
	/*Methods*/
	void startServerSocket() {
		try {
			ss = new ServerSocket(ssPort);
		} catch(IOException ioe) {
//			ioe.printStackTrace();
		}
	}
	
	// Thread to accept clients. This is the first thread to be run.
	@Override
	public void run() {
		
		int possibleID, confirmedID;
		int clientsLimit = getTotalConcurrentClients();
		
		while(true) {
			if(getTotalActiveClients() < clientsLimit) {
				possibleID = queryActiveClients();
				try {
					clientSocket[possibleID] = ss.accept();
//					System.out.println(clientSocket[possibleID]);
					confirmedID = setActiveClients();
					MessageHandlerServer mh = new MessageHandlerServer(confirmedID);
					mh.start();
				} catch(IOException ioe) {
//					ioe.printStackTrace();
				}
			}
			else {
//				System.out.println("Max Concurrent Connections Reached.");
				try {
					Thread.sleep(60000);
				} catch(InterruptedException interre) {
//					interre.printStackTrace();
				}
			}
			
		}
		
	}
	
	/*Main Method*/
	public static void main(String[] args) {
		
		if(args.length != 1) {
			System.out.println("\nUsage: java Relay_Server <port1>\n");
			System.exit(0);
		}
		else {
			//System.out.println("Main Method started...");
			try {
				Server rs = new Server(Integer.parseInt(args[0]));
				rs.startServerSocket();
				rs.start();
				//System.out.println("---This is a Test Message.---");
			} catch(NumberFormatException nfe) {
				System.out.println("Ports are Integers.");
				System.exit(0);
			}
		}
		
	}
	
}