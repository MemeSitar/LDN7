import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ChatClient extends Thread
{
	protected int serverPort = 8888;
	private String username;


	public static void main(String[] args) throws Exception {
		String username;
		
		// DO NOT FIX THIS IT BREAKS THE CLIENT
		@SuppressWarnings({ "resource" }) 
		Scanner sc = new Scanner(System.in);
			System.out.printf("Please input username: \n");
			username = sc.next();
		
		new ChatClient(username);
	}

	public ChatClient(String username) throws Exception {
		Socket socket = null;
		DataInputStream in = null;
		DataOutputStream out = null;
		this.username = username;
		ChatClientMessageReceiver message_receiver = null;

		// connect to the chat server
		try {
			System.out.println("[system] connecting to chat server ...");
			socket = new Socket("localhost", serverPort); // create socket connection
			in = new DataInputStream(socket.getInputStream()); // create input stream for listening for incoming messages
			out = new DataOutputStream(socket.getOutputStream()); // create output stream for sending messages

			Message loginMessage = new Message("LOGIN", username, null, "");
			this.sendMessage(loginMessage, out);
			
			System.out.println("[system] connected");

			message_receiver = new ChatClientMessageReceiver(in); // create a separate thread for listening to messages from the chat server
			message_receiver.start(); // run the new thread
		} catch (Exception e) {
			e.printStackTrace(System.err);
			System.exit(1);
		}

		// read from STDIN and send messages to the chat server
		BufferedReader std_in = new BufferedReader(new InputStreamReader(System.in));

		this.readUserInput(std_in, out);
		
		// cleanup
		out.close();
		in.close();
		std_in.close();
		socket.close();
		System.exit(0);
	}
	
	private void readUserInput(BufferedReader std_in, DataOutputStream out) throws IOException{
		String userInput;
		while ((userInput = std_in.readLine()) != null) { // read a line from the console
			if (userInput.equals("exit()")){
				return;
			} else if (userInput.length() > 1 && userInput.substring(0, 1).equals("@")
				 && userInput.indexOf("[") != -1 && userInput.indexOf("]") != -1){ 
				// if message is private
				// also checks that other strings still come through
				int endOfUsername = userInput.indexOf("]");
				String receiver = userInput.substring(userInput.indexOf("[") + 1, endOfUsername);
	
				//! THIS IS HERE FOR DEBUGGING
				//*System.out.printf("THIS IS A PRIVATE MESSAGE\n");
				//*System.out.printf("THE RECEIVER IS: %s\n", receiver);
	
				// catches out of bounds exceptions. (invalid PMs)
				try {
					Message message = new Message("PRIVATE",
					 this.username, receiver, userInput.substring(endOfUsername + 1).trim());
					this.sendMessage(message, out);
				} catch (Exception e) {
					System.out.printf("[system] invalid private message\n");
					//e.printStackTrace(System.err);
				}
	
			} else { // message is public
				Message message = new Message("PUBLIC", username, null, userInput);
				this.sendMessage(message, out);
			}
		}
	}

	private void sendMessage(Message message, DataOutputStream out) {
		if (message.isOnlyWhitespace()){
			return;
		}
		try {
			out.writeUTF(message.toJSONString()); // send the message to the chat server
			out.flush(); // ensure the message has been sent
		} catch (IOException e) {
			System.err.println("[system] could not send message");
			e.printStackTrace(System.err);
		}
	}
}

// wait for messages from the chat server and print the out
class ChatClientMessageReceiver extends Thread {
	private DataInputStream in;

	public ChatClientMessageReceiver(DataInputStream in) {
		this.in = in;
	}

	public void run() {
		try {
			String msg_received;
			Message message;
			while ((msg_received = this.in.readUTF()) != null) { // read new message (from DataInputStream)
				message = Message.fromJSON(msg_received);
				System.out.printf("%s", message); // print the message to the console
			}
		} catch (Exception e) {
			if (e instanceof SocketException){
				//! for debugging purposes.
				//*System.err.println("[system] socket closed outside of thread.");
				return;
			} else {
				System.err.println("[system] could not read message");
				e.printStackTrace(System.err);
				System.exit(1);
			}
		}
	}
}
