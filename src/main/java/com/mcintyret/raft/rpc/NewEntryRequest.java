package com.mcintyret.raft.rpc;

import com.mcintyret.raft.client.Client;
import com.mcintyret.raft.client.ClientMessage;

/**
 * User: tommcintyre
 * Date: 11/29/14
 */

// Unlike the other message types, this comes from clients of the Raft cluster, not other members
public class NewEntryRequest extends BaseRequest implements RpcMessage, ClientMessage {

    private final Client client;

    private final byte[] data;

    public NewEntryRequest(Client client, byte[] data) {
        this.client = client;
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public void visit(RpcMessageVisitor visitor) {
        visitor.onNewEntryRequest(this);
    }

    @Override
    public Client getClient() {
        return client;
    }
}
