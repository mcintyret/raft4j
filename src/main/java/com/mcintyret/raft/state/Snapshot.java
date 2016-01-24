package com.mcintyret.raft.state;

import java.nio.ByteBuffer;

import com.mcintyret.raft.core.IndexedAndTermed;

public interface Snapshot extends IndexedAndTermed {

    ByteBuffer getData();

}
