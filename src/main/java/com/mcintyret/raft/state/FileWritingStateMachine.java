package com.mcintyret.raft.state;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * User: tommcintyre
 * Date: 12/1/14
 */
public class FileWritingStateMachine implements StateMachine {

    private final BufferedWriter writer;

    public FileWritingStateMachine(String fileName) {
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
    public void apply(long index, byte[] data) {
        try {
            writer.write(index + ": " + new String(data));
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
