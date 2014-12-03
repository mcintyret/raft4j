package com.mcintyret.raft.rpc;

import com.mcintyret.raft.address.Peer;

/**
 * User: tommcintyre
 * Date: 11/29/14
 */

// Unlike other messages this is only sent to the client.
public class NewEntryResponse extends BaseResponse<NewEntryRequest> {

    private final Peer redirect;

    public NewEntryResponse(Header header, Peer redirect) {
        super(header);
        this.redirect = redirect;
    }

    public Peer getRedirect() {
        return redirect;
    }

}
