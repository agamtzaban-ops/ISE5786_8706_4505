package primitives;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for class {@link primitives.Ray}.
 */
class RayTests {

    /**
     * Test method for constructor {@link primitives.Ray#Ray(primitives.Point, primitives.Vector)}.
     */
    @Test
    void testConstructor() {
        // ============ Equivalence Partitions Tests ==============
        // EP01: Test that the constructor normalizes the direction vector
        Ray ray = new Ray(new Point(1, 2, 3), new Vector(0, 3, 4));
        assertEquals(1d, ray.direction().length(), 0.000001,
                "ERROR: Ray constructor does not normalize the direction vector");
    }
}