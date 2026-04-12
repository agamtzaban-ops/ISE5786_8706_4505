package geometries.impl;

import org.junit.jupiter.api.Test;
import primitives.*;
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
}