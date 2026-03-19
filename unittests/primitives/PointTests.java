package primitives;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for class {@link primitives.Point}.
 */
class PointTests {

    /** Delta value for accuracy when comparing double values. */
    private static final double DELTA = 0.000001;

    /**
     * Test method for {@link primitives.Point#add(primitives.Vector)}.
     */
    @Test
    void testAdd() {
        Point p1 = new Point(1, 2, 3);
        Vector v = new Vector(-1, -2, -3);

        // ============ Equivalence Partitions Tests ==============
        // EP01: Simple point and vector addition
        assertEquals(new Point(0, 0, 0), p1.add(v), "ERROR: Point + Vector does not work correctly");
    }

    /**
     * Test method for {@link primitives.Point#subtract(primitives.Point)}.
     */
    @Test
    void testSubtract() {
        Point p1 = new Point(1, 2, 3);
        Point p2 = new Point(2, 4, 6);

        // ============ Equivalence Partitions Tests ==============
        // EP01: Simple point subtraction
        assertEquals(new Vector(1, 2, 3), p2.subtract(p1), "ERROR: Point - Point does not work correctly");

        // =============== Boundary Values Tests ==================
        // BV01: Subtraction of a point from itself
        assertThrows(IllegalArgumentException.class, () -> p1.subtract(p1),
                "ERROR: Point - itself should throw an exception (zero vector)");
    }

    /**
     * Test method for {@link primitives.Point#distanceSquared(primitives.Point)}.
     */
    @Test
    void testDistanceSquared() {
        Point p1 = new Point(1, 2, 3);
        Point p2 = new Point(1, 5, 7);

        // ============ Equivalence Partitions Tests ==============
        // EP01: Simple distance squared calculation
        // Distance squared = (1-1)^2 + (5-2)^2 + (7-3)^2 = 0 + 9 + 16 = 25
        assertEquals(25d, p1.distanceSquared(p2), DELTA, "ERROR: distanceSquared() wrong result");

        // =============== Boundary Values Tests ==================
        // BV01: Distance squared from a point to itself
        assertEquals(0d, p1.distanceSquared(p1), DELTA, "ERROR: distanceSquared() to itself should be 0");
    }

    /**
     * Test method for {@link primitives.Point#distance(primitives.Point)}.
     */
    @Test
    void testDistance() {
        Point p1 = new Point(1, 2, 3);
        Point p2 = new Point(1, 5, 7);

        // ============ Equivalence Partitions Tests ==============
        // EP01: Simple distance calculation
        assertEquals(5d, p1.distance(p2), DELTA, "ERROR: distance() wrong result");

        // =============== Boundary Values Tests ==================
        // BV01: Distance from a point to itself
        assertEquals(0d, p1.distance(p1), DELTA, "ERROR: distance() to itself should be 0");
    }
}