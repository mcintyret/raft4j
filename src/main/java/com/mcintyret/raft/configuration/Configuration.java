package com.mcintyret.raft.configuration;

import com.mcintyret.raft.address.Peer;

import java.util.List;

public class Configuration {

    private final List<Peer> peers;

    public Configuration(List<Peer> peers) {
        this.peers = peers;
    }

    public List<Peer> getPeers() {
        return peers;
    }
}
