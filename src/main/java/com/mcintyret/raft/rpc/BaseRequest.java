package com.mcintyret.raft.rpc;

import java.util.UUID;

/**
 * User: tommcintyre
 * Date: 11/30/14
 */
public class BaseRequest implements Message {

    private final String uuid = UUID.randomUUID().toString();

    @Override
    public String getRequestUid() {
        return uuid;
    }
}
