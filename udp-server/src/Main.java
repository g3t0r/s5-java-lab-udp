import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    private static final String QUESTIONS_FILE = "bazaPytan.txt";
    private static final String ANSWERS_FILE = "bazaOdpowiedzi.txt";
    private static final String SCORE_FILE = "wyniki.txt";

    public static void main(String[] args) throws Exception {
        System.out.println("Starting server");


        BufferedWriter answersWriter = null;
        BufferedWriter scoreWriter = null;

        List<Question> questions = parseQuestions();
        DatagramSocket serverSocket = null;
        ExecutorService executorService = null;
        Map<SocketAddress, ClientSession> map = new HashMap<>();
        try {
            serverSocket = new DatagramSocket(8080);
            executorService = Executors.newFixedThreadPool(250);
            scoreWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(SCORE_FILE)));
            answersWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(ANSWERS_FILE)));


            while (true) {
                int bufferSize = 1024;
                byte[] buffer = new byte[bufferSize];
                DatagramPacket packet = new DatagramPacket(buffer, bufferSize);
                serverSocket.receive(packet);

                DatagramSocket finalServerSocket = serverSocket;
                executorService.execute(() -> {
                    map.putIfAbsent(packet.getSocketAddress(), new ClientSession(questions, finalServerSocket, packet.getSocketAddress()));
                    ClientSession s = map.get(packet.getSocketAddress());
                    s.handleMessage(new String(packet.getData(), StandardCharsets.UTF_8).trim());
                });


            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {

            if (scoreWriter != null) {
                scoreWriter.close();
            }

            if (answersWriter != null) {
                answersWriter.close();
            }


            if (executorService != null) {
                executorService.close();
            }
        }
    }

    private static List<Question> parseQuestions() {
        List<Question> questions = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(QUESTIONS_FILE)))) {

            while (reader.ready()) {

                Question question = new Question();
                String line = reader.readLine();
                String[] segments = line.split(":");
                question.text = segments[0];
                question.answers = new ArrayList<>();

                for (int i = 0; i < 4; i++) {
                    line = reader.readLine();
                    Answer answer = new Answer();
                    answer.value = line;
                    if (i == charToIndex(segments[1].charAt(0))) {
                        answer.correct = true;
                        question.correctAnswer = answer;
                    }
                    question.answers.add(answer);
                }
                questions.add(question);
            }


        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return questions;
    }

    public static int charToIndex(char c) {
        return switch (c) {
            case 'a' -> 0;
            case 'b' -> 1;
            case 'c' -> 2;
            case 'd' -> 3;
            default -> throw new IllegalArgumentException("Illegal char: " + c);
        };
    }
}
