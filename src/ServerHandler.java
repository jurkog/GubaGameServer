import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: JurkoG
 * Date: 1/6/14
 * Time: 7:32 PM
 * To change this template use File | Settings | File
 *
 * Templates.
 */
public class ServerHandler implements Runnable {
    ServerSocket serverSocket;
    public static DatagramSocket socket;
    public static ArrayList<ClientHandler> clientArray = new ArrayList<ClientHandler>();
    public static ArrayList<Item> mapItems = new ArrayList<Item>();

    public void run() {
        try {

            serverSocket = new ServerSocket(5000);
            mapItems.add(new Item("gun_glock_right", 19 << 4, 62 << 4));
            while (true) {
                Socket incomingSocket = serverSocket.accept();
                ClientHandler incomingClient = new ClientHandler(incomingSocket);
                clientArray.add(incomingClient);
                Thread listener = new Thread(incomingClient);
                listener.start();
            }

        } catch (Exception e) {e.printStackTrace();}
    }

    public class ClientHandler implements Runnable {
        // Network Related Variables
        BufferedReader in;
        PrintWriter out;
        Socket client;
        InetAddress IPAddress;

        // Player Related Variables
        int x, y, dir, port;
        String name, weapon;
        ArrayList<Item> inventory = new ArrayList<Item>();


        public ClientHandler(Socket socket) {
            try {
                this.client = socket;
                this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                this.out = new PrintWriter(socket.getOutputStream());

            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }

        public void run() {
            sendClientTCPMessage("send");
            for (Item item : mapItems) {
                sendClientTCPMessage("drop"+"/"+item.x+"/"+item.y+"/"+item.id);
            }

            String message;
            try {
                while ((message = in.readLine())!= null) {
                    message.toLowerCase().trim();
                    String[] parseMessage = message.split("/");
                    System.out.println(message);
                    if (parseMessage[0].equals("connect")) {
                        createPlayer(parseMessage[1], parseMessage[2], parseMessage[3], parseMessage[4], parseMessage[5]);
                        sendClientGlobalUserList();
                        informServerOfConnection();
                        sendClientTCPMessage("udpstart");
                        inventory.add(new Item("gun_glock_right"));

                        for(Item item : inventory) {
                            sendClientTCPMessage("inventory/"+item.id);
                            System.out.println("inventory/"+item.id);
                        }
                    } else if (parseMessage[0].equals("disconnect")) {
                        sendGlobalMessage(message + "/" + name);
                        this.client.close();
                        clientArray.remove(this);

                    } else if (parseMessage[0].equals("drop")) {
                        for (Item item : inventory) {
                            if (item.id.equals(parseMessage[3])) {
                                inventory.remove(item);
                                addToMap(new Item(parseMessage[3], Integer.parseInt(parseMessage[1]), Integer.parseInt(parseMessage[2])));
                                sendGlobalMessage(message);
                                System.out.println("Grabbed");
                                break;
                            }
                        }
                    } else if (parseMessage[0].equals("grab")) {
                        for (int i = 0; i < mapItems.size(); i++) {
                            Item item = mapItems.get(i);
                            int dX = Math.abs(Integer.parseInt(parseMessage[1]) - item.x);
                            int dY = Math.abs(Integer.parseInt(parseMessage[2]) - item.y);
                            double distance = Math.sqrt(dX * dX + dY * dY);
                            if (distance <= 16) {
                                mapItems.remove(item);
                                for (ClientHandler client : clientArray) {
                                    if (client.name.equals(parseMessage[3])) {
                                        client.inventory.add(new Item(parseMessage[4]));
                                        sendGlobalMessage(message);
                                        System.out.println("Removed");

                                        break;
                                    }

                                }


                            }

                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        // TCP Individual Client Methods
        public void sendClientTCPMessage(String message) {
            out.println(message);
            out.flush();
        }

        public void sendClientGlobalUserList() {
            for (ClientHandler client : clientArray) {
                if (!client.equals(this)) {
                    this.out.println("connect" + "/" + client.x + "/" + client.y + "/" + client.name + "/" + client.weapon + "/" + client.dir);
                    this.out.flush();
                }
            }
        }

        public void informServerOfConnection() {
            for (ClientHandler client : clientArray) {
                if (!client.equals(this)) {
                    client.out.println("connect" + "/" + x + "/" + y + "/" + name + "/" + weapon + "/" + dir);
                    client.out.flush();
                }
            }
        }

        public void sendGlobalMessage(String message) {
            for (ClientHandler client : clientArray) {
                if (!client.equals(this)) {
                    client.out.println(message);
                    client.out.flush();
                }
            }
        }


        // UDP Global Client Methods
        public void broadcastUDPGlobalMessage (String message) {
            for (ClientHandler client : clientArray) {
                if (!client.equals(this)) {
                    try {
                        byte[] sendData = new byte[1024];
                        sendData = (message).getBytes();
                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, client.IPAddress, client.port);
                        socket.send(sendPacket);
                        // Send UDP Message
                    } catch (Exception e) {e.printStackTrace();}
                }
            }
        }

        // Basic Player Configuration Methods
        public void setPort(int port) {
            this.port = port;
        }

        public void setIPAddress(InetAddress IPAddress) {
            this.IPAddress = IPAddress;
        }

        public void setX(String x) {
            this.x = Integer.parseInt(x);
        }

        public void setY(String y) {
            this.y = Integer.parseInt(y);
        }

        public void setDir(String dir) {
            this.dir = Integer.parseInt(dir);
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setWeapon(String weapon) {
            this.weapon = weapon;
        }

        // Advanced Player Interaction methods
        public void createPlayer(String x, String y, String name, String weapon,  String dir) {
            setX(x);
            setY(y);
            setWeapon(weapon);
            setDir(dir);
            setName(name);
        }

        public void move(String x, String y, String dir) {
            setX(x);
            setY(y);
            setDir(dir);
            broadcastUDPGlobalMessage("move" + "/" + x + "/" + y + "/" + name + "/" + dir);
        }

        // Public server entity methods
        public void addToMap(Item item) {
            mapItems.add(item);
        }


    }

    public static void main (String[] args) {
        Thread thread = new Thread(new ServerHandler());
        thread.start();
        Thread UDPthread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new DatagramSocket(5000);
                     while (true) {
                         byte[] receiveData = new byte[1024];
                         DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                         socket.receive(receivePacket);
                         String message = new String(receivePacket.getData()).toLowerCase().trim();
                         System.out.println("Message: " + message);
                         String parseMessage[] = message.split("/");

                         if (parseMessage[0].equals("move")) {
                            for (ClientHandler client : clientArray) {
                                if (client.name.equals(parseMessage[3])) {
                                    client.move(parseMessage[1], parseMessage[2], parseMessage[4]);
                                }
                            }
                         } else if (parseMessage[0].equals("shoot")) {
                             for (ClientHandler client : clientArray) {
                                 if (client.name.equals(parseMessage[3])) {
                                    client.broadcastUDPGlobalMessage(message);
                                 }
                             }
                         } else if (parseMessage[0].equals("port")) {
                             for (ClientHandler client : clientArray) {
                                 if (client.name.equals(parseMessage[1])) {
                                     client.setPort(receivePacket.getPort());
                                     client.setIPAddress(receivePacket.getAddress());

                                 }
                             }
                         } else if (parseMessage[0].equals("stop")) {
                             for (ClientHandler client : clientArray) {
                                 if (client.name.equals(parseMessage[1])) {
                                     client.broadcastUDPGlobalMessage(message);
                                 }
                             }
                         }
                     }




                } catch (Exception e) { e.printStackTrace();}
            }
        });
        UDPthread.start();
    }



}
