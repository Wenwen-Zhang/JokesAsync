
/*--------------------------------------------------------

1. Name / Date: 

	Wenwen Zhang  11/04/2017

2. Java version used, if not the official version for the class: 

	build 1.8.0_121-b13

3. Precise command-line compilation examples / instructions:

	> javac AsyncJokeClientAdmin.java

4. Precise examples / instructions to run this program:

	Run the following files in separate shell windows:

	> java AsyncJokeServer 3245
	> java AsyncJokeServer 3246
	> java AsyncJokeClient 3245 3246
	> java AsyncJokeClientAdmin 3245 3246

	All acceptable commands are displayed on the various consoles.

5. List of files needed for running the program.

	a. AsyncJokeServer.java
	b. AsyncJokeClient.java
	c. AsyncJokeClientAdmin.java

6. Notes:

	a. For the AsyncJokeClientAdmin, the port number to admin the server will the be argument port number + 505, 
	if no argument is passed, the default port number will be 4545 + 505 = 5050

----------------------------------------------------------*/

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

public class AsyncJokeAdminClient {
	
	// Two port number, port1 for primary server, port2 for secondary server.
	public static int port1 = 0;
	public static int port2 = 0;
	
	//The server name, in this case, will always be localhost
	public static final String server = "localhost";

	// A string flag to check if the current connected server is server A or server B
	public static String serverOn = "A";

	public static void main(String args[]) {
		
		//If no argument is passed, the server port is 5050
		//Otherwise, assign the arguments to the corresponding port numbers 
		if (args.length < 1) 
			port1 = 5050;
		else if (args.length == 1) 
			port1 = Integer.parseInt(args[0]) + 505;
		else if (args.length > 1) 
		{
			port1 = Integer.parseInt(args[0]) + 505;
			port2 = Integer.parseInt(args[1]) + 505;			
		}

		System.out.println("This is an Administration client.");
		System.out.println("Server A at Port: " + port1 + "\n");
		System.out.println("Server B at Port: " + port2 + "\n");

		// According the server flag, print if the currently connected server is
		// Server A or Server B.
		if (serverOn.equals("A")) {
			System.out.println("Currently is connected with Server A. \n");
		} else {
			System.out.println("Currently is connected with Server B. \n");
		}

		// Create a BufferedReader object to store the user's input.
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

		try {
			String entry; // To hold the input.

			do {
				System.out.print("Enter your operation: \n" + "	'J' for Joke Mode,\n" + "	'P' for Proverb Mode,\n"
						+ "	'S' to switch server, \n" + "	'quit' to end, \n" + "	'shutdown' to close the server: ");
				System.out.flush();

				// Get the input, and make it consistently upper case for easy
				// comparison,
				entry = in.readLine().toUpperCase();

				// Get the server port number based on the serverOn flag.
				int port = (serverOn.equals("A") ? port1 : port2);

				// If the input command is 'j' or 'p', switch the server mode.
				if (entry.equals("J") || entry.equals("P"))
					changeServerMode(entry, port);

				// If the input command is 's', it means to switch servers.
				else if (entry.equals("S")) {
					
					// Switch the servers, and print summary messages on the
					// console.
					if (serverOn.equals("A")) {
						serverOn = "B";
						System.out.println("Now admistrating Server B at Port : " + port2 + ".\n");
					} else {
						serverOn = "A";
						System.out.println("Now administraing Server A at Port : " + port1 + ".\n");
					}			
					
				}

				// If the command is 'shutdown', close the server.
				else if (entry.equals("SHUTDOWN")) {
					changeServerMode(entry, port);
					String toPrint = "";
					if (serverOn.equals("A")) toPrint = "A";
					else toPrint = "B";
					System.out.println("Server " + toPrint + " has been shut down.\n");
				}

				// Ask for valid command if the input is not recognized.
				else if (!entry.equals("QUIT")) {
					System.out.println("Invalid input, please follow the instruction and re-enter.");
					System.out.flush();
				}
			} while (!entry.equals("QUIT")); // If 'quit' is typed, close this
												// client.

			System.out.println("AdminClient Cancelled.");
			System.exit(0);
		}

		//If anything goes wrong, catch the exception, and print out the error
		//information, so that the program can keep running.
		catch (IOException x) {
			x.printStackTrace();
		}
	}

	// This method is to send command typed to the connected server to manage
	// the server.
	public static void changeServerMode(String mode, int port) {
		Socket sock; // Create a socket.

		BufferedReader fromServer;
		PrintStream toServer;
		String textFromServer;
		

		try {
			// Create a socket connecting to the the server and the specified
			// port number passed in.
			sock = new Socket(server, port);

			// Create a bufferedReader object to buffer the input stream got
			// from the socket.
			fromServer = new BufferedReader(new InputStreamReader(sock.getInputStream()));

			// Create a printStream to store the output stream that will be
			// sending to the server.
			toServer = new PrintStream(sock.getOutputStream());

			// Tell the server what mode to be running.
			toServer.println(mode);
			toServer.flush();

			// Read the responses from the server after sending an operation
			// command.
			textFromServer = fromServer.readLine();
			if (textFromServer != null)
				System.out.println(textFromServer);

			sock.close(); // Close the local socket.
		} catch (IOException x) {
			System.out.println("Socket error.");
			x.printStackTrace();
		}
	}

}
