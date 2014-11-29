package com.mcintyret.raft.core;

import com.mcintyret.raft.elect.ElectionTimeoutGenerator;
import com.mcintyret.raft.message.MessageDispatcher;
import com.mcintyret.raft.persist.InMemoryPersistentState;
import com.mcintyret.raft.persist.PersistentState;
import com.mcintyret.raft.rpc.AppendEntriesRequest;
import com.mcintyret.raft.rpc.AppendEntriesResponse;
import com.mcintyret.raft.rpc.NewEntryRequest;
import com.mcintyret.raft.rpc.RequestVoteRequest;
import com.mcintyret.raft.rpc.RequestVoteResponse;
import com.mcintyret.raft.rpc.RpcMessage;
import com.mcintyret.raft.rpc.RpcMessageVisitor;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * User: tommcintyre
 * Date: 11/29/14
 */
public class Server implements RpcMessageVisitor {

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

    private long commitIndex;

    private long lastApplied;

    private long electionTimeout;

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
    }

    public void messageReceived(RpcMessage message) {
        messageQueue.add(message);
    }

    public void run() {
        while (true) {
            // TODO: use timeout version?
            RpcMessage message = messageQueue.poll();

            if (message != null) {
                if (!(message instanceof NewEntryRequest) && message.getTerm() > persistentState.getCurrentTerm()) {
                    currentRole = ServerRole.FOLLOWER;
                }
                message.visit(this);
            }
        }
    }

    @Override
    public void onAppendEntriesRequest(AppendEntriesRequest aeReq) {
        //To change body of implemented methods use File | Settings | File Templates.
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
        if (currentRole == ServerRole.CANDIDATE &&
            rvResp.isVoteGranted() &&
            votes++ >= peers.size() / 2) {

            // I've won! (as least as far as I'm concerned
            currentRole = ServerRole.LEADER;
        }
    }

    @Override
    public void onNewEntryRequest(NewEntryRequest neReq) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
