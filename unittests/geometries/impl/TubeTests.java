package geometries.impl;

import org.junit.jupiter.api.Test;
import geometries.api.Intersectable.Intersection;
import primitives.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for class {@link geometries.impl.Tube}.
 */
class TubeTests {

    /**
     * Test method for {@link geometries.impl.Tube#getNormal(primitives.Point)}.
     */
    @Test
    void testGetNormal() {
        // Updated order: Radius first, then Ray
        Tube tube = new Tube(1.0, new Ray(new Point(0, 0, 0), new Vector(0, 0, 1)));
        Vector expectedNormal = new Vector(1, 0, 0);

        // ============ Equivalence Partitions Tests ==============
        // EP01: Normal at a point opposite the axis ray (t > 0)
        assertEquals(expectedNormal, tube.getNormal(new Point(1, 0, 1)),
                "ERROR: getNormal() wrong result for Tube (EP1 - opposite axis ray)");

        // EP02: Normal at a point opposite the back of the axis ray (t < 0)
        assertEquals(expectedNormal, tube.getNormal(new Point(1, 0, -1)),
                "ERROR: getNormal() wrong result for Tube (EP2 - opposite back of axis ray)");

        // =============== Boundary Values Tests ==================
        // BV01: Normal at a point opposite the head of the axis ray (t = 0)
        assertEquals(expectedNormal, tube.getNormal(new Point(1, 0, 0)),
                "ERROR: getNormal() wrong result for Tube (BV1 - opposite head of ray)");
    }

    /**
     * Test method for {@link geometries.impl.Tube#findIntersections(primitives.Ray)}.
     */
    @Test
    void testFindIntersections() {
        Ray axis = new Ray(new Point(0, 0, 1), new Vector(0, 0, 1));
        // Updated order: Radius (1.0) first, then Ray (axis)
        Tube tube = new Tube(1.0, axis);

        // ============ Equivalence Partitions Tests ==============

        // EP01: Ray is outside and misses the tube (0 points)
        assertNull(tube.findIntersections(new Ray(new Point(2, 0, 0), new Vector(0, 1, 0))),
                "Ray should miss the tube");

        // EP02: Ray starts outside and crosses the tube (2 points)
        List<Point> result = tube.findIntersections(new Ray(new Point(2, 0, 1), new Vector(-1, 0, 0)));
        assertNotNull(result, "Ray should hit the tube");
        assertEquals(2, result.size(), "Should be 2 intersection points");

        // EP03: Ray starts inside the tube (1 point)
        List<Point> resultInside = tube.findIntersections(new Ray(new Point(0.5, 0, 1), new Vector(1, 0, 0)));
        assertNotNull(resultInside, "Ray starts inside - should hit");
        assertEquals(1, resultInside.size(), "Ray starts inside, should have 1 point");

        // =============== Boundary Values Tests ==================

        // BV01: Ray is parallel to the axis and inside (0 points)
        assertNull(tube.findIntersections(new Ray(new Point(0.5, 0, 0), new Vector(0, 0, 1))),
                "Parallel ray inside should have no intersections");

        // BV02: Ray is parallel to the axis and outside (0 points)
        assertNull(tube.findIntersections(new Ray(new Point(2, 0, 0), new Vector(0, 0, 1))),
                "Parallel ray outside should have no intersections");

        // BV03: Ray is orthogonal to the axis and starts at the center (1 point)
        List<Point> resultCenter = tube.findIntersections(new Ray(new Point(0, 0, 1), new Vector(1, 0, 0)));
        assertNotNull(resultCenter, "Ray from center - should hit");
        assertEquals(1, resultCenter.size(), "Ray from center should hit once");
    }

    // ================== STAGE 8 TESTS ==================

    /**
     * Test method for {@link geometries.impl.Tube#calcIntersections(primitives.Ray, double)}.
     */
    @Test
    void testCalcIntersectionsMaxDistance() {
        Ray axis = new Ray(new Point(0, 0, 1), new Vector(0, 0, 1));
        Tube tube = new Tube(1.0, axis);

        // Ray starts at (2, 0, 1) and moves towards the tube in direction (-1, 0, 0)
        // Expected intersections:
        // P1: (1, 0, 1) at distance t1 = 1.0
        // P2: (-1, 0, 1) at distance t2 = 3.0
        Ray ray = new Ray(new Point(2, 0, 1), new Vector(-1, 0, 0));
        Point p1 = new Point(1, 0, 1);
        Point p2 = new Point(-1, 0, 1);

        // Case 1: maxDistance is smaller than the distance to the first intersection point (0.5 < 1)
        assertNull(tube.calcIntersections(ray, 0.5),
                "Intersections should be filtered out if maxDistance is before the first intersection");

        // Case 2: maxDistance is exactly at the first intersection point (1.0 == 1)
        List<Intersection> resultExactFirst = tube.calcIntersections(ray, 1.0);
        assertNotNull(resultExactFirst, "Should find 1 intersection");
        assertEquals(1, resultExactFirst.size(), "Wrong number of intersections");
        assertEquals(p1, resultExactFirst.get(0).p, "Wrong intersection point");

        // Case 3: maxDistance is between the first and second intersection points (2.0)
        List<Intersection> resultBetween = tube.calcIntersections(ray, 2.0);
        assertNotNull(resultBetween, "Should find 1 intersection");
        assertEquals(1, resultBetween.size(), "Wrong number of intersections");
        assertEquals(p1, resultBetween.get(0).p, "Wrong intersection point");

        // Case 4: maxDistance is exactly at the second intersection point (3.0 == 3)
        List<Intersection> resultExactSecond = tube.calcIntersections(ray, 3.0);
        assertNotNull(resultExactSecond, "Should find 2 intersections");
        assertEquals(2, resultExactSecond.size(), "Wrong number of intersections");
        assertTrue((resultExactSecond.get(0).p.equals(p1) && resultExactSecond.get(1).p.equals(p2)) ||
                        (resultExactSecond.get(0).p.equals(p2) && resultExactSecond.get(1).p.equals(p1)),
                "Missing expected intersection points");

        // Case 5: maxDistance is beyond the second intersection point (4.0 > 3)
        List<Intersection> resultBeyond = tube.calcIntersections(ray, 4.0);
        assertNotNull(resultBeyond, "Should find 2 intersections");
        assertEquals(2, resultBeyond.size(), "Wrong number of intersections");
        assertTrue((resultBeyond.get(0).p.equals(p1) && resultBeyond.get(1).p.equals(p2)) ||
                        (resultBeyond.get(0).p.equals(p2) && resultBeyond.get(1).p.equals(p1)),
                "Missing expected intersection points");

        // Case 6: maxDistance is zero (0.0)
        assertNull(tube.calcIntersections(ray, 0.0),
                "Intersections should be null if maxDistance is zero");
    }
}