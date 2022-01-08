import api.API;
import api.APIFactory;
import database.Database;
import database.Firestore;
import http.HttpMessage;
import http.HttpReceiver;
import http.HttpRequest;
import http.HttpSender;

import java.net.*;

public class Server {
    public static void run(int port) {
        try {
            System.out.println("Listening on port " + port);
            ServerSocket serverSocket = new ServerSocket(port);
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    ServerThread st = new ServerThread(socket);
                    st.start();
                    System.out.println("Connected");
                } catch (Exception e) {
                    System.out.println("Connection Error");
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            System.out.println("Server error");
            e.printStackTrace();
        }
    }
}

class ServerThread extends Thread {
    private final Socket socket;

    public ServerThread(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try {
            Database database = new Firestore();
            HttpSender sender = new HttpSender(socket);
            HttpReceiver receiver = new HttpReceiver(socket);

            while(true) {
                HttpMessage message = receiver.readMessage();
                System.out.println("Get request");
                if(message instanceof HttpRequest request) {
                    API api = APIFactory.getAPI(request.path);
                    api.handle(request, sender, database);
                    System.out.println("Send response");
                }
                else {
                    System.out.println("Http error");
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Server thread error");
        }
    }
}