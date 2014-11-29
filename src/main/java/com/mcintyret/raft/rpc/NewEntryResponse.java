package com.mcintyret.raft.rpc;

/**
 * User: tommcintyre
 * Date: 11/29/14
 */

// Unlike other messages this is only sent to the client.
public class NewEntryResponse {

    private final int redirect;

    private final boolean knownSuccess;

    public NewEntryResponse(int redirect, boolean knownSuccess) {
        this.redirect = redirect;
        this.knownSuccess = knownSuccess;
    }

    public int getRedirect() {
        return redirect;
    }

    public boolean isKnownSuccess() {
        return knownSuccess;
    }
}
