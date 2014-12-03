package com.mcintyret.raft.rpc;

/**
 * User: tommcintyre
 * Date: 12/3/14
 */
public abstract class BaseMessage implements Message {

    private final Header header;

    protected BaseMessage(Header header) {
        this.header = header;
    }

    @Override
    public Header getHeader() {
        return header;
    }
}
