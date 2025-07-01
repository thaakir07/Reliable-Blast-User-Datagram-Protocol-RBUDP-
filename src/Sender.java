//Gets file from a client and sends it to the receiver
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implements the sender portion of the Reliable Blast UDP (RBUDP) protocol.
 * This class is responsible for reading a file, dividing it into packets,
 * sending the packets in bursts, and handling retransmissions.
 */
public class Sender {
    private static final int UDP_PORT = 5000;
    private static final int TCP_PORT = 6000;
    
    private static final int BURST_SIZE = 400; // Number of packets per burst
    
    private DatagramSocket udpSocket;
    private Socket tcpSocket;
    private ObjectOutputStream tcpOut;
    private ObjectInputStream tcpIn;
    private String receiverIP;
    private int packetSize = 1000;
    private File file;
    private Map<Integer, byte[]> packets = new HashMap<>();

    /**
     * Constructor for the Sender class.
     * 
     * @param receiverIP The IP address of the receiver
     * @param file The file to be sent
     */
    public Sender(String receiverIP, File file, int packetSize) {
        this.receiverIP = receiverIP;
        this.file = file;
        this.packetSize = packetSize;
    }

    /**
     * Method to send a file using RBUDP protocol.
     * The file is sent in bursts, with each burst being processed until all packets
     * in that burst are received before moving to the next burst.
     */
    public void sendFileUDP() {
        try {
            udpSocket = new DatagramSocket();
            System.out.println("receiver IP is " + receiverIP);
            tcpSocket = new Socket(receiverIP, TCP_PORT);
            tcpOut = new ObjectOutputStream(tcpSocket.getOutputStream());
            tcpIn = new ObjectInputStream(tcpSocket.getInputStream());
            tcpOut.writeUTF(file.getName());
            tcpOut.flush();
            tcpOut.writeChar('U');
            tcpOut.flush();
            tcpOut.writeInt(packetSize);
            tcpOut.flush();
            readFile();
            // Define burst limit
            List<Integer> allPacketSeqs = new ArrayList<>(packets.keySet());
            tcpOut.writeInt(packets.size()); // Send total packets count to receiver for error check
            tcpOut.flush();
            
            // Process packets in bursts
            for (int burstStart = 0; burstStart < allPacketSeqs.size(); burstStart += BURST_SIZE) {
                // Determine the end index of this burst
                int burstEnd = Math.min(burstStart + BURST_SIZE, allPacketSeqs.size());
                
                // Get the sequence numbers for this burst
                List<Integer> burstSeqs = allPacketSeqs.subList(burstStart, burstEnd);
                Set<Integer> remainingPackets = new HashSet<>(burstSeqs);
                
                System.out.println("Processing burst " + (burstStart/BURST_SIZE + 1) + " with " + burstSeqs.size() + " packets");
                
                // Continue until all packets in this burst are confirmed received
                // Limit retries to prevent infinite loop
                int retryCount = 0;
                while (!remainingPackets.isEmpty() && retryCount < 16) { 
                    sendBurst(remainingPackets);
                    sendSeqNumbers(remainingPackets);
                    List<Integer> missingPackets = getMissingPackets();
                    
                    // Update remaining packets to only those that were missed
                    remainingPackets = new HashSet<>(missingPackets);
                    if (!missingPackets.isEmpty()) {
                        System.out.println("Retransmitting " + missingPackets.size() + " lost packets...");
                        retryCount++;
                    } else {
                        break;
                    }
                }
                System.out.println("Burst " + (burstStart/BURST_SIZE + 1) + " complete");
            }
            System.out.println("File transfer complete from sender side.");

            // Send empty list to let receiver know we done
            tcpOut.writeObject(new ArrayList<Integer>());
            tcpOut.flush();

            // Close resources
            udpSocket.close();
            tcpOut.close();
            tcpIn.close();
            tcpSocket.close();

        } catch (Exception e) {
            System.out.println("Current port or IP unavaible, please try again");
            System.exit(0);
        }
    }

    /**
     * Reads the file into memory and divides it into packets.
     * Each packet is assigned a unique sequence number.
     */
    private void readFile() {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[packetSize];
            int seqNum = 0;

            while (true) {
                int bytesRead = fis.read(buffer);
                if (bytesRead == -1) {
                    break;
                }

                //We make a copy because we dont want our data overwritten
                byte[] data = Arrays.copyOf(buffer, bytesRead);
                packets.put(seqNum, data);
                seqNum++;
                System.out.println("[Sender] Read packet SeqNum: " + seqNum + " (Size: " + bytesRead + " bytes)");
            }
            System.out.println("File read and split into " + packets.size() + " packets.");
            


        } catch (IOException e) {
            e.printStackTrace();
        } 
    }

    /**
     * Sends a burst of packets over UDP.
     * 
     * @param packetSeqs The set of packet sequence numbers to send
     * @throws IOException If an I/O error occurs during sending
     */
    private void sendBurst(Set<Integer> packetSeqs) throws IOException {
        InetAddress receiverAddress = InetAddress.getByName(receiverIP);
        
        for (int seq : packetSeqs) {
            byte[] data = packets.get(seq);
            System.out.println("[Sender] Sending packet SeqNum: " + seq);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeInt(seq);
            dos.write(data);
            byte[] packetData = baos.toByteArray();
            DatagramPacket packet = new DatagramPacket(packetData, packetData.length, receiverAddress, UDP_PORT);
            udpSocket.send(packet);
            System.out.println("[DEBUG] UDP packet sent: size=" + packetData.length + 
                         " to=" + receiverAddress + ":" + UDP_PORT);
        }
        System.out.println("Burst of " + packetSeqs.size() + " packets sent.");
    }

    /**
     * Sends the sequence numbers of a set of packets over TCP.
     * 
     * @param packetSeqs The set of packet sequence numbers to send
     */
    private void sendSeqNumbers(Set<Integer> packetSeqs) {
        try {
            tcpOut.writeObject(new ArrayList<>(packetSeqs));
            tcpOut.flush();
            System.out.println("Sequence numbers sent over TCP.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the list of missing packets from the receiver.
     * 
     * @return A list of sequence numbers of missing packets
          * @throws ClassNotFoundException 
          */
    private List<Integer> getMissingPackets() throws ClassNotFoundException {
        try {
            @SuppressWarnings("unchecked")
            List<Integer> missingPackets = (List<Integer>) tcpIn.readObject();
            if (missingPackets.isEmpty()) {
                System.out.println("No missing packets reported.");
            } else {
                System.out.println("Received list of " + missingPackets.size() + " missing packets.");
            }
            return missingPackets;
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Implementation of TCP file transfer for comparison with RBUDP.
     * 
     * @param file The file to be sent
     */
    public void sendFileTCP() {
        try {
            Socket socket = new Socket(receiverIP, TCP_PORT);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream()); // Added input stream

            out.writeUTF(file.getName());
            out.flush();
    
            out.writeChar('T');
            out.flush();
            
            // Send file size first
            out.writeLong(file.length());
            out.flush();
            
            // Send file data
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[packetSize];
    
            int bytesRead;
            long totalSent = 0;
            
            while ((bytesRead = fis.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                totalSent += bytesRead;
                
                if (totalSent % (1024 * 1024) == 0) { // Every 1 MB
                    System.out.println("TCP transfer: " + (totalSent / (1024 * 1024)) + " MB sent");
                }
            }
            out.flush();

            System.out.println("\nTCP file transfer complete. Total sent: " + totalSent + " bytes");
    
            // **Wait for final acknowledgment from receiver**
            //String confirmation = in.readUTF(); // Added this line
            //System.out.println("[Sender] Received confirmation from receiver: " + confirmation);
    
            // Close resources
            fis.close();
            out.close();
            in.close(); // Close the new input stream
            socket.close();
    
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}
