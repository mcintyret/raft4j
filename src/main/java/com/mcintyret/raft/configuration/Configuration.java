package com.mcintyret.raft.configuration;

import com.mcintyret.raft.address.Peer;

import java.util.List;

public class Configuration {

    private final List<Peer> peers;

    private final int majoritySize;

    public Configuration(List<Peer> peers) {
        this.peers = peers;
        majoritySize = 1 + (peers.size() / 2);
    }

    public List<Peer> getPeers() {
        return peers;
    }

    public int getMajoritySize() {
        return majoritySize;
    }
}
