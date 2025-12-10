package pwr.zpi.hotspotter.unit.fileanalysis.complexity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pwr.zpi.hotspotter.fileanalysis.complexity.component.LizardStrategy;

import static org.junit.jupiter.api.Assertions.*;

class LizardStrategyTest {

    private LizardStrategy lizardStrategy;

    @BeforeEach
    void setUp() {
        lizardStrategy = new LizardStrategy();
    }

    @Test
    void isSupported_ShouldReturnTrueForSupportedExtensions() {
        // C/C++ family
        assertTrue(lizardStrategy.isSupported("c"));
        assertTrue(lizardStrategy.isSupported("cpp"));
        assertTrue(lizardStrategy.isSupported("cc"));
        assertTrue(lizardStrategy.isSupported("h"));
        assertTrue(lizardStrategy.isSupported("hpp"));
        assertTrue(lizardStrategy.isSupported("cxx"));
        assertTrue(lizardStrategy.isSupported("m"));
        assertTrue(lizardStrategy.isSupported("mm"));

        // JVM languages
        assertTrue(lizardStrategy.isSupported("java"));
        assertTrue(lizardStrategy.isSupported("kt"));
        assertTrue(lizardStrategy.isSupported("kts"));
        assertTrue(lizardStrategy.isSupported("scala"));

        // .NET
        assertTrue(lizardStrategy.isSupported("cs"));

        // JavaScript/TypeScript
        assertTrue(lizardStrategy.isSupported("js"));
        assertTrue(lizardStrategy.isSupported("jsx"));
        assertTrue(lizardStrategy.isSupported("ts"));
        assertTrue(lizardStrategy.isSupported("tsx"));

        // Other languages
        assertTrue(lizardStrategy.isSupported("py"));
        assertTrue(lizardStrategy.isSupported("rb"));
        assertTrue(lizardStrategy.isSupported("php"));
        assertTrue(lizardStrategy.isSupported("swift"));
        assertTrue(lizardStrategy.isSupported("lua"));
        assertTrue(lizardStrategy.isSupported("rs"));
        assertTrue(lizardStrategy.isSupported("go"));
        assertTrue(lizardStrategy.isSupported("sol"));
        assertTrue(lizardStrategy.isSupported("gd"));
    }

    @Test
    void isSupported_ShouldBeCaseInsensitive() {
        assertTrue(lizardStrategy.isSupported("JAVA"));
        assertTrue(lizardStrategy.isSupported("Java"));
        assertTrue(lizardStrategy.isSupported("CPP"));
        assertTrue(lizardStrategy.isSupported("Py"));
        assertTrue(lizardStrategy.isSupported("TSX"));
    }

    @Test
    void isSupported_ShouldReturnFalseForUnsupportedExtensions() {
        assertFalse(lizardStrategy.isSupported("txt"));
        assertFalse(lizardStrategy.isSupported("md"));
        assertFalse(lizardStrategy.isSupported("json"));
        assertFalse(lizardStrategy.isSupported("xml"));
        assertFalse(lizardStrategy.isSupported("yml"));
        assertFalse(lizardStrategy.isSupported("yaml"));
        assertFalse(lizardStrategy.isSupported(""));
        assertFalse(lizardStrategy.isSupported("unknown"));
    }
}