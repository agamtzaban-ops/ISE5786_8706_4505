package geometries.impl;

import org.junit.jupiter.api.Test;
import primitives.Point;
import primitives.Vector;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for class {@link geometries.impl.Plane}.
 */
class PlaneTests {

    private static final double DELTA = 0.000001;

    @Test
    void testConstructors() {
        // ============ Equivalence Partitions Tests ==============
        // EP01: Constructor with 3 valid points
        assertDoesNotThrow(() -> new Plane(new Point(0, 0, 1), new Point(1, 0, 0), new Point(0, 1, 0)),
                "ERROR: Failed constructing a correct plane");

        // EP02: Constructor with point and vector (checking normalization)
        Plane plane = new Plane(new Point(1, 1, 1), new Vector(0, 3, 4));
        assertEquals(1d, plane.getNormal(new Point(1, 1, 1)).length(), DELTA,
                "ERROR: Constructor with Point and Vector does not normalize the normal");

        // =============== Boundary Values Tests ==================
        // BV01: Constructor with 3 points - first and second points coincide
        assertThrows(IllegalArgumentException.class,
                () -> new Plane(new Point(1, 0, 0), new Point(1, 0, 0), new Point(0, 1, 0)),
                "ERROR: Constructor with 3 points should throw exception when two points are the same");

        // BV02: Constructor with 3 points - all three points coincide
        assertThrows(IllegalArgumentException.class,
                () -> new Plane(new Point(1, 0, 0), new Point(1, 0, 0), new Point(1, 0, 0)),
                "ERROR: Constructor with 3 points should throw exception when all points are the same");

        // BV03: Constructor with 3 points - points are on the same line
        assertThrows(IllegalArgumentException.class,
                () -> new Plane(new Point(1, 1, 1), new Point(2, 2, 2), new Point(3, 3, 3)),
                "ERROR: Constructor with 3 points should throw exception when points are on the same line");
    }

    @Test
    void testGetNormal() {
        // Create a simple plane on the X-Y axes
        Plane plane = new Plane(new Point(0, 0, 0), new Point(1, 0, 0), new Point(0, 1, 0));
        Vector expectedNormal = new Vector(0, 0, 1);

        // ============ Equivalence Partitions Tests ==============
        // EP01: Get normal at a point on the plane which is not the reference point
        assertEquals(expectedNormal, plane.getNormal(new Point(1, 1, 0)),
                "ERROR: getNormal() wrong result for Plane (EP)");

        // =============== Boundary Values Tests ==================
        // BV01: Get normal at the reference point itself (q0)
        assertEquals(expectedNormal, plane.getNormal(new Point(0, 0, 0)),
                "ERROR: getNormal() wrong result for Plane (BV)");
    }
}