package com.mcintyret.raft.address;

import com.mcintyret.raft.rpc.BaseRequest;
import com.mcintyret.raft.rpc.Header;

public interface Addressable {

    Address getAddress();

    default Header headerFor(Peer recipient) {
        return new Header(getAddress(), recipient);
    }

    default Header headerFor(BaseRequest request) {
        return new Header(getAddress(), request.getHeader().getSource(), request.getHeader().getRuuid());
    }

}
