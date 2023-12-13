import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.*;

public class Main {
    private static Semaphore mutex = new Semaphore(0);
    private static DatagramSocket socket;
    private static SocketAddress serverAddress;

    private static Scanner scanner = new Scanner(System.in);

    static ExecutorService executorService = Executors.newSingleThreadExecutor();
    static ExecutorService inputExecutor = Executors.newSingleThreadExecutor();

    private static boolean exit;


    public static void main(String[] args) {

        try {

            socket = new DatagramSocket();
            serverAddress = new InetSocketAddress("localhost", 8080);

            sendMessageNewThread("Hello");

            while (!exit) {
                int bufferSize = 1024;
                byte[] buffer = new byte[bufferSize];
                DatagramPacket packet = new DatagramPacket(buffer, bufferSize);
                socket.receive(packet);
                handleMessage(new String(packet.getData(), StandardCharsets.UTF_8).trim());
            }

            System.out.println("Exiting client");


        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void sendMessageNewThread(String message) {
        executorService.execute(() -> sendMessage(message));
    }

    static void handleMessage(String message) {
        if ("ACK".equals(message)) {
            ack();
            return;
        }

        System.out.println(message);
        justSendMessage("ACK");
        if (message.charAt(0) == 'P') {

            Future<String> input = inputExecutor.submit(() -> scanner.nextLine());

            try {
                String line = input.get(10, TimeUnit.SECONDS);
                sendMessageNewThread(line);
            } catch (Exception e) {
                System.out.println("Upłynął czas na pytanie");
                sendMessageNewThread("TIMEOUT");
            }


        } else {
            exit = true;
        }
    }

    static void sendMessage(final String message) {

        while (true) {

            try {
                justSendMessage(message);

                if (mutex.tryAcquire(10, TimeUnit.SECONDS)) {
                    break;
                }

            } catch (InterruptedException e) {
                System.out.println("ACK waiting timeout");
            }
        }
    }

    private static void justSendMessage(String message) {
        try {

            byte[] buffer = message.getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            packet.setSocketAddress(serverAddress);
            socket.send(packet);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static void ack() {
        mutex.release();
    }
}