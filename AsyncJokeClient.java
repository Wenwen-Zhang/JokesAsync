
/*--------------------------------------------------------

1. Name / Date: 

	Wenwen Zhang  11/04/2017

2. Java version used, if not the official version for the class: 

	build 1.8.0_121-b13

3. Precise command-line compilation examples / instructions:

	> javac AsyncJokeClient.java

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
	
	a. If no argument is passed, it would be connected to the server at port 4545.
	b. The client will maintain a UDP server at port number 6000+.

----------------------------------------------------------*/

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.util.Random;
import java.util.UUID;

public class AsyncJokeClient {
	
	//The server name, in this case, will always be localhost
	public static final String server = "localhost";

	//Create a UDP port number
	final static int udpPort = 6000 + new Random().nextInt(100);
	
	//The port numbers for Server A and Server B
	public static int port1 = 0;	
	public static int port2 = 0;

	public static void main(String args[]) {
		//Create an unique uuid string to represent this client
		final String ID = UUID.randomUUID().toString(); 

		//If no argument is passed, the server port is 4545
		//Otherwise, assign the arguments to the corresponding port numbers
		if (args.length < 1) 		
			port1 = 4545;
		else if (args.length == 1) 
			port1 = Integer.parseInt(args[0]);
		else if (args.length > 1) 
		{
			port1 = Integer.parseInt(args[0]);
			port2 = Integer.parseInt(args[1]);
		}
		
		//Print start up messages
		System.out.println("Wenwen Zhang's Asynchrouous JokeClient started with bindings: ");
		System.out.println("Server A at Port " + port1 + ".");
		System.out.println("Server B at Port " + port2 + ".\n");		
		
		//Create a thread to handle the UDP server in this client
		UDPThread UDPT = new UDPThread(udpPort);
		Thread udp = new Thread(UDPT);
		udp.start();

		// To get the signals that will be sent to the server.
		BufferedReader inputHolder = new BufferedReader(new InputStreamReader(System.in));

		try {

			String entry = null; // To store the operation user typed in.

			do {
				//Ask for input
				display("Enter A or B to get a joke or proverb, or numbers for sum: ");
				entry = inputHolder.readLine(); // Read the input.

				//Get response from Server A
				if (entry.toUpperCase().equals("A")) {					
					getSomething(port1, ID);	
				}
				
				//Get response from Server B
				else if (entry.toUpperCase().equals("B")) {				
					getSomething(port2, ID);	
				}
				
				//Do local work, calculate the sum of the two numbers entered
				else
				{
					try{				
						int n1 = Integer.parseInt(entry.split(" ")[0]);
						int n2 = Integer.parseInt(entry.split(" ")[1]);						
						display("Your sum is: " + (n1 + n2) + ".\n");						
					}
					//If the calculation could not be completed, continue to the next cycle.
					catch(Exception x) {continue;}
				}
			}

			// When 'quit' is typed in, close this client.
			while (!entry.equals("quit"));
			System.out.println("Cancelled by user request.");
			System.exit(0);
		}

		// If anything goes wrong, catch the exception, and print out the error
		// information, so that the program can keep running.
		catch (IOException x) {
			x.printStackTrace();
		}
	}
	
	//This method is to send the id and the UDP port number to server to get jokes or proverbs later.
	public static void getSomething(int port, String id) {
		Socket sock; 

		PrintStream toServer;

		try {
			// Create a socket connecting to the the server and the specified
			// port number passed in.
			sock = new Socket(server, port);

			// Create a printStream to store the output stream that will be
			// sending to the server.
			toServer = new PrintStream(sock.getOutputStream());

			// Tell the server the unique ID of this client.
			toServer.println(id);
			toServer.flush();
			
			// Send the UDP port number
			toServer.println(AsyncJokeClient.udpPort);
			toServer.flush();

			sock.close(); // Close the local socket.
			
		} catch (IOException x) {
			System.out.println("Socket error.");
			x.printStackTrace();
		}
	}
	
	//A synchronized method to display outputs on the console
	public static synchronized void display(String s) {
		
		System.out.print(s);
		System.out.flush();
	}
}

class UDPThread implements Runnable {
	
	//Port number
	int port;
	
	//Constructors
	UDPThread() {}
	UDPThread(int p) {port = p;}

	public void run() {
		
		try {			
			//Create a Datagram socket with the passed port number
			DatagramSocket aSocket = new DatagramSocket(port);
			
			//To contain the requests received
			byte[] buffer = new byte[1000];
			
			//Always listening
			while (true) {
				
				//Get the request and convert to string form
				DatagramPacket request = new DatagramPacket(buffer, buffer.length);			
				aSocket.receive(request); 			
				String req = new String(request.getData());
				
				//Get the server port to determine from which server the request comes
				int portServer = Integer.parseInt(req.substring(0, 4));
				
				//The header string to show the specific server
				String header = "";
				
				//Get the correct header accoring to the portServer variable
				if (portServer == AsyncJokeClient.port1)
					header = "Server A responds: ";
				else if (portServer == AsyncJokeClient.port2)
					header = "Server B responds: ";
				
				//Get the mode of the response
				String mode = req.substring(4, 5);
				
				//Create a reply string based on the mode
				String replyString = "";		
				if (mode.equals("J"))
				{
					replyString = req.substring(5, 12) + " has been received.\n";
				}
				else if (mode.equals("P"))
				{
					replyString = req.substring(5, 15) + " has been received.\n";
				}
				
				//Display the jokes or proverbs with correct header
				AsyncJokeClient.display("\n" + header + req.substring(5) + ".\n");				
				
				//Send the reply back to the server
				byte[] ack = replyString.getBytes();
				DatagramPacket reply = new DatagramPacket(ack, replyString.length(), request.getAddress(),
						request.getPort());
				aSocket.send(reply);				
			}
		} catch (IOException ioe) {System.out.println(ioe);}
	}
}


