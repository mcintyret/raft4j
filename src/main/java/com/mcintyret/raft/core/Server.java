package com.mcintyret.raft.core;

import com.mcintyret.raft.elect.ElectionTimeoutGenerator;
import com.mcintyret.raft.message.MessageDispatcher;
import com.mcintyret.raft.persist.PersistentState;
import com.mcintyret.raft.rpc.AppendEntriesRequest;
import com.mcintyret.raft.rpc.AppendEntriesResponse;
import com.mcintyret.raft.rpc.NewEntryRequest;
import com.mcintyret.raft.rpc.NewEntryResponse;
import com.mcintyret.raft.rpc.RequestVoteRequest;
import com.mcintyret.raft.rpc.RequestVoteResponse;
import com.mcintyret.raft.rpc.RpcMessage;
import com.mcintyret.raft.rpc.RpcMessageVisitor;
import com.mcintyret.raft.util.Multiset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private static final long HEARTBEAT_TIMEOUT = 20;

    private final int myId;

    // Simplifying assumption for now: peers are immutable. A peer may disappear then reappear, but no NEW peers will
    // turn up.
    // TODO: remove this assumption!
    private final List<Integer> peers;

    private final int majoritySize;

    private final PersistentState persistentState;

    private final ElectionTimeoutGenerator electionTimeoutGenerator;

    private final MessageDispatcher messageDispatcher;

    // All messages are processed in a single thread, simplifying the logic
    private final BlockingQueue<RpcMessage> messageQueue = new LinkedBlockingQueue<>();

    // TODO: behaviour by polymorphism, rather than switching on this
    private ServerRole currentRole = ServerRole.CANDIDATE; // At startup, every server is a candidate

    private final Multiset<Long> indicesAwaitingCommit = new Multiset<>();

    private int currentLeaderId;

    private long commitIndex;

    private long lastApplied;

    private long nextElectionTimeout;

    private long nextHeartbeat;

    // candidate only
    private final Set<Integer> votes = new HashSet<>();

    // leader only
    private final long[] nextIndices;

    // TODO: this can just keep growing if there aren't any responses - needs to be smarter
    private final Map<Long, AppendEntriesRequest> appendEntriesRequests = new HashMap<>();

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
        majoritySize = peers.size() / 2;
        nextIndices = new long[peers.size()];
    }

    public void messageReceived(RpcMessage message) {
        messageQueue.add(message);
    }

    public void run() {
        while (true) {
            long timeout = (currentRole == ServerRole.LEADER ? nextHeartbeat :
                nextElectionTimeout) - System.currentTimeMillis();

            RpcMessage message;
            try {
                message = messageQueue.poll(timeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                throw new AssertionError(e);
            }

            if (message != null) {
                checkTerm(message);
                message.visit(this);
            } else {
                // timed out
                if (currentRole == ServerRole.LEADER) {
                    sendAppendEntriesRequests(false);
                } else {
                    startElection();
                }
            }
        }
    }

    // If someone else's term > mine, then I should become a follower and update my term
    private void checkTerm(RpcMessage message) {
        if (!(message instanceof NewEntryRequest) && message.getTerm() > persistentState.getCurrentTerm()) {
            currentRole = ServerRole.FOLLOWER;
            persistentState.setCurrentTerm(message.getTerm());
        }
    }

    private void startElection() {
        currentRole = ServerRole.CANDIDATE;
        votes.clear(); // reset
        long newTerm = persistentState.getCurrentTerm() + 1;
        persistentState.setCurrentTerm(newTerm);

        // TODO: is this correct? Or should it be last COMMITTED log entry?
        LogEntry lastLogEntry = persistentState.getLastLogEntry();

        RequestVoteRequest voteRequest = new RequestVoteRequest(
            newTerm,
            myId,
            lastLogEntry.getIndex(),
            lastLogEntry.getTerm()
        );

        sendToAll(voteRequest);

        resetElectionTimeout();
    }

    @Override
    public void onAppendEntriesRequest(AppendEntriesRequest aeReq) {
        currentLeaderId = aeReq.getLeaderId();
        long currentTerm = persistentState.getCurrentTerm();

        boolean success = false;

        if (currentTerm <= aeReq.getTerm()) {

            resetElectionTimeout();
            currentRole = ServerRole.FOLLOWER; // probably already was, but we may have been a candidate

            LogEntry lastLogEntry = persistentState.getLastLogEntry();

            List<LogEntry> entries = aeReq.getEntries();
            if (lastLogEntry.getTerm() == aeReq.getPrevLogTerm() &&
                lastLogEntry.getIndex() == aeReq.getPrevLogIndex()) {

                // Success!
                persistentState.deleteConflictingAndAppend(entries);
                success = true;

            }

            if (aeReq.getLeaderCommit() > commitIndex && !entries.isEmpty()) {
                commitIndex = Math.min(aeReq.getLeaderCommit(), entries.get(entries.size() - 1).getIndex());
                // TODO: apply to state machine
            }
        }

        AppendEntriesResponse response = new AppendEntriesResponse(aeReq.getRequestId(), currentTerm, success);
        messageDispatcher.sendMessage(currentLeaderId, response);
    }

    private void resetElectionTimeout() {
        nextElectionTimeout = electionTimeoutGenerator.nextElectionTimeout();
    }

    @Override
    public void onAppendEntriesResponse(AppendEntriesResponse aeResp) {
        // This may be the case if this response told us that we should be a follower now
        if (currentRole != ServerRole.LEADER) {
            return;
        }

        AppendEntriesRequest request = appendEntriesRequests.remove(aeResp.getRequestId());
        if (request == null) {
//            throw new IllegalStateException("Could not find AppendEntriesRequest corresponding to response with id " + aeResp.getRequestId());
            return; // Presumably wasn't us who sent this request and we have received the response in error
        }

        int peerIndex = getIndexForPeer(request.getTargetId());
        if (aeResp.isSuccess()) {
            List<LogEntry> entries = request.getEntries();
            if (!entries.isEmpty()) {
                // not a heartbeat - actually update state
                nextIndices[peerIndex] = entries.get(entries.size() - 1).getIndex() + 1;

                // This response could meant that a majority of servers have seen that log entry - they have been committed.
                entries.forEach(entry -> {
                    long index = entry.getIndex();
                    if (index > commitIndex && indicesAwaitingCommit.add(index) >= majoritySize) {
                        commitIndex = index;
                    }
                });
                // TODO: apply newly-committed entries!

            }
        } else {
            nextIndices[peerIndex]--; // TODO: smarter algorithm here?
        }
    }

    @Override
    public void onRequestVoteRequest(RequestVoteRequest rvReq) {
        long currentTerm = persistentState.getCurrentTerm();
        boolean voteGranted = false;

        if (rvReq.getTerm() >= currentTerm) {
            int votedFor = persistentState.getVotedFor();
            // if we haven't voted for someone else already...
            if ((votedFor == -1 || votedFor == rvReq.getCandidateId())) {
                // ...and this candidate is at lease as up-to-date as we are
                LogEntry lastLogEntry = persistentState.getLastLogEntry();
                if (rvReq.getLastLogTerm() > lastLogEntry.getTerm() ||
                    (rvReq.getLastLogTerm() == lastLogEntry.getTerm() && rvReq.getLastLogIndex() >= lastLogEntry.getIndex())) {

                    voteGranted = true;
                }
            }
        }

        RequestVoteResponse response = new RequestVoteResponse(myId, currentTerm, voteGranted);

        messageDispatcher.sendMessage(rvReq.getCandidateId(), response);
    }


    @Override
    public void onRequestVoteResponse(RequestVoteResponse rvResp) {
        if (currentRole == ServerRole.CANDIDATE && rvResp.isVoteGranted()) {
            votes.add(rvResp.getVoterId());

            if (votes.size() >= majoritySize) {

                // I've won! (as least as far as I'm concerned...
                becomeLeader();
            }
        }
    }

    private void becomeLeader() {
        currentRole = ServerRole.LEADER;
        currentLeaderId = myId;
        sendAppendEntriesRequests(true);

        // TODO: Again, should this be the last COMMITTED index?
        LogEntry lastLogEntry = persistentState.getLastLogEntry();
        Arrays.fill(nextIndices, lastLogEntry.getIndex() + 1);
    }

    @Override
    public void onNewEntryRequest(NewEntryRequest neReq) {
        NewEntryResponse response;
        if (currentRole == ServerRole.LEADER) {
            LogEntry lastEntry = persistentState.getLastLogEntry();
            LogEntry newEntry = new LogEntry(lastEntry.getTerm(), lastEntry.getIndex() + 1, neReq.getData());
            persistentState.appendLogEntry(newEntry);


        } else {
            // redirect to the leader
            response = new NewEntryResponse(currentLeaderId, false);
        }

        // TODO - some way to respond to the client!
        // NewEntry probably shouldn't implement RpcMessage
    }

    public int getMyId() {
        return myId;
    }

    private void sendAppendEntriesRequests(boolean heartbeat) {
        if (currentRole != ServerRole.LEADER) {
            throw new IllegalStateException("Only the leader should send AppendEntriesRequests");
        }

        LogEntry lastLogEntry = persistentState.getLastLogEntry();

        peers.forEach(recipient -> {
            int peerIndex = getIndexForPeer(recipient);

            LogEntry previousForPeer = persistentState.getLogEntry(nextIndices[peerIndex] - 1);

            List<LogEntry> logsToSend = heartbeat ? Collections.<LogEntry>emptyList() :
                // Everything from the last-seen to now. If this turns out to be empty this is essentially a heartbeat.
                persistentState.getLogEntriesBetween(nextIndices[peerIndex], lastLogEntry.getIndex() + 1);

            AppendEntriesRequest request = new AppendEntriesRequest(
                persistentState.getCurrentTerm(),
                myId,
                recipient,
                previousForPeer.getIndex(),
                previousForPeer.getTerm(),
                logsToSend,
                commitIndex
            );
            appendEntriesRequests.put(request.getRequestId(), request);

            messageDispatcher.sendMessage(recipient, request);
        });
    }

    private void sendToAll(RpcMessage message) {
        peers.forEach(recipient -> messageDispatcher.sendMessage(recipient, message));
    }

    private int getIndexForPeer(Integer peerId) {
        return peers.indexOf(peerId);
    }
}
