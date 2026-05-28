package geometries.impl;

import org.junit.jupiter.api.Test;
import geometries.api.Intersectable.Intersection;
import primitives.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** Unit tests for Geometries class */
class GeometriesTests {

    @Test
    void testFindIntersections() {
        // Setup a bulletproof scene: shapes are spaced out along the X axis
        Geometries geometries = new Geometries(
                new Sphere(new Point(2, 0, 0), 1d), // Sphere occupies X: [1, 3]
                new Plane(new Point(5, 0, 0), new Vector(1, 0, 0)), // Plane is at X=5
                new Triangle(new Point(8, 2, -2), new Point(8, -2, -2), new Point(8, 0, 2)) // Triangle is at X=8
        );

        // ============ Equivalence Partitions Tests ==============
        // EP01: Some geometries intersect (Ray misses the sphere, but hits the plane and triangle)
        Ray rayEP = new Ray(new Point(4, 0, 0), new Vector(1, 0, 0));
        assertEquals(2, geometries.findIntersections(rayEP).size(),
                "Some geometries should intersect");

        // =============== Boundary Values Tests ==================
        // BV01: Empty collection
        assertNull(new Geometries().findIntersections(new Ray(new Point(0, 0, 0), new Vector(1, 0, 0))),
                "Empty collection should return null");

        // BV02: No geometry intersects (Ray goes perfectly in the opposite direction)
        assertNull(geometries.findIntersections(new Ray(new Point(0, 0, 0), new Vector(-1, 0, 0))),
                "No geometry should intersect");

        // BV03: Only one geometry intersects (Ray starts inside the sphere and goes UP, parallel to the plane)
        Ray rayOne = new Ray(new Point(2, 0, 0), new Vector(0, 1, 0));
        assertEquals(1, geometries.findIntersections(rayOne).size(),
                "Only one geometry should intersect");

        // BV04: All geometries intersect (Sphere: 2, Plane: 1, Triangle: 1 = total 4)
        Ray rayAll = new Ray(new Point(0, 0, 0), new Vector(1, 0, 0));
        assertEquals(4, geometries.findIntersections(rayAll).size(),
                "All geometries should intersect");
    }

    // ================== STAGE 8 TESTS ==================

    /**
     * Test method for {@link geometries.impl.Geometries#calcIntersections(primitives.Ray, double)}.
     */
    @Test
    void testCalcIntersectionsMaxDistance() {
        // Same scene setup: shapes along the positive X axis
        Geometries geometries = new Geometries(
                new Sphere(new Point(2, 0, 0), 1d), // Intersects at distance 1 and 3
                new Plane(new Point(5, 0, 0), new Vector(1, 0, 0)), // Intersects at distance 5
                new Triangle(new Point(8, 2, -2), new Point(8, -2, -2), new Point(8, 0, 2)) // Intersects at distance 8
        );

        // Ray starts at origin, moving along the positive X axis
        Ray ray = new Ray(new Point(0, 0, 0), new Vector(1, 0, 0));

        // Case 1: maxDistance is smaller than the distance to the first intersection
        assertNull(geometries.calcIntersections(ray, 0.5),
                "Should return null when maxDistance is smaller than the first object");

        // Case 2: maxDistance includes only the first intersection of the sphere (t=1)
        List<Intersection> resultOne = geometries.calcIntersections(ray, 2.0);
        assertNotNull(resultOne, "Should return 1 intersection");
        assertEquals(1, resultOne.size(), "Should find exactly 1 intersection (front of sphere)");

        // Case 3: maxDistance includes both intersections of the sphere (t=1, t=3) but misses the plane (t=5)
        List<Intersection> resultTwo = geometries.calcIntersections(ray, 4.0);
        assertNotNull(resultTwo, "Should return 2 intersections");
        assertEquals(2, resultTwo.size(), "Should find exactly 2 intersections (both sides of sphere)");

        // Case 4: maxDistance includes the sphere and the plane (t=5)
        List<Intersection> resultThree = geometries.calcIntersections(ray, 6.0);
        assertNotNull(resultThree, "Should return 3 intersections");
        assertEquals(3, resultThree.size(), "Should find exactly 3 intersections (sphere + plane)");

        // Case 5: maxDistance includes all geometries (t=8)
        List<Intersection> resultAll = geometries.calcIntersections(ray, 10.0);
        assertNotNull(resultAll, "Should return all intersections");
        assertEquals(4, resultAll.size(), "Should find exactly 4 intersections (sphere x2 + plane + triangle)");
    }
}