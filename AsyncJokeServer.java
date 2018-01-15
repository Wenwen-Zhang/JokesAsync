
/*--------------------------------------------------------

1. Name / Date: 

	Wenwen Zhang  11/04/2017

2. Java version used, if not the official version for the class: 

	build 1.8.0_121-b13

3. Precise command-line compilation examples / instructions:

	> javac AsyncJokeServer.java

4. Precise examples / instructions to run this program:

	Run the following files in separate shell windows:

	> java AsyncJokeServer 3245
	> java AsyncJokeServer 3246
	> java AsyncJokeClient 3245 3246
	> java AsyncJokeClientAdmin 3245 3246

	All acceptable commands are displayed on the various consoles.

	
	For the AsyncJokeClientAdmin, the port number to admin the server will the be argument port number + 505, 
	if no argument is passed, the default port number will be 4545 + 505 = 5050

5. List of files needed for running the program.

	a. AsyncJokeServer.java
	b. AsyncJokeClient.java
	c. AsyncJokeClientAdmin.java

6. Notes:

	a. For the AsyncJokeServer, if no argument is passed, the default port number is 4545.
	And the ports for the admin client will be the port number + 505. 
	b. There are 10 jokes and 10 proverbs stored in this server. 
	c. When a client gets connected, its UUID then is stored in this server along with 4 randomly 
	selected jokes and 4 random proverbs. 
	d. Depending on the mode, jokes and proverbs are sent to the client one at a time, when all 4 
	jokes/proverbs have been sent, randomly select new 4 jokes/proverbs to be stored with the unique UUID. 

----------------------------------------------------------*/

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class AsyncJokeServer {
	
	// Store the id of each connected client and the four random selected
	// jokes/proverbs.
	public static HashMap <String, ArrayList<Integer> > jokesClients; 
	public static HashMap <String, ArrayList<Integer> > proverbsClients;
	
	// Store the state of each connected client
	public static HashMap <String, Integer> clientState;

	// 10 jokes and 10 proverbs stored in each hashMap. 
	public static HashMap <Integer, String> jokes;
	public static HashMap <Integer, String> proverbs;
	
	// Server mode, J for Joke, P for Proverb, Joke mode is the default setting.
	public static String mode = "J";
	
	// Port number
	public static int port = 0;
	
	// The seconds for which the server is going to sleep 
	public static int sleepSeconds = 40000;

	public static void main(String args[]) throws IOException {
		
		int q_len = 6; // Maximum requests the server can accept simultaneously.

		Socket sock; // Create a local socket.
		
		//If an argument is detected, assign it to port number, and change sleep value
		if (args.length > 0) {
			port = Integer.parseInt(args[0]);
			sleepSeconds = 70000;
		}
		//Otherwise, the port number is 4545
		else
			port = 4545;

		// Initialize these client hash map to be ready to store client's
		// information.
		jokesClients = new HashMap<>();
		proverbsClients = new HashMap<>(); 		
		clientState = new HashMap<>();

		// Get jokes and proverbs ready for use.
		getPoolReady();

		// Start a new thread to wait for the Admin Client's connection.
		AdminThread AT = new AdminThread();
		Thread t = new Thread(AT);
		t.start();

		/*
		 * Create a server socket object which is bounded to the specified port
		 * number, and has a capacity of 6 simultaneous requests.
		 */	
		ServerSocket primarysock = new ServerSocket(port, q_len);
		

		// Print out the message that the server is working and ports at which
		// are listening for connections.
		System.out.println("Wenwen Zhang's Asynchronous JokeServer staring up, listening at Port " +  port + ".\n");		
		
		while (true) // The server runs forever, waiting for connections.
		{
			/*
			 * Server socket listens for connection requests made from clients,
			 * and if any, accepts it and assigns to the local socket.
			 */
			sock = primarysock.accept();

			// Create a new thread with this connected local socket and start
			// running this thread.
			new Worker(sock).start();
		}
	}

	// Initialize the joke maps and proverb maps, put 10 entries into each one.
	public static void getPoolReady() {
		
		jokes = new HashMap<>();
		proverbs = new HashMap<>();
		
		jokes.put(0, "[Joke0] What's orange and sounds like a parrot? A carrot.");
		jokes.put(1, "[Joke1] What do you call it when Batman skips church? Christian Bale.");
		jokes.put(2, "[Joke2] Two fish are sitting in a tank. One looks over at the other and says: \"Hey, do you know how to drive this thing?\"");
		jokes.put(3, "[Joke3] I told my doctor that I broke my arm in two places. He told me to stop going to those places.");
		jokes.put(4, "[Joke4] I told my girlfriend she drew her eyebrows too high. She seemed surprised.");
		jokes.put(5, "[Joke5] Two cows are sitting in a field, and one says to the other, \"so, how about that mad cow disease? Scary stuff, right?\" To which to other replies, \"terrifying. But what do I care? I’m a helicopter.\"");
		jokes.put(6, "[Joke6] What did the 0 say to the 8? Nice belt!");
		jokes.put(7, "[Joke7] Why is six afraid of seven? Because seven ate nine.");
		jokes.put(8, "[Joke8] Two muffins are in an oven. One muffin says \"gosh, it’s hot in here\". The other muffin screams \"AAAH!! A talking muffin!\"");
		jokes.put(9, "[Joke9] What do you call bears with no ears? B");
		
		proverbs.put(0, "[Proverb0] The early bird catches the worm.");
		proverbs.put(1, "[Proverb1] Actions speak louder than words.");
		proverbs.put(2, "[Proverb2] When in Rome, do as the Romans.");
		proverbs.put(3, "[Proverb3] The squeaky wheel gets the grease.");
		proverbs.put(4, "[Proverb4] When the going gets tough, the tough get going.");
		proverbs.put(5, "[Proverb5] Fortune favors the bold.");
		proverbs.put(6, "[Proverb6] Hope for the best, but prepare for the worst.");
		proverbs.put(7, "[Proverb7] Birds of a feather flock together.");
		proverbs.put(8, "[Proverb8] Better late than never.");
		proverbs.put(9, "[Proverb9] There's no such thing as a free lunch.");
	}
}

/*
 * Inherits Thread class and override the run() method so that the server can be
 * multi-threaded.
 */
class Worker extends Thread {
	Socket sock; // A local socket for connection.

	Worker(Socket s) {
		sock = s;
	} // Assign s to sock when this constructor is called.

	// Used to get the bits for client's joke state or proverb state.
	public static final int JMask = 0xF0; 
	public static final int PMask = 0x0F; 

	public void run() {
		
		BufferedReader in = null;

		try {
			// Get the client's input, in this case, including client's id and
			// udp port number.
			in = new BufferedReader(new InputStreamReader(sock.getInputStream()));

			try {
				// Get the client's unique id which will used as a key to
				// associate the jokes/proverbs array.
				String ID = in.readLine();
				
				//If the client has visited before, update its state, 
				//Otherwise, add this client and initialize its state to 0
				int state = 0;				
				if (AsyncJokeServer.clientState.containsKey(ID))	
					state = AsyncJokeServer.clientState.get(ID);				
				else				
					AsyncJokeServer.clientState.put(ID, 0);				
				
				// Get the client's UDP port number				
				int udpPort = Integer.parseInt(in.readLine());
				
				//Break the connection
				sock.close();
				
				//Sleep for a while
				Thread.sleep(AsyncJokeServer.sleepSeconds);
				
				// Call this method to send jokes or proverbs via UDP port
				giveSomething(ID, state, udpPort);
			}
			
			// If anything goes wrong, keep the program running by catch the
			// exception and print out the error message.
			catch (Exception x) {
				System.out.println("Server read error");
				x.printStackTrace();
			}

		} catch (IOException ioe) {
			System.out.println(ioe);
		}
	}		

	//This method is to send the jokes or proverbs using UDP
	public static void callBack(int udp, String content)
	{
		DatagramSocket aSocket = null;
		
		try 
		{	
			//Create a Datagram Socket instance
			aSocket = new DatagramSocket();
			
			//Convert the content to bytes
			byte[] m = content.getBytes();
			
			//Get the address of the server, in this case, the server is localhost
			InetAddress aHost = InetAddress.getByName("localhost");

			//Create a datagram packet to send
			DatagramPacket request = new DatagramPacket(m, content.length(), aHost, udp);
			aSocket.send(request);

			//Create a buffer to contain the reply
			byte[] buffer = new byte[1000];
			
			//Get the reply from the UDP server
			DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
			aSocket.receive(reply);
			
			//Display the reply
			System.out.println("Reply: " + new String(reply.getData()));

		} catch (SocketException e) {
			System.out.println("Socket: " + e.getMessage());
		} catch (IOException e) {
			System.out.println("IO: " + e.getMessage());
		}
	}

	public static void giveSomething(String id, int n, int udp) throws IOException {
				
		// The updated integer which is used to get the joke/proverb
		// that will be send to client.
		int update; 
		
		// Call the readState() method to get the
		// client's state, it would be 0 to 4.
		int stateOld = readState(n); 

		// Check if the server is on Proverb mode
		if (AsyncJokeServer.mode.equals("P")) 
		{	
			// if the state is 1, 2, or 3, it means that the client is not first
			// time connected, and has an associated array list of 4 proverbs
			// which has had several items being sent before.
			if (stateOld > 0 && stateOld < 4) 
			{			
				update = stateOld; // Get the current proverb state of the client
			}

			// if the state is not 1, 2, or 3, it means either the client is
			// first time connected, or the associated 4 proverbs had 
			// all been sent to the client, a new cycle need to be started.
			else {
				// Call the randList() method to generate an array with 4 random
				// selected proverb's indexes in range [0, 9].
				AsyncJokeServer.proverbsClients.put(id, randList());
				// Start a new cycle, grab the first item in the id-associated array.
				update = 0; 
			}

			// Print out on the console which proverb will be sent to the client
			// in this request.
			System.out.println("Sending Proverb #" + AsyncJokeServer.proverbsClients.get(id).get(update) 
								+ " to Clent " + id + ".\n");

		} else // The server is on Joke mode
		{
			// if the state is 1, 2, or 3, it means that the client is not first
			// time connected, and has an associated array list of 4 jokes 
			// which has had several items being sent before.
			if (stateOld > 0 && stateOld < 4) {
				update = stateOld; // Get the current joke state of the client
			}

			// if the state is not 1, 2, or 3, it means either the client is
			// first time connected, or the associated 4 proverbs had all 
			// been sent to the client, a new cycle need to be started.
			else {
				// Call the randList() method to generate an array with 4 random
				// selected proverb's indexes in range [0, 9].
				AsyncJokeServer.jokesClients.put(id, randList());
				// Start a new cycle, grab the first item in the id-associated array.
				update = 0; 
			}

			// Print out on the console which proverb will be sent to the client
			// in this request.
			System.out.println("Sending Joke #" + AsyncJokeServer.jokesClients.get(id).get(update) 
								+ " to Clent " + id + ".\n");
		}
				
		// Update the state
		updateState(id, n, stateOld);
		// Construct the content that will be sent to the client
		String content = AsyncJokeServer.port + AsyncJokeServer.mode + getOutputs(id, update);
		// Call callback() method to send the content
		callBack(udp, content);
	}

	// This method is to generate an array containing 4 integers in range [0, 9]
	public static ArrayList<Integer> randList() {
		
		Random rand = new Random();
		ArrayList<Integer> arr = new ArrayList<Integer>();
		int i = 0;
		while (i < 4) {
			int n = rand.nextInt(10);
			
			// Add to the array only when the random integer is not in the array,
			if (!arr.contains(n)) 
			{
				arr.add(n);
				i++;
			}
		}
		return arr;
	}

	// This method is to get the corresponding Joke/Proverb state of the client.
	public static Integer readState(int n) {
		
		int ret = 0;

		// Mask the state to get the corresponding four bits.
		int temp = (AsyncJokeServer.mode.equals("P") ? (n ^ PMask) : (n ^ JMask));

		// Offset is used to pointed to the correct bits in different mode.
		int offset = (AsyncJokeServer.mode.equals("P") ? 0 : 4);

		// Count how many bits are 1, the result is exactly how many jokes or
		// proverbs had been sent before.
		for (int i = 0 + offset; i < 4 + offset; i++) {
			ret += (temp >> i) & 1;
		}
		return ret;
	}

	// This method is to update the client's state.
	public static void updateState(String id, int n, int s) {
		int ret = 0;

		// Offset is used to pointed to the correct bits in different mode
		int offset = (AsyncJokeServer.mode.equals("P") ? 0 : 4);

		// If the state is 1, 2, or 3, turn on the next bit.
		if (s > 0 && s < 4) {
			ret = n | (1 << (s + offset));
		}

		// If the state is 0 or 4, switch all 4 bits to zero, and turn on the
		// first bit to start a new cycle.
		else {
			n = (AsyncJokeServer.mode.equals("P") ? (n & 0xF0) : (n & 0x0F));
			ret = n | (1 << offset);
		}
		
		AsyncJokeServer.clientState.put(id, ret);
	}
	

	// This method is to grab the corresponding joke/proverb based on the client
	// state.
	public static String getOutputs(String id, int n) {
		if (AsyncJokeServer.mode.equals("P")) {
			return AsyncJokeServer.proverbs.get(AsyncJokeServer.proverbsClients.get(id).get(n));
		} 
		else {
			return AsyncJokeServer.jokes.get(AsyncJokeServer.jokesClients.get(id).get(n));
		}
	}
}

// A thread class to enable the connections and executions of admin clients.
class AdminThread implements Runnable {
	public static int port = AsyncJokeServer.port + 505; // Get the correct port number for the admin

	AdminThread() {} // constructor.

	public void run() {
		
		int q_len = 6;
		Socket sock;

		try {
			ServerSocket adminsock = new ServerSocket(port, q_len);
			while (true) {
				// wait for the connections requested from admin clients.
				sock = adminsock.accept();
				new AdminWorker(sock).start();
			}
		} catch (IOException ioe) {
			System.out.println(ioe);
		}
	}
}

// An admin worker thread to run the admin clients.
class AdminWorker extends Thread {
	Socket sock; // A local socket object sock.

	AdminWorker(Socket s) {
		sock = s;
	} // Assign s to sock when this constructor is called.

	public void run() {
		PrintStream out = null;
		BufferedReader in = null;

		// Print out that the admin client has been connected.
		System.out.println("AdminClient connected at port " + AdminThread.port + ".");

		try {
			// Get the command send by the admin client.
			in = new BufferedReader(new InputStreamReader(sock.getInputStream()));

			// Print out the message to show on the console of admin client.
			out = new PrintStream(sock.getOutputStream());

			try {
				String oldMode = AsyncJokeServer.mode; // Get the current mode of the server.
				AsyncJokeServer.mode = in.readLine(); // Get the command sent by the admin.
				// Initialize a String which will be displayed on both the server and the admin.
				String toDisplay = null; 
				// If the command is 'shutdown', print a message and notify the admin, then close the server.
				if (AsyncJokeServer.mode.equals("SHUTDOWN")) {
					System.out.println("Shut down by the Admin Client. Closing...");
					System.out.flush();
					out.println("Server has been shut down.");
					System.exit(0);
				}
				// If the command is P or J, switch between proverb mode and joke mode.
				else {
					toDisplay = (AsyncJokeServer.mode.equals("P") ? "Proverb Mode." : "Joke Mode.");
				}

				// If the server is already on the demanded mode, notify the admin that nothing needs to be changed.
				if (AsyncJokeServer.mode.equals(oldMode)) {
					out.println("Currently on this mode, no need to change.");
				}

				// Print summary message indicating the current mode the server is on.
				else {
					System.out.println("Server Mode has been changed, now it is on " + toDisplay + "\n");
					System.out.flush();
					out.println("Server Mode has been changed, now it is on " + toDisplay + ".");
				}
			}

			 //If anything goes wrong, keep the program running by catch the
			 //exception and print out the error message.		 
			catch (IOException x) {
				System.out.println("Server read error");
				x.printStackTrace();
			}

			sock.close(); // Close this socket.
		} catch (IOException ioe) {
			System.out.println(ioe);
		}
	}
}

