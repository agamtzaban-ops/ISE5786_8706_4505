package geometries.impl;

import org.junit.jupiter.api.Test;
import geometries.api.Intersectable.Intersection;
import primitives.Point;
import primitives.Ray;
import primitives.Vector;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for class {@link geometries.impl.Cylinder}.
 */
class CylinderTests {

    /**
     * Test method for {@link geometries.impl.Cylinder#getNormal(primitives.Point)}.
     */
    @Test
    void testGetNormal() {
        // Fix: Updated constructor call order (Ray, double, double)
        Ray axis = new Ray(new Point(0, 0, 0), new Vector(0, 0, 1));
        double radius = 1.0;
        double height = 5.0;

        Cylinder cylinder = new Cylinder(axis, radius, height);

        Vector expectedNormalSide = new Vector(1, 0, 0);
        Vector expectedNormalBottom = new Vector(0, 0, -1);
        Vector expectedNormalTop = new Vector(0, 0, 1);

        // ============ Equivalence Partitions Tests ==============
        // EP01: Point on the side of the cylinder
        assertEquals(expectedNormalSide, cylinder.getNormal(new Point(1, 0, 2)),
                "ERROR: getNormal() wrong result for Cylinder side (EP)");

        // EP02: Point on the bottom base of the cylinder
        assertEquals(expectedNormalBottom, cylinder.getNormal(new Point(0.5, 0, 0)),
                "ERROR: getNormal() wrong result for Cylinder bottom base (EP)");

        // EP03: Point on the top base of the cylinder
        assertEquals(expectedNormalTop, cylinder.getNormal(new Point(0.5, 0, 5)),
                "ERROR: getNormal() wrong result for Cylinder top base (EP)");

        // =============== Boundary Values Tests ==================
        // BV01: Point at the center of the bottom base
        assertEquals(expectedNormalBottom, cylinder.getNormal(new Point(0, 0, 0)),
                "ERROR: getNormal() wrong result for Cylinder bottom base center (BV)");

        // BV02: Point at the center of the top base
        assertEquals(expectedNormalTop, cylinder.getNormal(new Point(0, 0, 5)),
                "ERROR: getNormal() wrong result for Cylinder top base center (BV)");

        // BV03: Point on the edge between the side and the bottom base
        assertEquals(expectedNormalBottom, cylinder.getNormal(new Point(1, 0, 0)),
                "ERROR: getNormal() wrong result for Cylinder bottom edge (BV)");

        // BV04: Point on the edge between the side and the top base
        assertEquals(expectedNormalTop, cylinder.getNormal(new Point(1, 0, 5)),
                "ERROR: getNormal() wrong result for Cylinder top edge (BV)");
    }

    // ================== STAGE 8 TESTS ==================

    /**
     * Test method for {@link geometries.impl.Cylinder#calcIntersections(primitives.Ray, double)}.
     */
    @Test
    void testCalcIntersectionsMaxDistance() {
        Ray axis = new Ray(new Point(0, 0, 0), new Vector(0, 0, 1));
        Cylinder cylinder = new Cylinder(axis, 1.0, 5.0);

        // Ray starts outside at (2, 0, 2) and goes left in direction (-1, 0, 0)
        // It crosses the side of the cylinder horizontally at Z = 2
        // Expected intersections:
        // P1: (1, 0, 2) at distance t1 = 1.0
        // P2: (-1, 0, 2) at distance t2 = 3.0
        Ray ray = new Ray(new Point(2, 0, 2), new Vector(-1, 0, 0));
        Point p1 = new Point(1, 0, 2);
        Point p2 = new Point(-1, 0, 2);

        // Case 1: maxDistance is smaller than the distance to the first intersection point (0.5 < 1)
        assertNull(cylinder.calcIntersections(ray, 0.5),
                "Cylinder intersections should be filtered out if maxDistance is before the first intersection");

        // Case 2: maxDistance is exactly at the first intersection point (1.0 == 1)
        List<Intersection> resultExactFirst = cylinder.calcIntersections(ray, 1.0);
        assertNotNull(resultExactFirst, "Should find 1 intersection at exact first distance");
        assertEquals(1, resultExactFirst.size(), "Wrong number of intersections for exact first distance");
        assertEquals(p1, resultExactFirst.get(0).p, "Wrong intersection point for exact first distance");

        // Case 3: maxDistance is between the first and second intersection points (2.0)
        List<Intersection> resultBetween = cylinder.calcIntersections(ray, 2.0);
        assertNotNull(resultBetween, "Should find 1 intersection when maxDistance is between points");
        assertEquals(1, resultBetween.size(), "Wrong number of intersections when maxDistance is between points");
        assertEquals(p1, resultBetween.get(0).p, "Wrong intersection point when maxDistance is between points");

        // Case 4: maxDistance is exactly at the second intersection point (3.0 == 3)
        List<Intersection> resultExactSecond = cylinder.calcIntersections(ray, 3.0);
        assertNotNull(resultExactSecond, "Should find 2 intersections at exact second distance");
        assertEquals(2, resultExactSecond.size(), "Wrong number of intersections for exact second distance");
        assertTrue((resultExactSecond.get(0).p.equals(p1) && resultExactSecond.get(1).p.equals(p2)) ||
                        (resultExactSecond.get(0).p.equals(p2) && resultExactSecond.get(1).p.equals(p1)),
                "Missing expected intersection points at exact second distance");

        // Case 5: maxDistance is beyond the second intersection point (4.0 > 3)
        List<Intersection> resultBeyond = cylinder.calcIntersections(ray, 4.0);
        assertNotNull(resultBeyond, "Should find 2 intersections when maxDistance is beyond both points");
        assertEquals(2, resultBeyond.size(), "Wrong number of intersections when maxDistance is beyond both points");
        assertTrue((resultBeyond.get(0).p.equals(p1) && resultBeyond.get(1).p.equals(p2)) ||
                        (resultBeyond.get(0).p.equals(p2) && resultBeyond.get(1).p.equals(p1)),
                "Missing expected intersection points when maxDistance is beyond both points");

        // Case 6: maxDistance is zero (0.0)
        assertNull(cylinder.calcIntersections(ray, 0.0),
                "Intersections should be null if maxDistance is zero");
    }
}