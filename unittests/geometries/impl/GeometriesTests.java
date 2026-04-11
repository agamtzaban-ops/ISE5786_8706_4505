package geometries.impl;

import org.junit.jupiter.api.Test;
import primitives.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** Unit tests for Geometries class */
class GeometriesTests {

    @Test
    void testFindIntersections() {
        Geometries geometries = new Geometries(
                new Sphere(new Point(1, 0, 0), 1d),
                new Plane(new Point(2, 0, 0), new Vector(1, 0, 0)),
                new Triangle(new Point(3, 1, 0), new Point(3, -1, 0), new Point(3, 0, 1))
        );

        // ============ Equivalence Partitions Tests ==============
        // EP01: Some geometries intersect (but not all)
        // Ray starts after the sphere, hits plane and triangle
        Ray rayEP = new Ray(new Point(1.5, 0, 0), new Vector(1, 0, 0));
        assertEquals(2, geometries.findIntersections(rayEP).size(), "Some geometries should intersect");

        // =============== Boundary Values Tests ==================
        // BV01: Empty collection
        assertNull(new Geometries().findIntersections(new Ray(new Point(0, 0, 0), new Vector(1, 0, 0))),
                "Empty collection should return null");

        // BV02: No geometry intersects
        assertNull(geometries.findIntersections(new Ray(new Point(0, -5, 0), new Vector(0, -1, 0))),
                "No geometry should intersect");

        // BV03: Only one geometry intersects (Sphere)
        Ray rayOne = new Ray(new Point(0.5, 0, 0), new Vector(0, 1, 0));
        assertEquals(1, geometries.findIntersections(rayOne).size(), "Only one geometry should intersect");

        // BV04: All geometries intersect (Sphere: 2, Plane: 1, Triangle: 1 = total 4)
        Ray rayAll = new Ray(new Point(-1, 0, 0), new Vector(1, 0, 0));
        assertEquals(4, geometries.findIntersections(rayAll).size(), "All geometries should intersect");
    }
}