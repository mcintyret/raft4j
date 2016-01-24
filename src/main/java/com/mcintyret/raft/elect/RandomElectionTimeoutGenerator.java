package com.mcintyret.raft.elect;

import java.util.Random;

/**
 * User: tommcintyre
 * Date: 11/29/14
 */
public class RandomElectionTimeoutGenerator implements ElectionTimeoutGenerator {

    private final Random random;

    private final long minimum;

    private final int diff;

    public RandomElectionTimeoutGenerator(long minimum, long maximum) {
        this(new Random(), minimum, maximum);
    }

    public RandomElectionTimeoutGenerator(long seed, long minimum, long maximum) {
        this(new Random(seed), minimum, maximum);
    }

    private RandomElectionTimeoutGenerator(Random random, long minimum, long maximum) {
        if (minimum > maximum) {
            throw new IllegalArgumentException("Minimum must be less than or equal to maximum");
        }
        this.random = random;
        this.minimum = minimum;
        this.diff = (int) (maximum - minimum);
    }

    @Override
    public synchronized long nextElectionTimeout() {
        return System.currentTimeMillis() + minimum + random.nextInt(diff);
    }

    @Override
    public long minimumElectionTimeout() {
        return minimum;
    }
}
