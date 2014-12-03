package com.mcintyret.raft.rpc;

import com.mcintyret.raft.address.Address;

import java.util.UUID;

/**
 * User: tommcintyre
 * Date: 12/3/14
 */
public class Header {

    private final Address source;

    private final Address destination;

    private final String ruuid;

    public Header(Address source, Address destination) {
        this(source, destination, UUID.randomUUID().toString());
    }

    public Header(Address source, Address destination, String ruuid) {
        this.source = source;
        this.destination = destination;
        this.ruuid = ruuid;
    }

    public Address getSource() {
        return source;
    }

    public Address getDestination() {
        return destination;
    }

    public String getRuuid() {
        return ruuid;
    }
}
