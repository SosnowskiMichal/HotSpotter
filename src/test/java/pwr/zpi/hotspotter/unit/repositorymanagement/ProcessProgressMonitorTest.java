package pwr.zpi.hotspotter.unit.repositorymanagement;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import pwr.zpi.hotspotter.repositorymanagement.operation.ProcessProgressMonitor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class ProcessProgressMonitorTest {

    private ProcessProgressMonitor monitor;
    private ListAppender<ILoggingEvent> listAppender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        monitor = new ProcessProgressMonitor(10);

        logger = (Logger) LoggerFactory.getLogger(ProcessProgressMonitor.class);
        logger.setLevel(Level.DEBUG);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(listAppender);
        listAppender.stop();
    }

    @Test
    void startsProcessWithTotalTasks() {
        monitor.start(5);

        System.out.println(listAppender.list.stream().toList());

        boolean found = listAppender.list.stream()
                .anyMatch(e -> "Starting process with 5 tasks".equals(e.getFormattedMessage()) && e.getLevel() == Level.DEBUG);
        assertTrue(found);
    }

    @Test
    void beginsTaskWithTitleAndTotalWork() {
        monitor.beginTask("Task 1", 100);

        boolean found = listAppender.list.stream()
                .anyMatch(e -> "Process started: Task 1 (Total work: 100)".equals(e.getFormattedMessage()) && e.getLevel() == Level.DEBUG);
        assertTrue(found);
    }

    @Test
    void updatesProgressAndLogsWhenIntervalReached() {
        monitor.beginTask("Task 1", 100);
        monitor.update(15);
        monitor.update(15);
        monitor.update(15);

        boolean found = listAppender.list.stream()
                .anyMatch(e -> "Git operation progress: Task 1 (30%)".equals(e.getFormattedMessage()) && e.getLevel() == Level.INFO);
        assertTrue(found);
    }

    @Test
    void doesNotLogProgressIfIntervalNotReached() {
        monitor.beginTask("Task 1", 100);
        monitor.update(5);

        boolean found = listAppender.list.stream()
                .anyMatch(e -> e.getFormattedMessage().startsWith("Git operation progress:"));
        assertFalse(found);
    }

    @Test
    void endsTaskAndLogsCompletion() {
        monitor.beginTask("Task 1", 100);
        monitor.endTask();

        boolean found = listAppender.list.stream()
                .anyMatch(e -> "Process finished: Task 1 (Total work: 100 (100%))".equals(e.getFormattedMessage()) && e.getLevel() == Level.DEBUG);
        assertTrue(found);
    }

    @Test
    void doesNotLogProgressIfTotalWorkIsZero() {
        monitor.beginTask("Task 1", 0);
        monitor.update(10);

        boolean found = listAppender.list.stream()
                .anyMatch(e -> e.getFormattedMessage().startsWith("Git operation progress:"));
        assertFalse(found);
    }

    @Test
    void isCancelledAlwaysReturnsFalse() {
        assertFalse(monitor.isCancelled());
    }
}