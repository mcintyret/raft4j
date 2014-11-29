package com.mcintyret.raft.core;

import com.mcintyret.raft.elect.ElectionTimeoutGenerator;
import com.mcintyret.raft.message.MessageDispatcher;
import com.mcintyret.raft.persist.PersistentState;
import com.mcintyret.raft.rpc.AppendEntriesRequest;
import com.mcintyret.raft.rpc.AppendEntriesResponse;
import com.mcintyret.raft.rpc.NewEntryRequest;
import com.mcintyret.raft.rpc.RequestVoteRequest;
import com.mcintyret.raft.rpc.RequestVoteResponse;
import com.mcintyret.raft.rpc.RpcMessage;
import com.mcintyret.raft.rpc.RpcMessageVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * User: tommcintyre
 * Date: 11/29/14
 */
public class Server implements RpcMessageVisitor {

    private static final Logger LOG = LoggerFactory.getLogger(Server.class);

    // TODO: configurable?
    private static final long HEARTBEAT_TIMEOUT = 200;

    private final int myId;

    // Simplifying assumption for now: peers are immutable. A peer may disappear then reappear, but no NEW peers will
    // turn up.
    // TODO: remove this assumption!
    private final List<Integer> peers;

    private final PersistentState persistentState;

    private final ElectionTimeoutGenerator electionTimeoutGenerator;

    private final MessageDispatcher messageDispatcher;

    // All messages are processed in a single thread, simplifying the logic
    private final BlockingQueue<RpcMessage> messageQueue = new LinkedBlockingQueue<>();

    // TODO: behaviour by polymorphism, rather than switching on this
    private ServerRole currentRole = ServerRole.CANDIDATE; // At startup, every server is a candidate

    private int currentLeaderId;

    private long commitIndex;

    private long lastApplied;

    private long nextElectionTimeout;

    private long nextHeartbeat;

    // candidate only
    private int votes;

    public Server(int myId, List<Integer> peers,
                  PersistentState persistentState,
                  ElectionTimeoutGenerator electionTimeoutGenerator,
                  MessageDispatcher messageDispatcher) {
        // TODO: do I actually need this constraint? Isn't the point of election timeouts to deal with split votes?
        if (peers.size() % 2 != 0) {
            throw new IllegalArgumentException("Must be an even number of peers (and so an odd number of servers" +
                " including myself) so that a majority can be reached.");
        }
        this.myId = myId;
        this.peers = peers;
        this.persistentState = persistentState;
        this.electionTimeoutGenerator = electionTimeoutGenerator;
        this.messageDispatcher = messageDispatcher;

        resetElectionTimeout();
        this.nextHeartbeat = System.currentTimeMillis() + HEARTBEAT_TIMEOUT;
    }

    public void messageReceived(RpcMessage message) {
        messageQueue.add(message);
    }

    public void run() throws InterruptedException {
        while (true) {
            long timeout = (currentRole == ServerRole.LEADER ? nextHeartbeat :
                nextElectionTimeout) - System.currentTimeMillis();

            RpcMessage message = messageQueue.poll(timeout, TimeUnit.MILLISECONDS);

            if (message != null) {
                // If I'm not already a follower, and someone else's term > mine, then I should become a follower
                if ( currentRole != ServerRole.FOLLOWER &&
                    !(message instanceof NewEntryRequest)
                    && message.getTerm() > persistentState.getCurrentTerm()) {
                    currentRole = ServerRole.FOLLOWER;
                }
                message.visit(this);
            } else {
                // timed out
                if (currentRole == ServerRole.LEADER) {
                    sendHeartbeat();
                } else {
                    startElection();
                }
            }
        }
    }

    private void startElection() {
        currentRole = ServerRole.CANDIDATE;
        long newTerm = persistentState.getCurrentTerm() + 1;
        persistentState.setCurrentTerm(newTerm);
        LogEntry lastLogEntry = getLastLogEntry();

        RequestVoteRequest voteRequest = new RequestVoteRequest(
            newTerm,
            myId,
            lastLogEntry == null ? 0 : lastLogEntry.getIndex(),
            lastLogEntry == null ? 0 : lastLogEntry.getTerm()
        );

        sendToAll(voteRequest);

        resetElectionTimeout();
    }

    @Override
    public void onAppendEntriesRequest(AppendEntriesRequest aeReq) {
        AppendEntriesResponse response = null;
        if (aeReq.getTerm() >= persistentState.getCurrentTerm()) {
            resetElectionTimeout();
        }
    }

    private void resetElectionTimeout() {
        nextElectionTimeout = electionTimeoutGenerator.nextElectionTimeout();
    }

    @Override
    public void onAppendEntriesResponse(AppendEntriesResponse aeResp) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void onRequestVoteRequest(RequestVoteRequest rvReq) {
        // Same behaviour for all states
        RequestVoteResponse response = null;
        long currentTerm = persistentState.getCurrentTerm();

        if (rvReq.getTerm() >= currentTerm) {
            int votedFor = persistentState.getVotedFor();
            // if we haven't voted for someone else already...
            if ((votedFor == -1 || votedFor == rvReq.getCandidateId())) {
                // ...and this candidate is at lease as up-to-date as we are
                LogEntry lastLogEntry = getLastLogEntry();
                if (lastLogEntry == null || rvReq.getLastLogTerm() > lastLogEntry.getTerm() ||
                    (rvReq.getLastLogTerm() == lastLogEntry.getTerm() && rvReq.getLastLogIndex() >= lastLogEntry.getIndex())) {

                    response = new RequestVoteResponse(currentTerm, true);
                }
            }
        }

        if (response == null) {
            response = new RequestVoteResponse(currentTerm, false);
        }

        messageDispatcher.sendMessage(rvReq.getCandidateId(), response);
    }

    private LogEntry getLastLogEntry() {
        return commitIndex == 0 ? null : persistentState.getLogEntry(commitIndex);
    }

    @Override
    public void onRequestVoteResponse(RequestVoteResponse rvResp) {
        // TODO: is this safeguarded against repeated votes?
        if (currentRole == ServerRole.CANDIDATE &&
            rvResp.isVoteGranted() &&
            votes++ >= peers.size() / 2) {

            // I've won! (as least as far as I'm concerned
            currentRole = ServerRole.LEADER;
            currentLeaderId = myId;
            sendHeartbeat();
        }
    }

    @Override
    public void onNewEntryRequest(NewEntryRequest neReq) {

    }

    public int getMyId() {
        return myId;
    }

    private void sendHeartbeat() {
        if (currentRole != ServerRole.LEADER) {
            throw new IllegalStateException("Only the leader should send heartbeats");
        }
        AppendEntriesRequest heartbeat = new AppendEntriesRequest(
            persistentState.getCurrentTerm(),
            myId,
            -1,
            -1,
            Collections.<LogEntry>emptyList(),
            commitIndex
        );

        sendToAll(heartbeat);
        this.nextHeartbeat = System.currentTimeMillis() + HEARTBEAT_TIMEOUT;
    }

    private void sendToAll(RpcMessage message) {
        peers.forEach(recipient -> messageDispatcher.sendMessage(recipient, message));
    }
}
