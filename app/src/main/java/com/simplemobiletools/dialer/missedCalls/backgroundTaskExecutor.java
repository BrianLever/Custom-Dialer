package com.simplemobiletools.dialer.missedCalls;

import java.util.concurrent.Executor;

public class backgroundTaskExecutor implements Executor {
    @Override
    public void execute(Runnable runnable) {
        new Thread(runnable).start();
    }
}
