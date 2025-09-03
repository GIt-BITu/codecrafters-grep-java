package grep.tests;

import grep.cli.HandlerRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class HandlerRegistryTest {

  @Test
  void testERegexHandlerExists() {
    assertTrue(
        HandlerRegistry.loadAll().stream()
            .anyMatch(h -> h.supports(new String[] {"-E", "foo"}, 0)));
  }

  @Test
  void testRecursiveHandlerExists() {
    assertTrue(
        HandlerRegistry.loadAll().stream().anyMatch(h -> h.supports(new String[] {"-r"}, 0)));
  }

  @Test
  void testUnknownFlagReturnsNull() {
    assertFalse(
        HandlerRegistry.loadAll().stream().anyMatch(h -> h.supports(new String[] {"--foo"}, 0)));
  }
}
