import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class ClientSession {
    private List<Question> questions = new ArrayList<>();
    private DatagramSocket socket;
    private SocketAddress address;
    private int questionIndex;
    private Semaphore mutex = new Semaphore(0);

    private int score;

    public ClientSession(List<Question> questions, DatagramSocket socket, SocketAddress address) {
        this.questions = questions;
        this.socket = socket;
        this.address = address;
    }

    public void handleMessage(String message) {
        if("ACK".equals(message)) {
            ack();
            return;
        }

        justSendMessage("ACK");

        if("Hello".equals(message)) {
            sendQuestion();
            return;
        }


        if(!"TIMEOUT".equals(message)) {
            char answerSymbol = message.charAt(0);
            Question q = questions.get(questionIndex);
            if(q.answers.get(Main.charToIndex(answerSymbol)).correct) {
                score++;
            }
        }

        questionIndex++;

        if(questionIndex == questions.size()) {
            sendMessage("Wynik: " + score);
        } else {
            sendQuestion();
        }
    }

    private void sendQuestion() {
        Question q = questions.get(questionIndex);
        StringBuilder sb = new StringBuilder();
        sb.append(q.text);
        sb.append("\n");
        for(Answer a : q.answers) {
            sb.append(a.value);
            sb.append("\n");
        }
        sendMessage(sb.toString());
    }

    private void justSendMessage(String message) {
        try {

            byte[] buffer = message.getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            packet.setSocketAddress(address);
            socket.send(packet);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public void sendMessage(String message) {
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

    public void ack() {
        mutex.release();
    }
}
