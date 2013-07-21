/*  This class handles all interactions with the tracker
 *
 */
import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.*;
 public class Tracker
{
	TorrentInfo tor_info;
	private byte[] last_response;
	String PEER_ID;

	public Tracker(TorrentInfo t, String pid)
	{
		tor_info = t;
		last_response = null;
		PEER_ID = pid;
	}

	/**  Function takes in a binary array of bytes and converts it to a URL encoded string
	  *
	  *  @param toURL A byte array that will be URL encoded
	  *  @return A URL encoded string version of toURL
	  */
	public String byteArrayToURLString(byte toURL[])
	{
		// do not allow empty arrays
		if (toURL == null || toURL.length <=0)
			return null;
		
		String[] hexVals = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F"}; // all possible hex values
		String urlEncoded = "";
		byte current = 0x00;
		
		for(int i = 0; i < toURL.length; i++)
		{
			current = (byte)((byte)((byte)(toURL[i] & 0xF0) >>> 4) & 0x0F); // take the front half of the byte and shift it down
			urlEncoded += "%"+hexVals[(int)current];
			current = (byte)(toURL[i] & 0x0F); // grab the back half of the byte
			urlEncoded += hexVals[(int)current];
		}

		return urlEncoded;
	}

	/**  Function will contact the tracker defined by the information in the TorrentInfo object
	  *  and return the response that is recieved.
	  *
	  *  @param tor A TorrentInfo object created from a torrent file
	  *  @throws MalformedURLException
	  *  @throws IOException
	  */
	public void readTrackerResponse() throws IOException
	{
		// put together the parameters for the url object
		String file_and_params = tor_info.announce_url.getFile()+"?";
		file_and_params += "info_hash="+byteArrayToURLString(tor_info.info_hash.array()); // add the info hash
		file_and_params += "&PEER_ID="+byteArrayToURLString(PEER_ID.getBytes()); // add the peer id
		file_and_params += "&port=6883"; // add the port
		file_and_params += "&uploaded=0&downloaded=0"; // add uploaded and downloaded params
		file_and_params += "&left="+tor_info.file_length; // add how much more is needed for download

		URL url = new URL("http", tor_info.announce_url.getHost(), tor_info.announce_url.getPort(), file_and_params); // create the url

		System.out.println("Contacting tracker : " + url + "\n");
		try
		{
			// open an input stream from the url
			BufferedInputStream reader = new BufferedInputStream(((HttpURLConnection)url.openConnection()).getInputStream());
			ByteArrayOutputStream temp_response = new ByteArrayOutputStream();
			byte[] buf = new byte[1];

			while (reader.read(buf) != -1) // read the response
			{
				temp_response.write(buf);
			}
			reader.close(); //** close the reader **
			last_response = temp_response.toByteArray();
		}
		catch (MalformedURLException e)
		{
			System.out.println(e.getMessage());
		}
		catch (IOException e)
		{
			System.out.println(e.getMessage());
		}
	}

	/**  Function provided by instructors.
	  */
	public String[] decodeCompressedPeers(Map map)
	{
		ByteBuffer peers = (ByteBuffer)map.get(ByteBuffer.wrap("peers".getBytes()));
		ArrayList<String> peerURLs = new ArrayList<String>();
		try {
			while (true) {
				String ip = String.format("%d.%d.%d.%d",
					peers.get() & 0xff,
					peers.get() & 0xff,
					peers.get() & 0xff,
					peers.get() & 0xff);
				int port = peers.get() * 256 + peers.get();
				peerURLs.add(ip + ":" + port);
			}
		} catch (BufferUnderflowException e) {
			// done
		}
		return peerURLs.toArray(new String[peerURLs.size()]);
	}

	/**  Function returns the last reponse read from this tracker.
	  *
	  *  @return The last response from this tracker.
	  */
	public byte[] getLastResponse()
	{
		return last_response;
	}

	/**  Function finds searches through the trackers response to find the peer in the peer list which
	  *  matches match.
	  *
	  *  @param match The IP in the peer list that should be used
	  *  @return Returns the IP which matches match if it exists in the peer list
	  */
	public String[] getPeer(String[] match)
	{
		// test the object to make sure it is an instance of a map before attempting to bind it
		Object test_first = null;
		try
		{
			test_first = Bencoder2.decode(getLastResponse());
		}
		catch(BencodingException e)
		{
			System.out.println("Error deBencoding peer list!");
			return null;
		}
		if (!(test_first instanceof HashMap))
			System.out.println("Error invalid response response");

		Map<ByteBuffer, Object> responseMap = (Map<ByteBuffer, Object>)test_first;
		String[] peer_ip = new String[2];
		for(String peer : decodeCompressedPeers(responseMap)) // pull out the specified ip from the peer list
			if (peer.split(":")[0].equals(match[0]))
				peer_ip[0] = peer;
			else if (peer.split(":")[0].equals(match[1]))
								peer_ip[1] = peer;
		System.out.println("Found peer : " + peer_ip[0] + ", " + peer_ip[1]);
		
		return peer_ip;
	}
}
