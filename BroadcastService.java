import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.HashMap;
import java.util.Map;

import java.lang.Integer;
import java.util.concurrent.atomic.AtomicInteger;


public class BroadcastService {

    private int nodeAmount = 0; // total nodes amount
    private String minId = "";
    private Map<String, String> idAddressMap = new HashMap<>();   // id-address map
    private Map<String, Integer> idPortMap = new HashMap<>();     // id-port map

    private String id;      // my id
    private String address; // my address;
    private Integer port;   // my port

    // original network
    private ArrayList<String> neighbors = new ArrayList<>();    // neighbors

    // broadcast network
    private ArrayList<String> broadcastNeighbors = new ArrayList<>();   // broadcast tree neighbors
    private String father = "";
    private ArrayList<String> children = new ArrayList<>();
    private int replyCount = 0;
    private int endCount = 0;

    private ServerSocket serverSocket;  // server
    private boolean toEndServer = false;
    private HashMap<String, String> srcCarrier = new HashMap<>();//To store src and carrier;
    private HashMap<String, Integer> srcReply = new HashMap<>();//To store src and reply count

    enum BroadcastStateEnum {
        DISABLED, IDLE, BUSY
    }

    private volatile BroadcastStateEnum broadcastState = BroadcastStateEnum.DISABLED;

    enum MessageType {
        BUILD_NETWORK, BUILD_NETWORK_END, BUILD_NETWORK_REPLY, BROADCAST, BROADCAST_END, BROADCAST_REPLY
    }

    private ArrayList<String> messageRecords = new ArrayList<>();


    public BroadcastService(String id, String configFile) {
        this.id = id;
        parseConfig(configFile);
        address = idAddressMap.get(id);
        port = idPortMap.get(id);
    }

    // parse config file
    private void parseConfig(String fileName) {
        String line;
        String step = "READ_NODE_AMOUNT";
        int counter = 0;
        try {
            BufferedReader br = new BufferedReader(new FileReader(fileName));
            String[] wordList;
            while ((line = br.readLine()) != null) {
                // remove the blank at begin and end
                line = line.trim();
                // ignore blank line
                if (line.length() == 0) continue;
                // ignore invalid line
                try {
                    Integer.parseInt(line.substring(0, 1));
                } catch (NumberFormatException e) {
                    continue;
                }
                // parse line
                switch (step) {
                    case "READ_NODE_AMOUNT":
                        // line format:node_amount
                        nodeAmount = Integer.parseInt(line);
                        step = "READ_NODE_MAP";
                        break;
                    case "READ_NODE_MAP":
                        // line format:id address port
                        wordList = line.split(" ");
                        if (minId.equals("") || Integer.parseInt(wordList[0]) < Integer.parseInt(minId)) {
                            minId = wordList[0];
                        }
                        idAddressMap.put(wordList[0], wordList[1]);
                        idPortMap.put(wordList[0], Integer.parseInt(wordList[2]));

                        counter++;
                        if (counter == nodeAmount) {
                            counter = 0;
                            step = "READ_NETWORK_INFO";
                        }
                        break;
                    case "READ_NETWORK_INFO":
                        // line format:id id1 id2 ...
                        wordList = line.split(" ");
                        if (wordList[0].equals(id)) {
                            for (int i = 1; i < wordList.length; i++) {
                                neighbors.add(wordList[i]);
                            }
                        }
                        break;
                }
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void parseConfig2(String fileName) {
        String line;
        String step = "READ_NODE_AMOUNT";
        int counter = 0;
        try {
            BufferedReader bf = new BufferedReader(new FileReader(fileName));
            String[] wordList = {};
            while ((line = bf.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0) continue;
                if (line.charAt(0) == '#') continue;
                switch (step) {
                    case "READ_NODE_AMOUNT":
                        nodeAmount = Integer.parseInt(line);
                        step = "READ_NETWORK_INFO";
                        break;
                    case "READ_NETWORK_INFO":
                        wordList = line.split("\\s+");
                        if (address.equals(wordList[0] + ".utdallas.edu")) {
                            idPortMap.put(id, Integer.parseInt(wordList[1]));
                            for (int i = 2; i < wordList.length; i++) {
                                neighbors.add(wordList[i]);
                            }
                        }
                        break;
                }
            }
            bf.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // init socket server
    private void initServer() {
        // start a background thread to listen request
        Thread serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    serverSocket = new ServerSocket(port);
                    while (!toEndServer) {
                        try {
                            // Listens for a connection to be made to this socket and accepts it
                            Socket sock = serverSocket.accept();    // this method blocks until a connection is made
                            // read message from other node
                            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                            String message = bufferedReader.readLine();
                            receiveMessage(message);
                            bufferedReader.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        serverThread.start();
    }

    // end socket server
    private void endServer() {
        toEndServer = true;
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(id+":server at " + address + " has been shut down.");
    }


    public static void main(String[] args) throws InterruptedException {
        if (args.length != 3) {
            System.err.println("Usage: java BroadcastService <id> <configfile> <outputdir>");
            System.exit(1);
        }
        String id = args[0];
        String configFile = args[1];
        String outputDir = args[2];

        BroadcastService broadcastService = new BroadcastService(id, configFile);
        broadcastService.initServer();
        Thread.sleep(1000); // wait for all servers starting
        broadcastService.buildBroadcastNetwork();
        Thread.sleep(2000); // wait for network build done
        int counter = 0;
        int broadcastTimes = 3;
        while (counter < broadcastTimes) {
            broadcastService.sendBroadcastMessage();
            counter++;
            Thread.sleep(1000);
        }
        broadcastService.sendShutdownRequest();
        broadcastService.writeMessagesToFile(outputDir);    // write sent&received broadcast messageRecords to file
    }

    private void buildBroadcastNetwork() {
        if (id.equals(minId)) {
            // message format:  type:sourceId:carrierId:transmissionPath:content
            String message = String.format("%s::%s::%s::%s::%s", MessageType.BUILD_NETWORK, id, " ", " ", " ");
            for (String neighbor : neighbors) {
                sendMessage(neighbor, message);
            }
        }
    }

    // send message to a target
    private void sendMessage(String target, String message) {
        try {
            //Create a client socket and connect to target's server
            Socket clientSocket = new Socket(idAddressMap.get(target), idPortMap.get(target));
            PrintWriter writer = new PrintWriter(clientSocket.getOutputStream());
            writer.println(message);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // action after receiving a message
    // message format:  type:sourceId:carrierId:transmissionPath:content
    private void receiveMessage(String message) {
        //messageRecords.add(message);
        //System.out.println(message);
        MessageType type = MessageType.valueOf(message.split("::")[0]);
        String source = message.split("::")[1];
        String carrier = message.split("::")[2];
        String path = message.split("::")[3];
        String content = message.split("::")[4];


        // TODO
        switch (type) {
            case BUILD_NETWORK:
                if (father.equals("")) {    // join tree
                    father = source;
                    broadcastNeighbors.add(source);
                    if (replyCount == neighbors.size() - 1) {    // no other neighbors, directly reply "success"
                        String newMessage = String.format("%s::%s::%s::%s::%s", MessageType.BUILD_NETWORK_REPLY, id, " ", " ", "success");
                        sendMessage(father, newMessage);
                    }
                    for (String neighbor : neighbors) { // continue broadcast
                        if (neighbor.equals(source))
                            continue;
                        String newMessage = String.format("%s::%s::%s::%s::%s", MessageType.BUILD_NETWORK, id, " ", " ", " ");
                        sendMessage(neighbor, newMessage);
                    }

                } else {    // has been in part of tree
                    String newMessage = String.format("%s::%s::%s::%s::%s", MessageType.BUILD_NETWORK_REPLY, id, " ", " ", "fail");
                    sendMessage(source, newMessage);
                }
                break;
            case BUILD_NETWORK_REPLY:
                if (content.equals("success")) {
                    children.add(source);
                    broadcastNeighbors.add(source);
                }
                replyCount++;
                if (!id.equals(minId) && replyCount == neighbors.size() - 1) {    // not root node and subtree finished, reply to father
                    String newMessage = String.format("%s::%s::%s::%s::%s", MessageType.BUILD_NETWORK_REPLY, id, " ", " ", "success");
                    sendMessage(father, newMessage);
                } else if (id.equals(minId) && replyCount == neighbors.size()) {  // root node and whole tree finished
                    for (String broadcastNeighbor : broadcastNeighbors) {
                        String newMessage = String.format("%s::%s::%s::%s::%s", MessageType.BUILD_NETWORK_END, id, " ", " ", "success");
                        sendMessage(broadcastNeighbor, newMessage);
                    }
                    broadcastState = BroadcastStateEnum.IDLE;
                }
                break;
            case BUILD_NETWORK_END:
                printBroadcastNeighbors();
                for (String broadcastNeighbor : broadcastNeighbors) {
                    if (broadcastNeighbor.equals(source))
                        continue;
                    String newMessage = String.format("%s::%s::%s::%s::%s", MessageType.BUILD_NETWORK_END, id, " ", " ", "success");
                    sendMessage(broadcastNeighbor, newMessage);
                }
                broadcastState = BroadcastStateEnum.IDLE;
                break;
            case BROADCAST:
                messageRecords.add(new String(id + " received broadcast message from " + source + ":" + content));
                System.out.println(new String(id + " received broadcast message from " + source + ":" + content));
                srcCarrier.put(source, carrier);
                srcReply.put(source, 0);

                //Not leave node
                if (broadcastNeighbors.size() > 1) {
                    for (String broadcastNeighbor : broadcastNeighbors) {
                        if (broadcastNeighbor.equals(carrier))
                            continue;
                        // message format:  type:sourceId:carrierId:transmissionPath:content
                        String newMessage = String.format("%s::%s::%s::%s::%s", MessageType.BROADCAST, source, id, " ", content);
                        sendMessage(broadcastNeighbor, newMessage);
                    }

                } else {
                    String newMessage = String.format("%s::%s::%s::%s::%s", MessageType.BROADCAST_REPLY, source, id, carrier, " ");
                    sendMessage(carrier, newMessage);

                }
                break;
            case BROADCAST_REPLY:
                //Update the reply count of corresponding source;
                srcReply.put(source, srcReply.get(source) + 1);
                if (id.equals(source)) {
                    if (srcReply.get(source) == broadcastNeighbors.size()) {
                        messageRecords.add(new String(id + " received all broadcast reply."));
                        System.out.println(new String(id + " received all broadcast reply."));
                        broadcastState = BroadcastStateEnum.IDLE;
                        srcReply.put(source, 0);
                    }
                } else if (srcReply.get(source) == broadcastNeighbors.size() - 1) {
                    String newMessage = String.format("%s::%s::%s::%s::%s", MessageType.BROADCAST_REPLY, source, id, carrier, "broadcasreply");
                    sendMessage(srcCarrier.get(source), newMessage);
                    srcReply.put(source, 0);
                }
                break;
            case BROADCAST_END:
                if(id.equals(minId)){
                    endCount++;
                    if(endCount==nodeAmount){
                        for (String target : broadcastNeighbors) {
                            String newMessage = String.format("%s::%s::%s::%s::%s", MessageType.BROADCAST_END, id, id, " ", " ");
                            sendMessage(target, newMessage);
                        }
                        System.out.println(new String(id + " sent broadcast shutdown message."));
                        broadcastState = BroadcastStateEnum.DISABLED;
                        endServer();
                    }
                }else{
                    // message format:  type:sourceId:carrierId:transmissionPath:content
                    for (String target : broadcastNeighbors) {
                        if (target.equals(carrier))
                            continue;
                        String newMessage = String.format("%s::%s::%s::%s::%s", MessageType.BROADCAST_END, source, id, " ", " ");
                        sendMessage(target, newMessage);
                    }
                    System.out.println(new String(id + " received broadcast shutdown message from "+source));
                    broadcastState = BroadcastStateEnum.DISABLED;
                    endServer();
                }
                break;
        }
    }

    private void sendBroadcastMessage() throws InterruptedException {
        // no broadcast message when spanning tree is not built
        while (broadcastState == BroadcastStateEnum.DISABLED);
        // no concurrent broadcast messages from same node
        while (broadcastState == BroadcastStateEnum.BUSY);
        // message format:  type:sourceId:carrierId:transmissionPath:content
        srcReply.put(id, 0);
        String type = MessageType.BROADCAST.toString();
        String message = String.format("%s::%s::%s::%s::%s", type, id, id, id, dateMessage());
        for (String target : broadcastNeighbors) {
            sendMessage(target, message);
        }
        messageRecords.add(new String(id + " sends broadcast message:" + message));
        System.out.println(new String(id + " sends broadcast message:" + message));
        broadcastState = BroadcastStateEnum.BUSY;
    }

    private void sendShutdownRequest() {
        while (broadcastState == BroadcastStateEnum.BUSY);
        // message format:  type:sourceId:carrierId:transmissionPath:content
        if(id.equals(minId)){
            endCount++;
        }else{
            String message = String.format("%s::%s::%s::%s::%s", MessageType.BROADCAST_END, id, id, " ", " ");
            sendMessage(minId, message);
        }
    }


    // produce a unique message
    private String dateMessage() {
        String pattern = "yyyy-MM-dd HH:mm:ss";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        String dateAndTime = simpleDateFormat.format(new Date());
        return "Hello! Now is " + dateAndTime;
    }


    void printBroadcastNeighbors() {
        System.out.println(id + ":my neighbors are " + String.join(",", broadcastNeighbors));
    }

    private void writeTestFile() {
        try {
            BufferedWriter bf = new BufferedWriter(new FileWriter("./" + address + "SPT"));
            for (String s : broadcastNeighbors) {
                bf.write(s + '\n');
            }
            bf.close();
        } catch (IOException e) {

        }
    }

    private void writeMessagesToFile(String outputDir) throws InterruptedException {
        while (broadcastState != BroadcastStateEnum.DISABLED) {
            Thread.sleep(1000);
        }
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outputDir + "/" + address+"_"+port));
            for (String message : messageRecords) {
                bufferedWriter.write(message + "\n");
            }
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(id + ":broadcast log has been written to file.");
    }

}
