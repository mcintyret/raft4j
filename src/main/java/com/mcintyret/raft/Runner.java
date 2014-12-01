package com.mcintyret.raft;

import com.mcintyret.raft.client.Client;
import com.mcintyret.raft.client.ClientMessage;
import com.mcintyret.raft.core.Server;
import com.mcintyret.raft.elect.RandomElectionTimeoutGenerator;
import com.mcintyret.raft.message.MessageDispatcher;
import com.mcintyret.raft.message.MessageHandler;
import com.mcintyret.raft.persist.InMemoryPersistentState;
import com.mcintyret.raft.rpc.Message;
import com.mcintyret.raft.rpc.NewEntryRequest;
import com.mcintyret.raft.rpc.NewEntryResponse;
import com.mcintyret.raft.rpc.RaftRpcMessage;
import com.mcintyret.raft.rpc.RpcMessage;
import com.mcintyret.raft.state.StateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * User: tommcintyre
 * Date: 11/29/14
 */
public class Runner {

    private enum LoggingStateMachine implements StateMachine {
        INSTANCE;

        @Override
        public void apply(long index, byte[] data) {
            LOG.info("Index: {}, data: {}", index, new String(data));
        }

        private static final Logger LOG = LoggerFactory.getLogger("StateMachine");

    }

    public static void main(String[] args) throws IOException, InterruptedException {
        int size = 5;

        final List<Server> servers = new ArrayList<>(size);

        final BlockingQueue<ClientMessage> clientMessages = new LinkedBlockingQueue<>();

        MessageDispatcher messageDispatcher = new MessageDispatcher() {

            @Override
            public void sendMessage(int recipientId, Message message) {
                servers.get(recipientId).messageReceived((RpcMessage) message);
            }

            @Override
            public void sendMessage(ClientMessage message) {
                clientMessages.add(message);
            }
        };

        List<Integer> allIds = makeAllIds(size);

        for (int i = 0; i < size; i++) {
            List<Integer> peers = new ArrayList<>(allIds);
            peers.remove((Integer) i);
            servers.add(new Server(i, peers,
                new InMemoryPersistentState(),
                new RandomElectionTimeoutGenerator(5000L, 6000L),
                messageDispatcher,
                LoggingStateMachine.INSTANCE));
        }

        // Start them all!
        servers.forEach(server -> new Thread(server::run, "Server: " + server.getMyId()).start());

        ConsoleClient client = new ConsoleClient(messageDispatcher, clientMessages);
        client.run();

    }

    private static List<Integer> makeAllIds(int size) {
        List<Integer> tmp = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            tmp.add(i);
        }
        return Collections.unmodifiableList(tmp);
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
                        Integer id = Integer.valueOf(parts[0].trim());
                        byte[] messageBytes = parts[1].getBytes();

                        sendBytes(id, messageBytes);
                    } catch (Exception e) {
                        System.err.println("Invalid input: " + input + " - " + e.getClass() + " - " + e.getMessage());
                    }
                }
            }
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

//    public static void main(String[] args) throws IOException {
//        while (true) {
//            String in = readInputStreamWithTimeout(System.in, 1000);
//            if (in != null) {
//                System.out.println(in);
//            }
//        }
//    }

}
