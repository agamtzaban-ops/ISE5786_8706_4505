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

        // ============ Equivalence Partitions Tests ==============
        // EP01: Simple point and vector addition (Positive components)
        assertEquals(new Point(2, 4, 6), p1.add(new Vector(1, 2, 3)),
                "ERROR: Point + Vector does not work correctly for positive numbers");

        // EP02: Point and vector addition resulting in negative components
        assertEquals(new Point(-1, -2, -3), p1.add(new Vector(-2, -4, -6)),
                "ERROR: Point + Vector does not work correctly for negative results");

        // =============== Boundary Values Tests ==================
        // BV01: Addition resulting exactly in the origin point (0,0,0)
        assertEquals(new Point(0, 0, 0), p1.add(new Vector(-1, -2, -3)),
                "ERROR: Point + Vector resulting in origin does not work correctly");
    }

    /**
     * Test method for {@link primitives.Point#subtract(primitives.Point)}.
     */
    @Test
    void testSubtract() {
        Point p1 = new Point(1, 2, 3);
        Point p2 = new Point(2, 4, 6);

        // ============ Equivalence Partitions Tests ==============
        // EP01: Simple point subtraction (Resulting in positive vector)
        assertEquals(new Vector(1, 2, 3), p2.subtract(p1),
                "ERROR: Point - Point does not work correctly (positive result)");

        // EP02: Point subtraction (Resulting in negative vector)
        assertEquals(new Vector(-1, -2, -3), p1.subtract(p2),
                "ERROR: Point - Point does not work correctly (negative result)");

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
        Point p2 = new Point(-2, -2, -2);
        Point p3 = new Point(0.5, -1.5, 2);
        Point p4 = new Point(2.5, 1.5, -3);

        // ============ Equivalence Partitions Tests ==============
        // EP01: Distance squared with negative coordinates
        // dx = -2 - 1 = -3 -> 9
        // dy = -2 - 2 = -4 -> 16
        // dz = -2 - 3 = -5 -> 25
        // Sum = 9 + 16 + 25 = 50
        assertEquals(50d, p1.distanceSquared(p2), DELTA,
                "ERROR: distanceSquared() wrong result for integer points");

        // EP02: Distance squared with fractional/decimal coordinates
        // dx = 2.5 - 0.5 = 2 -> 4
        // dy = 1.5 - (-1.5) = 3 -> 9
        // dz = -3 - 2 = -5 -> 25
        // Sum = 4 + 9 + 25 = 38
        assertEquals(38d, p3.distanceSquared(p4), DELTA,
                "ERROR: distanceSquared() wrong result for fractional points");

        // =============== Boundary Values Tests ==================
        // BV01: Distance squared from a point to itself
        assertEquals(0d, p1.distanceSquared(p1), DELTA,
                "ERROR: distanceSquared() to itself should be 0");
    }

    /**
     * Test method for {@link primitives.Point#distance(primitives.Point)}.
     */
    @Test
    void testDistance() {
        Point p1 = new Point(1, 1, 1);
        Point p2 = new Point(3, 4, 7);
        Point p3 = new Point(0.5, 0, 0);
        Point p4 = new Point(2, 2, 0);

        // ============ Equivalence Partitions Tests ==============
        // EP01: Distance with completely different integers
        // dx=2 (4), dy=3 (9), dz=6 (36) -> Sum = 49 -> sqrt = 7
        assertEquals(7d, p1.distance(p2), DELTA,
                "ERROR: distance() wrong result for integer points");

        // EP02: Distance with fractional points
        // dx=1.5 (2.25), dy=2 (4), dz=0 -> Sum = 6.25 -> sqrt = 2.5
        assertEquals(2.5d, p3.distance(p4), DELTA,
                "ERROR: distance() wrong result for fractional points");

        // =============== Boundary Values Tests ==================
        // BV01: Distance from a point to itself
        assertEquals(0d, p1.distance(p1), DELTA, "ERROR: distance() to itself should be 0");
    }
}