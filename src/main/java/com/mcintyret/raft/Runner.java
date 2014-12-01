package com.mcintyret.raft;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mcintyret.raft.client.Client;
import com.mcintyret.raft.client.ClientMessage;
import com.mcintyret.raft.core.Server;
import com.mcintyret.raft.elect.ElectionTimeoutGenerator;
import com.mcintyret.raft.elect.RandomElectionTimeoutGenerator;
import com.mcintyret.raft.message.MessageDispatcher;
import com.mcintyret.raft.message.MessageHandler;
import com.mcintyret.raft.persist.InMemoryPersistentState;
import com.mcintyret.raft.persist.PersistentState;
import com.mcintyret.raft.rpc.Message;
import com.mcintyret.raft.rpc.NewEntryRequest;
import com.mcintyret.raft.rpc.NewEntryResponse;
import com.mcintyret.raft.rpc.RpcMessage;
import com.mcintyret.raft.state.FileWritingStateMachine;
import com.mcintyret.raft.state.StateMachine;

/**
 * User: tommcintyre
 * Date: 11/29/14
 */
public class Runner {

    private static final int SIZE = 5;

    private static final ElectionTimeoutGenerator ELECTION_TIMEOUT_GENERATOR = new RandomElectionTimeoutGenerator(5000L, 6000L);

    private static final Map<Integer, Server> SERVERS = new ConcurrentHashMap<>(SIZE);

    private static final BlockingQueue<ClientMessage> CLIENT_MESSAGES = new LinkedBlockingQueue<>();

    private static final Set<Integer> UNREACHABLE_CLIENTS = new CopyOnWriteArraySet<>();

    private static final List<ServerController> SERVER_CONTROLLERS = new ArrayList<>(SIZE);

    private static final MessageDispatcher MESSAGE_DISPATCHER = new MessageDispatcher() {

        @Override
        public void sendMessage(int recipientId, Message message) {
            Server server;
            if (!UNREACHABLE_CLIENTS.contains(recipientId) && (server = SERVERS.get(recipientId)) != null) {
                server.messageReceived((RpcMessage) message);
            }
        }

        @Override
        public void sendMessage(ClientMessage message) {
            CLIENT_MESSAGES.add(message);
        }
    };

    public static void main(String[] args) throws IOException, InterruptedException {

        for (int i = 0; i < SIZE; i++) {
            SERVER_CONTROLLERS.add(new ServerController(i));
        }

        // Start them all!
        SERVER_CONTROLLERS.forEach(ServerController::start);

        ConsoleClient client = new ConsoleClient(MESSAGE_DISPATCHER, CLIENT_MESSAGES);
        client.run();

    }

    private static class ConsoleClient implements Client {

        private static final Logger LOG = LoggerFactory.getLogger("Client");

        private final MessageDispatcher messageDispatcher;

        private final MessageHandler messageHandler = new MessageHandler();

        private final BlockingQueue<ClientMessage> clientMessages;

        private ConsoleClient(MessageDispatcher messageDispatcher,
                              BlockingQueue<ClientMessage> clientMessages) {
            this.messageDispatcher = messageDispatcher;
            this.clientMessages = clientMessages;
        }

        public void run() throws IOException, InterruptedException {
            while (true) {
                ClientMessage message = clientMessages.poll(100, TimeUnit.MILLISECONDS);

                if (message != null) {
                    handleResponse(messageHandler.decorate((NewEntryResponse) message));
                }

                String input = readInputStreamWithTimeout(System.in, 100);
                if (input != null) {
                    try {
                        String[] parts = input.split(":");
                        Integer id = Integer.valueOf(parts[1].trim());
                        String command = parts[0].trim();

                        switch (command) {
                            case "send":
                                sendBytes(id, parts[2].getBytes());
                                break;
                            case "start":
                                SERVER_CONTROLLERS.get(id).start();
                                break;
                            case "stop":
                                SERVER_CONTROLLERS.get(id).stop();
                                break;
                            case "mute":
                                SERVER_CONTROLLERS.get(id).makeUnreachable();
                                break;
                            case "unmute":
                                SERVER_CONTROLLERS.get(id).makeReachable();
                                break;
                            default:
                                System.err.println("Unknown command: '" + command);
                                usage();
                        }

                    } catch (Exception e) {
                        System.err.println("Invalid input: " + input + " - " + e.getClass() + " - " + e.getMessage());
                        usage();
                    }
                }
            }
        }

        private static void usage() {
            String usage = "send: <id>: <message>\n" +
                           "start: <id>\n" +
                           "stop: <id>\n" +
                           "mute: <id>\n" +
                           "unmute: <id>";
            System.err.println(usage);
        }

        private void handleResponse(NewEntryResponse message) {
            if (message.isKnownSuccess()) {
                LOG.info("Success for message " + message.getRequestUid());
            } else {
                sendBytes(message.getRedirect(), message.getRequest().getData());
            }
        }

        private void sendBytes(Integer serverId, byte[] bytes) {
            messageDispatcher.sendMessage(serverId, messageHandler.register(new NewEntryRequest(this, bytes)));
        }
    }

    private static final byte[] BUF = new byte[1024];
    private static int bufferOffset = 0;

    public static String readInputStreamWithTimeout(InputStream is, long timeoutMillis) throws IOException {
        long maxTimeMillis = System.currentTimeMillis() + timeoutMillis;

        while (System.currentTimeMillis() < maxTimeMillis && bufferOffset < BUF.length) {
            int readLength = Math.min(is.available(), BUF.length - bufferOffset);

            // can alternatively use bufferedReader, guarded by isReady():
            if (readLength > 0) {
                int readResult = is.read(BUF, bufferOffset, readLength);
                if (readResult == -1) break;
                bufferOffset += readResult;
            }
        }

        String ret = null;
        if (bufferOffset > 0 && BUF[bufferOffset - 1] == '\n') {
            ret = new String(BUF, 0, bufferOffset - 1);
            bufferOffset = 0;
        }
        return ret;
    }

    private static List<Integer> makePeersList(int id) {
        List<Integer> peers = new ArrayList<>(SIZE - 1);
        for (int i = 0; i < SIZE; i++) {
            if (i != id) {
                peers.add(i);
            }
        }
        return Collections.unmodifiableList(peers);
    }

    private static class ServerController {

        private final int id;

        private final List<Integer> peers;

        // Survives across incarnations, by definition
        private final PersistentState persistentState = new InMemoryPersistentState();

        // TODO: think about whether this should survive across incarnations?
        private final StateMachine stateMachine;

        private ServerController(int id) {
            this.id = id;
            this.peers = makePeersList(id);
            this.stateMachine = new FileWritingStateMachine("logs/" + id + ".log");
        }

        private Server server;

        private Thread thread;

        public void start() {
            if (server != null) {
                throw new IllegalStateException("Server already running, cannot call start");
            }

            server = new Server(id, peers, persistentState, ELECTION_TIMEOUT_GENERATOR, MESSAGE_DISPATCHER, stateMachine);
            thread = new Thread(server::run, "Server: " + id);

            if (SERVERS.put(id, server) != null) {
                throw new IllegalStateException("Existing value found for server id " + id + " when should be empty");
            }
            thread.start();
        }

        public void stop() {
            if (server == null) {
                throw new IllegalStateException("Server already stopped, cannot call stop");
            }

            thread.interrupt();
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new AssertionError(e);
            }
            SERVERS.remove(id);
            thread = null;
            server = null;
        }

        public void makeUnreachable() {
            UNREACHABLE_CLIENTS.add(id);
        }

        public void makeReachable() {
            UNREACHABLE_CLIENTS.remove(id);
        }
    }

//    public static void main(String[] args) throws IOException {
//        while (true) {
//            String in = readInputStreamWithTimeout(System.in, 1000);
//            if (in != null) {
//                System.out.println(in);
//            }
//        }
//    }

}
