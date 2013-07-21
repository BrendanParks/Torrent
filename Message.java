import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.*;

public class Message {
	
	public static int decodeMessage(byte[] message,int type) throws Exception {
		
		
		switch (type) {
			case 0: //keep-alive <length=0>
			
				break;
			case 1: //choke <length=1><MID=0>
			
				break;
			case 2: //unchoke <length=1><MID=1>
			
				break;
			case 3: //interested <length=1><MID=2>
			
				break;
			case 4:	//uninterested <length=1><MID=3>
			
				break;
			case 5: 
				
			
					//have <length=5><MID=4><payload>
					//The payload is a zero-based index of the piece
					//that has just been downloaded and verified.
				break;
			case 6: 
			
					//request <length=13><MID=6><payload>
					/*The payload is as follows:
						* <index><begin><length> 
						* Where <index> is an integer specifying the 
						* zero-based piece index, <begin> is an 
						* integer specifying the zero-based byte 
						* offset within the piece, and <length> 
						* is the integer specifying the requested 
						* length.<length> is typically 2^14 (16384) 
						* bytes. A smaller piece should only be used 
						* if the piece length is not divisible by 16384.
						*  A peer may close the connection if a block 
						* larger than 2^14 bytes is requested.
					*/
				break;
			case 7: 
			
					//piece <length=9+X><MID=7>
					/*The payload is as follows:
					 *	<index><begin><block> 
					 *	Where <index> is an integer specifying the
					 * zero-based piece index, <begin> is an integer
					 * specifying the zero-based byte offset within
					 * the piece, and <block> which is a block of data,
					 * and is a subset of the piece specified by<index>.
					 */
				break;
			default:
				throw new Exception("Illegal message type!");
		}	



		return 1;
	}
	
	
	public void closeConnection()
	{
	}
	
}
