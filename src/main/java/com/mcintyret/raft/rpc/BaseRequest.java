package com.mcintyret.raft.rpc;

import com.mcintyret.raft.address.Address;

/**
 * User: tommcintyre
 * Date: 11/30/14
 */
public class BaseRequest extends BaseMessage {

    public BaseRequest(Address source, Address destination) {
        this(new Header(source, destination));
    }

    public BaseRequest(Header header) {
        super(header);
    }
}
