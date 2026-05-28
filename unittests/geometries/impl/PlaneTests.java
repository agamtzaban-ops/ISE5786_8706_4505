package geometries.impl;

import org.junit.jupiter.api.Test;
import geometries.api.Intersectable.Intersection;
import primitives.Point;
import primitives.Ray;
import primitives.Vector;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for class {@link geometries.impl.Plane}.
 */
class PlaneTests {

    private static final double DELTA = 0.000001;

    /** Error message for plane intersection failures */
    private static final String ERROR_PLANE_INTERSECTION = "Wrong plane intersection result";

    /** Simple plane for tests: orthogonal to Z-axis, passing through (0, 0, 1) */
    private static final Plane PLANE = new Plane(new Point(0, 0, 1), new Vector(0, 0, 1));

    // ================== STAGE 2 TESTS ==================

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

    // ================== STAGE 3 TESTS ==================

    /**
     * Test method for {@link geometries.impl.Plane#findIntersections(primitives.Ray)}.
     */
    @Test
    void testFindIntersections() {
        // ============ Equivalence Partitions Tests ==============

        // EP01: Ray intersects the plane (1 point)
        List<Point> result = PLANE.findIntersections(new Ray(new Point(0, 2, 0), new Vector(0, -2, 1)));
        assertNotNull(result, ERROR_PLANE_INTERSECTION);
        assertEquals(1, result.size(), ERROR_PLANE_INTERSECTION);
        assertEquals(List.of(new Point(0, 0, 1)), result, ERROR_PLANE_INTERSECTION);

        // EP02: Ray does not intersect the plane (0 points)
        assertNull(PLANE.findIntersections(new Ray(new Point(0, 2, 0), new Vector(0, 2, -1))),
                ERROR_PLANE_INTERSECTION);

        // =============== Boundary Values Tests ==================

        // **** Group 1: Ray is parallel to the plane
        // BV11: Ray is included in the plane
        assertNull(PLANE.findIntersections(new Ray(new Point(0, 1, 1), new Vector(1, 0, 0))),
                ERROR_PLANE_INTERSECTION);

        // BV12: Ray is not included in the plane
        assertNull(PLANE.findIntersections(new Ray(new Point(0, 1, 2), new Vector(1, 0, 0))),
                ERROR_PLANE_INTERSECTION);

        // **** Group 2: Ray is orthogonal to the plane
        // BV21: Ray starts before the plane
        result = PLANE.findIntersections(new Ray(new Point(0, 1, 0), new Vector(0, 0, 1)));
        assertNotNull(result, ERROR_PLANE_INTERSECTION);
        assertEquals(1, result.size(), ERROR_PLANE_INTERSECTION);
        assertEquals(List.of(new Point(0, 1, 1)), result, ERROR_PLANE_INTERSECTION);

        // BV22: Ray starts in the plane
        assertNull(PLANE.findIntersections(new Ray(new Point(0, 1, 1), new Vector(0, 0, 1))),
                ERROR_PLANE_INTERSECTION);

        // BV23: Ray starts after the plane
        assertNull(PLANE.findIntersections(new Ray(new Point(0, 1, 2), new Vector(0, 0, 1))),
                ERROR_PLANE_INTERSECTION);

        // **** Group 3: Ray is neither orthogonal nor parallel to the plane
        // BV31: Ray begins in the plane (P0 is in the plane, but not the ray)
        assertNull(PLANE.findIntersections(new Ray(new Point(0, 1, 1), new Vector(1, 1, 1))),
                ERROR_PLANE_INTERSECTION);

        // BV32: Ray begins in the plane exactly at the plane's reference point (Q)
        assertNull(PLANE.findIntersections(new Ray(new Point(0, 0, 1), new Vector(1, 1, 1))),
                ERROR_PLANE_INTERSECTION);
    }

    // ================== STAGE 8 TESTS ==================

    /**
     * Test method for {@link geometries.impl.Plane#calcIntersections(primitives.Ray, double)}.
     */
    @Test
    void testCalcIntersectionsMaxDistance() {
        // The constant PLANE is at Z=1, looking upwards (Normal: 0,0,1)
        // Ray starts at (0, 0, -1) and goes straight up towards the plane
        // The expected intersection point is (0, 0, 1) at distance t = 2
        Ray ray = new Ray(new Point(0, 0, -1), new Vector(0, 0, 1));
        Point expectedIntersection = new Point(0, 0, 1);

        // Case 1: maxDistance is smaller than the distance to the plane (1.0 < 2)
        assertNull(PLANE.calcIntersections(ray, 1.0),
                "Plane intersection should be filtered out if maxDistance is before the plane");

        // Case 2: maxDistance is exactly at the plane intersection point (2.0 == 2)
        List<Intersection> resultExact = PLANE.calcIntersections(ray, 2.0);
        assertNotNull(resultExact, "Should find 1 intersection when maxDistance is exactly on the plane");
        assertEquals(1, resultExact.size(), "Wrong number of intersections for exact maxDistance");
        assertEquals(expectedIntersection, resultExact.get(0).p, "Wrong intersection point for exact maxDistance");

        // Case 3: maxDistance is beyond the plane intersection point (3.0 > 2)
        List<Intersection> resultBeyond = PLANE.calcIntersections(ray, 3.0);
        assertNotNull(resultBeyond, "Should find 1 intersection when maxDistance is beyond the plane");
        assertEquals(1, resultBeyond.size(), "Wrong number of intersections for broad maxDistance");
        assertEquals(expectedIntersection, resultBeyond.get(0).p, "Wrong intersection point for broad maxDistance");
    }
}