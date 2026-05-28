package geometries.impl;

import org.junit.jupiter.api.Test;
import geometries.api.Intersectable.Intersection;
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

    // ================== STAGE 8 TESTS ==================

    /**
     * Test method for {@link geometries.impl.Sphere#calcIntersections(primitives.Ray, double)}.
     */
    @Test
    void testCalcIntersectionsMaxDistance() {
        // Ray crosses the sphere twice through the center
        // Ray origin: (-1, 0, 0), direction: (1, 0, 0)
        // Sphere center: (1, 0, 0), radius: 1
        // Expected intersections:
        // P1: (0, 0, 0) at distance t1 = 1
        // P2: (2, 0, 0) at distance t2 = 3
        Ray ray = new Ray(new Point(-1, 0, 0), new Vector(1, 0, 0));
        Point p1 = new Point(0, 0, 0);
        Point p2 = new Point(2, 0, 0);

        // Q1: maxDistance is smaller than the distance to the first intersection point (0.5 < 1)
        assertNull(SPHERE.calcIntersections(ray, 0.5),
                "Q1: Intersections should be filtered out if maxDistance is before the first intersection");

        // Q2: maxDistance is exactly at the first intersection point (1.0 == 1)
        List<Intersection> resultQ2 = SPHERE.calcIntersections(ray, 1.0);
        assertNotNull(resultQ2, "Q2: Should find 1 intersection");
        assertEquals(1, resultQ2.size(), "Q2: Wrong number of intersections");
        assertEquals(p1, resultQ2.get(0).p, "Q2: Wrong intersection point");

        // Q3: maxDistance is between the first and second intersection points (2.0)
        List<Intersection> resultQ3 = SPHERE.calcIntersections(ray, 2.0);
        assertNotNull(resultQ3, "Q3: Should find 1 intersection");
        assertEquals(1, resultQ3.size(), "Q3: Wrong number of intersections");
        assertEquals(p1, resultQ3.get(0).p, "Q3: Wrong intersection point");

        // Q4: maxDistance is exactly at the second intersection point (3.0 == 3)
        List<Intersection> resultQ4 = SPHERE.calcIntersections(ray, 3.0);
        assertNotNull(resultQ4, "Q4: Should find 2 intersections");
        assertEquals(2, resultQ4.size(), "Q4: Wrong number of intersections");
        assertTrue((resultQ4.get(0).p.equals(p1) && resultQ4.get(1).p.equals(p2)) ||
                        (resultQ4.get(0).p.equals(p2) && resultQ4.get(1).p.equals(p1)),
                "Q4: Missing expected intersection points");

        // Q5: maxDistance is beyond the second intersection point (4.0 > 3)
        List<Intersection> resultQ5 = SPHERE.calcIntersections(ray, 4.0);
        assertNotNull(resultQ5, "Q5: Should find 2 intersections");
        assertEquals(2, resultQ5.size(), "Q5: Wrong number of intersections");
        assertTrue((resultQ5.get(0).p.equals(p1) && resultQ5.get(1).p.equals(p2)) ||
                        (resultQ5.get(0).p.equals(p2) && resultQ5.get(1).p.equals(p1)),
                "Q5: Missing expected intersection points");

        // Q6: maxDistance is zero (0.0)
        assertNull(SPHERE.calcIntersections(ray, 0.0),
                "Q6: Intersections should be null if maxDistance is zero");
    }
}