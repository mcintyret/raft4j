package com.mcintyret.raft.rpc;

import com.mcintyret.raft.client.Client;
import com.mcintyret.raft.client.ClientMessage;

/**
 * User: tommcintyre
 * Date: 11/29/14
 */

// Unlike other messages this is only sent to the client.
public class NewEntryResponse extends BaseResponse<NewEntryRequest> implements ClientMessage {

    private final int redirect;

    private final boolean knownSuccess;

    private final Client client;

    public NewEntryResponse(String uuid, Client client, int redirect, boolean knownSuccess) {
        super(uuid);
        this.client = client;
        this.redirect = redirect;
        this.knownSuccess = knownSuccess;
    }

    public int getRedirect() {
        return redirect;
    }

    public boolean isKnownSuccess() {
        return knownSuccess;
    }

    @Override
    public Client getClient() {
        return client;
    }
}
