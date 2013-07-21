import java.io.*;
import java.util.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.BufferUnderflowException;


public class RUBTClient {
	static private String PEER_ID = "PARKSDEOLIVEIRA";
	static private String PEER_KEY = "peers";
	static private String[] COMPARE_IP = {"128.6.171.3", "128.6.171.4"}; // ** ONLY USED TO CHOOSE THE CORRECT IP OUT OF THE PEER LIST **
	/**  Function randomly generates the first 5 integers for the peer id
	  */
	public static void generateID()
	{
		for(int i = 0; i < 5; i++)
		{
			int random = (int)(Math.random()*10);
			PEER_ID = ""+random + PEER_ID;
		}
	}

	public static byte[] readTorrent(String filename) throws IOException{
		File file = new File(filename);

		int fileLen = (int) file.length();
		FileInputStream torrent = null;
		
		try {
			//Checking if file is a torrent
			if (!filename.matches(".+\\.torrent")){
				System.out.println("ERROR: File extension is not a torrent!");
				return null;
			}
			torrent = new FileInputStream(file);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			System.out.println("ERROR: Torrent file not found!");
			e1.printStackTrace();
			return null;
		} finally {
			
		}
		
		//ByteArrayOutputStream b = new ByteArrayOutputStream();
		byte[] torBytes = new byte[fileLen];
		try {
			if (torrent.read(torBytes,0,fileLen) == 0){
				torrent.close();
				return null;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("ERROR: I/O");
			e.printStackTrace();
			torrent.close();
			return null;
		}
		
		
		torrent.close();
		return torBytes;
		
	}
	
	public static void main(String[] args) throws Exception {
		
		// Exiting gracefully if arguments are invalid.
		if (args.length != 2){
			System.out.println("USAGE: java -cp . RUBTClient <TORRENT> <FILENAME>");
			return;
		}		

		generateID();

		String userInputString = null;
		userInputString  = args[0];
		System.out.println("==ARGUMENTS==\n" + args[0] + "\n" + args[1]);
		System.out.println("Attempting to read file \"" + userInputString + "\"...");
		byte[] torrentBytes = null;

		// read the torrent file
		try {
			torrentBytes = readTorrent(userInputString);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			System.out.println("Error reading torrent.");
			e1.printStackTrace();
			return;
		}

		// parse the torrent file info
		TorrentInfo tor = null;
		try {
			tor = new TorrentInfo(torrentBytes);
		} catch (BencodingException e) {
			System.out.println("Error deBeencoding torrent!!!");
			e.printStackTrace();
			return;
		}

		// use torrent file info to contact the tracker
		Tracker tracker = new Tracker(tor, PEER_ID);
		try{
			tracker.readTrackerResponse();
		}catch (IOException e){
		System.out.println(e.getMessage());
			System.out.println("Error reading from tracker URL.");
			return;
		}
		
		
		
		System.out.println("\n===============DEBUG===============\n");
		System.out.println("Tracker response: " + new String(tracker.getLastResponse(),"UTF-8"));
		System.out.println("\n===============END DEBUG===============\n");

		String peer_ip[] = tracker.getPeer(COMPARE_IP);
		Peer peer = new Peer(tor, PEER_ID, peer_ip[0]);
		
		if (peer.handshake()){
			
			
			
			//Bypass the bitfield in the pipeline
			peer.receiveMessage();
			
			//Send interested message
			peer.sendMessage(2);
			
			byte[] receipt = peer.receiveMessage();
			
			
			//send interest message
			//receive unchoke
			//pull pieces			
			BufferedOutputStream fileOutput = new BufferedOutputStream(new FileOutputStream(args[1]));
		
			byte[] piece = null;
		
			//Do not write header to block!
			int msgsSent = 0;
			while ((piece = peer.getPiece()) != null){
				fileOutput.write(piece);
				msgsSent++;
				System.out.println("Get piece req's sent: " + msgsSent);
				
			}
			
			
			fileOutput.flush();
			fileOutput.close();
					
			
			
		} else throw new Exception("ERROR: Bad Handshake!");
		
		

		//Output peer pieces to file
		peer.closeConnection();
	}
}
