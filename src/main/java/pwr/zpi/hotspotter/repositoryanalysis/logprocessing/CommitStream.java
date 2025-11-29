package pwr.zpi.hotspotter.repositoryanalysis.logprocessing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.model.Commit;

import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
public class CommitStream implements AutoCloseable {

    private static final int WAIT_TIMEOUT_SECONDS = 5;

    private final Process process;
    private final Stream<Commit> commitStream;

    public Stream<Commit> getStream() {
        return commitStream;
    }

    @Override
    public void close() {
        try {
            if (commitStream != null) {
                commitStream.close();
            }
        } catch (Exception e) {
            log.error("Error closing commit stream: {}", e.getMessage());
        }

        if (process != null && process.isAlive()) {
            process.destroyForcibly();
            try {
                process.waitFor(WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while waiting for process to terminate");
            }
        }
    }

}
