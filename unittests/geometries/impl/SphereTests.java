package geometries.impl;

import org.junit.jupiter.api.Test;
import primitives.Point;
import primitives.Ray;
import primitives.Vector;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for class {@link geometries.impl.Sphere}.
 */
class SphereTests {

    private static final Point P100 = new Point(1, 0, 0);
    private static final Point P01 = new Point(-1, 0, 0);
    private static final Vector V310 = new Vector(3, 1, 0);
    private static final Vector V110 = new Vector(1, 1, 0);

    /** Error message for sphere intersection failures */
    private static final String ERROR_SPHERE_INTERSECTION = "Wrong sphere intersection result";

    /** Sphere used in some tests */
    private static final Sphere SPHERE = new Sphere(P100, 1d);

    // ================== STAGE 2 TESTS ==================

    /**
     * Test method for {@link geometries.impl.Sphere#getNormal(primitives.Point)}.
     */
    @Test
    void testGetNormal() {
        Sphere sphere = new Sphere(new Point(0, 0, 0), 5.0);

        // ============ Equivalence Partitions Tests ==============
        // EP01: Normal on the surface of the sphere
        assertEquals(new Vector(0, 0, 1), sphere.getNormal(new Point(0, 0, 5)),
                "ERROR: Sphere getNormal() wrong result");
    }

    // ================== STAGE 3 TESTS ==================

    /**
     * Test method for {@link geometries.impl.Sphere#findIntersections(primitives.Ray)}.
     */
    @Test
    void testFindIntersections() {
        // ============ Equivalence Partitions Tests ==============
        // EP01: Ray's line is outside the sphere (0 points)
        assertNull(SPHERE.findIntersections(new Ray(P01, V110)), ERROR_SPHERE_INTERSECTION);

        // EP02: Ray starts before and crosses the sphere (2 points)
        Point p1 = new Point(0.0651530771650466, 0.355051025721682, 0);
        Point p2 = new Point(1.53484692283495, 0.844948974278318, 0);
        List<Point> result = SPHERE.findIntersections(new Ray(P01, V310));
        assertNotNull(result, ERROR_SPHERE_INTERSECTION);
        assertEquals(2, result.size(), ERROR_SPHERE_INTERSECTION);
        assertTrue(result.contains(p1) && result.contains(p2), ERROR_SPHERE_INTERSECTION);

        // EP03: Ray starts inside the sphere (1 point)
        assertEquals(List.of(new Point(2, 0, 0)),
                SPHERE.findIntersections(new Ray(new Point(1.5, 0, 0), new Vector(1, 0, 0))),
                ERROR_SPHERE_INTERSECTION);

        // EP04: Ray starts after the sphere (0 points)
        assertNull(SPHERE.findIntersections(new Ray(new Point(3, 0, 0), new Vector(1, 0, 0))),
                ERROR_SPHERE_INTERSECTION);

        // =============== Boundary Values Tests ==================

        // **** Group 1: Ray's line crosses the sphere (but not the center)
        // BV11: Ray starts at sphere and goes inside (1 point)
        assertEquals(List.of(new Point(1.6, 0.8, 0)),
                SPHERE.findIntersections(new Ray(new Point(0, 0, 0), new Vector(2, 1, 0))),
                ERROR_SPHERE_INTERSECTION);

        // BV12: Ray starts at sphere and goes outside (0 points)
        assertNull(SPHERE.findIntersections(new Ray(new Point(0, 0, 0), new Vector(-1, 0, 0))),
                ERROR_SPHERE_INTERSECTION);

        // **** Group 2: Ray's line goes through the center
        // BV21: Ray starts before the sphere (2 points)
        result = SPHERE.findIntersections(new Ray(new Point(-1, 0, 0), new Vector(1, 0, 0)));
        assertNotNull(result, ERROR_SPHERE_INTERSECTION);
        assertEquals(2, result.size(), ERROR_SPHERE_INTERSECTION);
        assertTrue(result.contains(new Point(0, 0, 0)) && result.contains(new Point(2, 0, 0)),
                ERROR_SPHERE_INTERSECTION);

        // BV22: Ray starts at sphere and goes inside (1 point)
        assertEquals(List.of(new Point(2, 0, 0)),
                SPHERE.findIntersections(new Ray(new Point(0, 0, 0), new Vector(1, 0, 0))),
                ERROR_SPHERE_INTERSECTION);

        // BV23: Ray starts inside (1 point)
        assertEquals(List.of(new Point(2, 0, 0)),
                SPHERE.findIntersections(new Ray(new Point(0.5, 0, 0), new Vector(1, 0, 0))),
                ERROR_SPHERE_INTERSECTION);

        // BV24: Ray starts at the center (1 point)
        assertEquals(List.of(new Point(2, 0, 0)),
                SPHERE.findIntersections(new Ray(new Point(1, 0, 0), new Vector(1, 0, 0))),
                ERROR_SPHERE_INTERSECTION);

        // BV25: Ray starts at sphere and goes outside (0 points)
        assertNull(SPHERE.findIntersections(new Ray(new Point(2, 0, 0), new Vector(1, 0, 0))),
                ERROR_SPHERE_INTERSECTION);

        // BV26: Ray starts after sphere (0 points)
        assertNull(SPHERE.findIntersections(new Ray(new Point(3, 0, 0), new Vector(1, 0, 0))),
                ERROR_SPHERE_INTERSECTION);

        // **** Group 3: Ray's line is tangent to the sphere (all tests 0 points)
        // BV31: Ray starts before the tangent point
        assertNull(SPHERE.findIntersections(new Ray(new Point(0, 1, 0), new Vector(1, 0, 0))),
                ERROR_SPHERE_INTERSECTION);

        // BV32: Ray starts at the tangent point
        assertNull(SPHERE.findIntersections(new Ray(new Point(1, 1, 0), new Vector(1, 0, 0))),
                ERROR_SPHERE_INTERSECTION);

        // BV33: Ray starts after the tangent point
        assertNull(SPHERE.findIntersections(new Ray(new Point(2, 1, 0), new Vector(1, 0, 0))),
                ERROR_SPHERE_INTERSECTION);

        // **** Group 4: Special cases
        // BV41: Ray's line is outside, ray is orthogonal to ray start to sphere's center line
        assertNull(SPHERE.findIntersections(new Ray(new Point(3, 0, 0), new Vector(0, 1, 0))),
                ERROR_SPHERE_INTERSECTION);

        // BV42: Ray starts inside, ray is orthogonal to ray start to sphere's center line
        assertEquals(List.of(new Point(1.5, 0.8660254037844386, 0)),
                SPHERE.findIntersections(new Ray(new Point(1.5, 0, 0), new Vector(0, 1, 0))),
                ERROR_SPHERE_INTERSECTION);
    }
}