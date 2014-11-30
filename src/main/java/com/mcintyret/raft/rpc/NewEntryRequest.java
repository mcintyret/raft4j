package com.mcintyret.raft.rpc;

/**
 * User: tommcintyre
 * Date: 11/29/14
 */

// Unlike the other message types, this comes from clients of the Raft cluster, not other members
public class NewEntryRequest implements RpcMessage {

    private final byte[] data;

    public NewEntryRequest(byte[] data) {
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public void visit(RpcMessageVisitor visitor) {
        visitor.onNewEntryRequest(this);
    }

}
