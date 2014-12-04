package com.mcintyret.raft.state;

import java.nio.ByteBuffer;

public interface Snapshot {

    long getLastLogIndex();

    long getLastLogTerm();

    ByteBuffer getData();

}
