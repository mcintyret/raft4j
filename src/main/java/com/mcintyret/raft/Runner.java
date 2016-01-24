package com.mcintyret.raft;

import com.mcintyret.raft.address.Address;
import com.mcintyret.raft.address.Client;
import com.mcintyret.raft.address.Peer;
import com.mcintyret.raft.core.Server;
import com.mcintyret.raft.elect.ElectionTimeoutGenerator;
import com.mcintyret.raft.elect.RandomElectionTimeoutGenerator;
import com.mcintyret.raft.message.BaseRetryingMessageDispatcher;
import com.mcintyret.raft.message.DecoratingMessageReceiver;
import com.mcintyret.raft.message.MessageDispatcher;
import com.mcintyret.raft.message.MessageReceiver;
import com.mcintyret.raft.message.Messages;
import com.mcintyret.raft.persist.InMemoryPersistentState;
import com.mcintyret.raft.persist.PersistentState;
import com.mcintyret.raft.rpc.BaseRequest;
import com.mcintyret.raft.rpc.BaseResponse;
import com.mcintyret.raft.rpc.Header;
import com.mcintyret.raft.rpc.Message;
import com.mcintyret.raft.rpc.NewEntryRequest;
import com.mcintyret.raft.rpc.NewEntryResponse;
import com.mcintyret.raft.rpc.RpcMessage;
import com.mcintyret.raft.state.FileWritingStateMachine;
import com.mcintyret.raft.state.StateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

/**
 * User: tommcintyre
 * Date: 11/29/14
 */
public class Runner {

    private static final int SIZE = 5;

    private static final ElectionTimeoutGenerator ELECTION_TIMEOUT_GENERATOR = new RandomElectionTimeoutGenerator(5000L, 6000L);

    private static final Map<Peer, ServerController> SERVERS = new ConcurrentHashMap<>(SIZE);

    private static final Set<Peer> UNREACHABLE_PEERS = new CopyOnWriteArraySet<>();

    private static final List<ServerController> SERVER_CONTROLLERS = new ArrayList<>(SIZE);


    public static void main(String[] args) throws IOException, InterruptedException {

        for (int i = 0; i < SIZE; i++) {
            SERVER_CONTROLLERS.add(new ServerController(i));
        }

        // Start them all!
        SERVER_CONTROLLERS.forEach(ServerController::start);

        ConsoleClient client = new ConsoleClient();
        client.run();

    }

    private static class ConsoleClient implements Client, MessageReceiver<Message> {

        private static final Logger LOG = LoggerFactory.getLogger("Client");

        private final MessageDispatcher messageDispatcher;

        private final BlockingQueue<Message> clientMessages = new LinkedBlockingQueue<>();

        private final MessageReceiver<Message> messageReceiver;

        private ConsoleClient() {
            Messages messages = new Messages();
            this.messageDispatcher = new RetryingMessageDispatcherImpl(messages);
            this.messageReceiver = new DecoratingMessageReceiver<>(messages, this);
        }

        public void run() throws IOException, InterruptedException {
            while (true) {
                Message message = clientMessages.poll(100, TimeUnit.MILLISECONDS);

                if (message != null) {
                    handleResponse((NewEntryResponse) message);
                }

                String input = readInputStreamWithTimeout(System.in, 100);
                if (input != null) {
                    try {
                        String[] parts = input.split(":");
                        Integer id = Integer.valueOf(parts[1].trim());
                        String command = parts[0].trim();

                        switch (command) {
                            case "send":
                                sendBytes(new IntegerPeer(id), parts[2].getBytes());
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
            if (message.getRedirect() == null) {
                LOG.info("Success for message " + message.getHeader().getRuuid());
            } else {
                sendBytes(message.getRedirect(), message.getRequest().getData());
            }
        }

        private void sendBytes(Peer peer, byte[] bytes) {
            messageDispatcher.sendRequest(new NewEntryRequest(new Header(this, peer), bytes));
        }

        @Override
        public void messageReceived(Message message) {
            clientMessages.add(message);
        }

        @Override
        public String toString() {
            return "ConsoleClient";
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

    private static List<Peer> makePeersList(IntegerPeer me) {
        List<Peer> peers = new ArrayList<>(SIZE - 1);
        for (int i = 0; i < SIZE; i++) {
            if (i != me.getId()) {
                peers.add(new IntegerPeer(i));
            }
        }
        return Collections.unmodifiableList(peers);
    }

    private static class ServerController {

        private final IntegerPeer peer;

        private final List<Peer> peers;

        // Survives across incarnations, by definition
        private final PersistentState persistentState = new InMemoryPersistentState();

        // TODO: think about whether this should survive across incarnations?
        private final StateMachine stateMachine;

        private ServerController(int i) {
            this.peer = new IntegerPeer(i);
            this.peers = makePeersList(peer);
            this.stateMachine = new FileWritingStateMachine("logs/" + peer + ".log");
        }

        private Server server;

        private Thread thread;

        private MessageReceiver<RpcMessage> messageReceiver;

        public void start() {
            if (server != null) {
                throw new IllegalStateException("Server already running, cannot call start");
            }

            Messages messages = new Messages();

            server = new Server(peer, peers, persistentState, ELECTION_TIMEOUT_GENERATOR, new RetryingMessageDispatcherImpl(messages), stateMachine);
            messageReceiver = new DecoratingMessageReceiver<>(messages, server);
            thread = new Thread(server::run, "Server: " + peer);

            if (SERVERS.put(peer, this) != null) {
                throw new IllegalStateException("Existing value found for server id " + peer + " when should be empty");
            }
            thread.start();
        }

        public void stop() {
            if (server == null) {
                throw new IllegalStateException("Server already stopped, cannot call stop");
            }

            messageReceiver = null;
            thread.interrupt();
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new AssertionError(e);
            }
            SERVERS.remove(peer);
            thread = null;
            server = null;
        }

        public void makeUnreachable() {
            UNREACHABLE_PEERS.add(peer);
        }

        public void makeReachable() {
            UNREACHABLE_PEERS.remove(peer);
        }
    }

    private static class RetryingMessageDispatcherImpl extends BaseRetryingMessageDispatcher {

        public RetryingMessageDispatcherImpl(Messages messages) {
            super(messages);
        }

        @Override
        protected void sendRequestInternal(BaseRequest message) {
            sendMessage(message);
        }

        private void sendMessage(Message message) {
            ServerController serverController;

            Address destination = message.getHeader().getDestination();
            if (destination instanceof ConsoleClient) {
                ((ConsoleClient) destination).messageReceiver.messageReceived(message);
            }

            if (!UNREACHABLE_PEERS.contains(destination) && (serverController = SERVERS.get(destination)) != null) {
                serverController.messageReceiver.messageReceived((RpcMessage) message);
            }
        }

        @Override
        public void sendResponse(BaseResponse response) {
            sendMessage(response);
        }
    }

    private static final class IntegerPeer implements Peer {
        private final int id;

        private IntegerPeer(int id) {
            this.id = id;
        }

        private int getId() {
            return id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            IntegerPeer that = (IntegerPeer) o;

            if (id != that.id) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return id;
        }

        @Override
        public String toString() {
            return Integer.toString(id);
        }
    }

}
