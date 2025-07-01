import java.util.*;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;

public class Receiver implements Runnable{
    private int tcpPort = 6000;         // This is for TCP control/ACKs
    private int rbudpPort = 5000;       // This is for UDP data reception

    // TCP communication
    private ServerSocket tcpServerSocket;
    private Socket tcpSocket;           // sender's TCP socket
    private ObjectInputStream tcpInput;
    private ObjectOutputStream tcpOutput;
    private char protocol;

    // UDP communication
    private DatagramSocket udpSocket;

    // Data to get from sender
    int totalPackets;                   // total number of packets to expect
    private int packetSize;
    private String saveFileName = "";
;
    private String user = "";

    // Receiver state
    private int receivedCnt = 0;

    // Packet buffers
    private final Map<Integer, byte[]> receivedPackets = Collections.synchronizedMap(new HashMap<>());
    private final Set<Integer> receivedSequenceNumbers = Collections.synchronizedSet(new HashSet<>());

    private Main main;

    public Receiver(String user, int port, Main main) {
        this.user = user;
        this.main = main;
        // this.rbudpPort = port;
        // this.tcpPort = port + 1000; // TCP port is UDP port + 1000

        System.out.println("[Receiver] Initialized:");
        System.out.println("User: " + user);
        System.out.println("[Receiver] Starting...");
    }

    public void run() {
        tcpControlHandler();
        closeTCP();
        saveFile(protocol);
    }

    private void tcpControlHandler() {
        try {
            totalPackets = 0;  
            tcpServerSocket = new ServerSocket(tcpPort);
            System.out.println("[TCP] Listening on port " + tcpPort);
            tcpSocket = tcpServerSocket.accept();
            System.out.println("[TCP] Connected to sender.");

            tcpOutput = new ObjectOutputStream(tcpSocket.getOutputStream());
            tcpInput = new ObjectInputStream(tcpSocket.getInputStream());
            System.out.println("port number: " + tcpPort);
            saveFileName = tcpInput.readUTF();

            // Confirm the protocol used
            protocol = tcpInput.readChar();
            System.out.println("[TCP] Protocol: " + protocol);

            if (protocol == 'T') {
                handleTCPFileTransfer();
            } else if (protocol == 'U') {
                packetSize = tcpInput.readInt();  // Read the packet size
                System.out.println("[TCP] Packet size: " + packetSize);
                handleRBUDPFileTransfer();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleTCPFileTransfer() {
        try {
            long fileSize = tcpInput.readLong();  // Sender sends file size
            System.out.println("[TCP] Expected file size: " + fileSize + " bytes");
        
            File directory = new File("./FILES_RECEIVED_" + user);
            if (!directory.exists()) {
                directory.mkdir();
            }
            File receivedFile = new File(directory, saveFileName);
            FileOutputStream fileOutput = new FileOutputStream(receivedFile);
            // Add the missing code to read the file data
            byte[] buffer = new byte[packetSize + 4];
            int bytesRead;
            long totalReceived = 0;
            long lastPrintedPercent = 0;
            // Read data in chunks until we've received the entire file
            while (totalReceived < fileSize && (bytesRead = tcpInput.read(buffer)) != -1) {
                fileOutput.write(buffer, 0, bytesRead);
                totalReceived += bytesRead;
                // Calculate percent complete to print progress
                long percent = (totalReceived * 100) / fileSize;
                if (percent != lastPrintedPercent) {
                    printProgress(totalReceived, fileSize, "TCP");
                    lastPrintedPercent = percent;
                }
            }

            fileOutput.close();
            System.out.println("[TCP] File transfer complete. Total received: " + totalReceived + " bytes"); 
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleRBUDPFileTransfer() {
        try {
            totalPackets = tcpInput.readInt();
            System.out.println("[TCP] Total packets expected: " + totalPackets);
    
            udpSocket = new DatagramSocket(rbudpPort);
            System.out.println("[UDP] Listening on port " + rbudpPort);
    
            boolean done = false;
    
            while (!done) {
                receiveUDPPacketsForBurst();  // Listen for UDP packets in this round
    
                @SuppressWarnings("unchecked")
                List<Integer> allPacketSeqs = (List<Integer>) tcpInput.readObject();
    
                if (allPacketSeqs.isEmpty()) {
                    System.out.println("[TCP] Transfer complete.");
                    done = true;
                    break;
                }
    
                List<Integer> missingPackets = findMissingPackets(allPacketSeqs);
                tcpOutput.writeObject(missingPackets);
                tcpOutput.flush();
                System.out.println("[TCP] Sent NACK list: " + missingPackets);
            }
    
            udpSocket.close();
            System.out.println("[UDP] UDP reception complete.");
    
        } catch (Exception e) {
            System.out.println("[RBUDP] Error during RBUDP transfer: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void receiveUDPPacketsForBurst() throws IOException {
        long endTime = System.currentTimeMillis() + 2000; // Listen for 2 seconds
        udpSocket.setSoTimeout(500);
    
        while (System.currentTimeMillis() < endTime) {
            byte[] buffer = new byte[packetSize + 4];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
    
            try {
                udpSocket.receive(packet);
            } catch (SocketTimeoutException e) {
                // No packet received in timeout, move on
                continue;
            }
    
            ByteBuffer byteBuffer = ByteBuffer.wrap(packet.getData());
            int seqNum = byteBuffer.getInt();
    
            byte[] dataPayload = new byte[packet.getLength() - 4];
            byteBuffer.get(dataPayload);
    
            if (!receivedSequenceNumbers.contains(seqNum)) {
                storePacket(seqNum, dataPayload);
                System.out.println("[UDP] Received packet SeqNum: " + seqNum);
            } else {
                System.out.println("[UDP] Discarded duplicate packet SeqNum: " + seqNum);
            }
        }
    }
    
    private List<Integer> findMissingPackets(List<Integer> allPacketSeqs) {
        List<Integer> missingPackets = new ArrayList<>();
        for (int seq : allPacketSeqs) {
            if (!receivedSequenceNumbers.contains(seq)) {
                missingPackets.add(seq);
            }
        }
        return missingPackets;
    }

    private void storePacket(int seqNum, byte[] dataPayload) {
        if (!receivedSequenceNumbers.contains(seqNum)) {
            receivedSequenceNumbers.add(seqNum);
            receivedPackets.put(seqNum, dataPayload);
        }

        receivedCnt++;
        printProgress(receivedCnt, totalPackets, "UDP");
    }
    
    private void printProgress(long current, long total, String label) {
        int percent = (int) ((current * 100) / total);
        int numHashes = (int) ((percent / 100.0) * 50);  // 50-char progress bar
    
        StringBuilder progressBar = new StringBuilder();
        for (int i = 0; i < numHashes; i++) {
            progressBar.append("#");
        }
        for (int i = numHashes; i < 50; i++) {
            progressBar.append("-");
        }

        if (percent != 100) {
            main.updateProgressBar(percent, true);
        } else {
            main.updateProgressBar(100, false);
        }
    
        System.out.print("\r[" + label + "] [" + progressBar.toString() + "] " + percent + "%");
        System.out.flush();
    }

    private void saveFile(char protocol) {
        try {
            File directory = new File("./FILES_RECEIVED_" + user);
            if (!directory.exists()) {
                directory.mkdir();
            }

            File receivedFile = new File(directory, saveFileName);

            if (protocol == 'T') {
                // TCP file is already saved during handleTCPFileTransfer()
                System.out.println("[SAVE] TCP file was saved during transfer: " + receivedFile.getAbsolutePath());
                return;  // No need to rewrite it
            }

            // UDP File Reassembly
            FileOutputStream fos = new FileOutputStream(receivedFile);
            for (int i = 0; i < totalPackets; i++) {
                byte[] data = receivedPackets.get(i);
                if (data != null) {
                    fos.write(data);
                } else {
                    System.out.println("[SAVE] WARNING: Missing packet " + i + "!");
                }
            }
            fos.close();
            System.out.println("[SAVE] UDP file successfully saved: " + receivedFile.getAbsolutePath());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void closeTCP() {
        try {
            if (tcpInput != null) tcpInput.close();
            if (tcpOutput != null) tcpOutput.close();
            if (tcpSocket != null) tcpSocket.close();
            if (tcpServerSocket != null) tcpServerSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
