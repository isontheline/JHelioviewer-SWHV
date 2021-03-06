package org.helioviewer.jhv.view.j2k;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.helioviewer.jhv.threads.JHVThread;
import org.helioviewer.jhv.view.j2k.image.DecodeParams;

class DecodeExecutor {

    private final ArrayBlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<>(1);
    // no need to intercept exceptions
    private final ExecutorService executor = new ThreadPoolExecutor(1, 1, 10000L, TimeUnit.MILLISECONDS, blockingQueue,
            new JHVThread.NamedThreadFactory("Decoder"),
            new ThreadPoolExecutor.DiscardPolicy());

    void decode(J2KView view, DecodeParams decodeParams) {
        blockingQueue.poll();
        executor.execute(new J2KDecoder(view, decodeParams, false));
    }

    void abolish() {
        try {
            blockingQueue.poll();
            executor.execute(new J2KDecoder(null, null, true));
            executor.shutdown();
            while (!executor.awaitTermination(1000L, TimeUnit.MILLISECONDS)) ;
        } catch (Exception ignore) {
        }
    }

}
