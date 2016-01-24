package com.mcintyret.raft.state;

import com.mcintyret.raft.core.LogEntry;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * User: tommcintyre
 * Date: 12/1/14
 */
public class FileWritingStateMachine extends BaseStateMachine {

    private final BufferedWriter writer;

    public FileWritingStateMachine(String fileName) {
//        super(new LogCountingSnapshotStrategy(5));
        super(SimpleSnapshotStrategy.NEVER);
        try {
            Path path = Paths.get(fileName);
            if (!Files.exists(path)) {
                Path parentDir = path.getParent();
                if (!Files.exists(parentDir)) {
                    Files.createDirectories(parentDir);
                }
                Files.createFile(path);
            }
            this.writer = Files.newBufferedWriter(path);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    protected Snapshot takeSnapshot() {
        return null; // TODO: implement
    }

    @Override
    protected void applyInternal(LogEntry entry) {
        try {
            writer.write(entry.getTerm() + ": " + entry.getIndex() + ": " + new String(entry.getData()));
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
