import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.*;
import java.security.MessageDigest;

public class Peer {
	
	private TorrentInfo tor_info;
	private final String PEER_ID;
	private final String PEER_IP;
	private Socket peerSocket;
	private int BLOCK_LENGTH = 16384; // Typical block size
	private DataOutputStream toPeer;
	private BufferedInputStream fromPeer;
	private int BLOCKS_PER_PIECE;
	private int NUMBER_PIECES_IN_FILE;
	
	/*WILL BE TRUE if:
	 *The piece size divided by the block size has a remainder >0.
	 *RAMIFICATIONS: The last block downloaded will be a size other than 16384.
	 */
	private boolean NONDIV_BLOCKS;

	public Peer(TorrentInfo t, String pid, String pip) {
		tor_info = t;
		PEER_ID = pid;
		PEER_IP = pip;
		NUMBER_PIECES_IN_FILE = tor_info.piece_length;
		BLOCKS_PER_PIECE = (NUMBER_PIECES_IN_FILE % BLOCK_LENGTH != 0) ? NUMBER_PIECES_IN_FILE / BLOCK_LENGTH + 1 : NUMBER_PIECES_IN_FILE / BLOCK_LENGTH;
		//
		NONDIV_BLOCKS = NUMBER_PIECES_IN_FILE % BLOCK_LENGTH != 0;
	
	}
	
	public boolean handshake() throws Exception{
		String[] ipParts = PEER_IP.split(":");
		
		System.out.println("\n\nIP Parts: " + ipParts[0] + "   " + ipParts[1] + "\n\n");
		
		peerSocket = new Socket(ipParts[0], Integer.parseInt(ipParts[1]));
		
		String protocol = "BitTorrent protocol";
		
		byte[] bytes = new byte[49 + protocol.length()];
		
		byte[] protocolBytes = protocol.getBytes();
		
		int protoLength = protocolBytes.length;

		int i = 0;
		int count;

		//pstrlen
		bytes[0] = 0x13;

		//pstr
		for (i = 1; i <= protoLength;i++){
			bytes[i] = protocolBytes[i-1];
		}

		//reserved

		for (count = 0; count < 8; count++){
			bytes[i + count] = (byte)0;
		}
		i+=count;
		
		//At this moment, i is at the very end of the 'reserved' block (00000000X)
		//info_hash
		byte[] infoHashBytes = tor_info.info_hash.array();
		for (count = 0; count < infoHashBytes.length; count++){
			bytes[i + count] = infoHashBytes[count];
		}
		i+=count;
		
		//peer_id
		byte[] peerIDBytes = PEER_ID.getBytes("UTF-8");
		for (count = 0; count < peerIDBytes.length; count++){
			bytes[i + count] = peerIDBytes[count];
		}
		i+=count;


		toPeer = new DataOutputStream(peerSocket.getOutputStream());
		fromPeer = new BufferedInputStream(peerSocket.getInputStream());
		ByteArrayOutputStream temp_response = new ByteArrayOutputStream();

		toPeer.write(bytes);
		
		byte[] buf = new byte[1];

		while (bytes.length != temp_response.size() && fromPeer.read(buf) != -1) // read the response
		{
			temp_response.write(buf);
		}

		byte[] resp_handshake = temp_response.toByteArray();

		for(int in = bytes.length - 21; in > bytes.length - 41; in--)
			if (bytes[in] != resp_handshake[in])
				return false;

		return true;
	}

	/**  Function will compute the SHA-1 hash of a piece and compare it to the SHA-1
	  *  from the meta-info torrent file
	  *
	  *  @param piece The byte array of the piece which was downloaded from the peer
	  *  @param index The index number of the piece.
	  *  @return True if the piece matches the SHA-1 hash from the meta-info torrent file otherwise false.
	  */
	public boolean verifySHA(byte[] piece, int index)
	{
		MessageDigest digest = null;
		try
		{
			digest = MessageDigest.getInstance("SHA");
		}
		catch(Exception e)
		{
			System.out.println("Bad SHA algorithm");
			return false;
		}
		byte[] hash = digest.digest(piece);
		byte[] info_hash = tor_info.piece_hashes[index].array();
		for(int i = 0; i < hash.length; i++)
			if (hash[i] != info_hash[i])
				return false;
		return true;
	}

	/**
	  *
	  * @param pidx The piece index.
	  * @param bidx The block index.
	  */
	public boolean sendMessage(int msgType, int pidx, int bidx) throws Exception{
		byte[] message = null;
		
		switch (msgType) {
			case 1000: //keep-alive <length=0>
			
				break;
			case 0: //choke <length=1><MID=0>
			
				break;
			case 1: //unchoke <length=1><MID=1>
			
				break;
			case 2: //interested <length=1><MID=2>
				message = new byte[5];
				message[3] = 0x1;
				message[4] = 0x2;
				break;
			case 3:	//uninterested <length=1><MID=3>
			
				break;
			case 4: 
					//have <length=5><MID=4><payload>
					//The payload is a zero-based index of the piece
					//that has just been downloaded and verified.
				break;
			case 6: 
					//request <length=13><MID=6><payload>
					message = new byte[17];
					int left = (bidx + 1 == BLOCKS_PER_PIECE) ?
									(pidx + 1 == tor_info.piece_hashes.length) ? 
											(tor_info.file_length - NUMBER_PIECES_IN_FILE * pidx) - (BLOCK_LENGTH * (bidx + 1))
											: NUMBER_PIECES_IN_FILE - (BLOCK_LENGTH * (bidx + 1))
									: BLOCK_LENGTH;
					System.out.println("BYTES LEFT: " + left);
					
					//If asking for more data than available, don't send msg
					if (left <= 0)
						return false;
						
					int msgSize = (left >= BLOCK_LENGTH) ? BLOCK_LENGTH : left;
					message[0]=0x0;
					message[1]=0x0;
					message[2]=0x1;
					message[3]=0x3;
					message[4]=0x6;
					//Index
					ByteBuffer a = ByteBuffer.allocate(4);
					a.putInt(pidx);
					byte[] index = a.array();
					message[5]= index[0];
					message[6]= index[1];
					message[7]= index[2];
					message[8]= index[3];
					// Begin
					a = ByteBuffer.allocate(4);
					a.putInt(bidx * BLOCK_LENGTH);
					byte[] begin = a.array();	
					message[9]= begin[0];
					message[10]= begin[1];
					message[11]= begin[2];
					message[12]= begin[3];
					//Length
					a = ByteBuffer.allocate(4);
					a.putInt(msgSize);
					index = a.array();					
					message[13]= index[0];
					message[14]= index[1];
					message[15]= index[2];
					message[16]= index[3];

					for(byte b : message)
						System.out.print(b+" ");
					System.out.println();
				break;
			case 7: 
					//piece <length=9+X><MID=7>
				break;
			default:
				throw new Exception("Illegal message type!");
		}
		
		toPeer.write(message);
		return true;
	
	}
	
	public byte[] receiveMessage() throws Exception {
		
		ByteArrayOutputStream temp_response = new ByteArrayOutputStream();
		
		byte[] lengthBuffer = new byte[4];
		
		fromPeer.read(lengthBuffer);
		ByteBuffer wrap = ByteBuffer.wrap(lengthBuffer);
		int length = wrap.getInt();
		
		byte[] buf = new byte[1];
		
		
		
		

		while (temp_response.size() != length && fromPeer.read(buf) != -1) // read the response
		{
			temp_response.write(buf);
		}
		
		System.out.println("====CLIENT RECEIVED MESSAGE====");
		System.out.println("MESSAGE LENGTH: " + temp_response.toByteArray().length);
		if (temp_response.toByteArray().length > 4)
			System.out.println("MESSAGE ID: " + temp_response.toByteArray()[0]);
		
		
		/*
		for (byte b : temp_response.toByteArray())
			System.out.print(b+" ");
	
		*/
		
		System.out.println();
		//System.out.println("CLIENT RECEIVED MESSAGE: " + new String(temp_response.toByteArray(),"UTF-8"));

		return temp_response.toByteArray();
	}

	//In future, can demand explicit pieces
	public byte[] getPiece(int piece_index) throws Exception {
		byte[] message = null;

		for(int block_index = 0; block_index < BLOCKS_PER_PIECE; block_index++)
		{
			// last piece recalculate the number of blocks
			if (piece_index + 1 == NUMBER_PIECES_IN_FILE)
			{
				int temp = tor_info.file_length - (NUMBER_PIECES_IN_FILE - 1) * tor_info.piece_length;
				BLOCKS_PER_PIECE = (NUMBER_PIECES_IN_FILE % BLOCK_LENGTH != 0) ? NUMBER_PIECES_IN_FILE / BLOCK_LENGTH + 1 : NUMBER_PIECES_IN_FILE / BLOCK_LENGTH;
			}
			if (sendMessage(6, piece_index, block_index)){
				message = receiveMessage();
				
				System.out.println();
				byte[] block = new byte[message.length - 9];
				System.arraycopy(message,9,block, 0, message.length - 9);
				
				//verifyHash(block, );
			}
		}
		return null;
	}
	
	public void closeConnection() throws Exception {
		
		toPeer.close();
		fromPeer.close();
		peerSocket.close();
	}
}
