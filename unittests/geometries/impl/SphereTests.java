package geometries.impl;

import org.junit.jupiter.api.Test;
import primitives.Point;
import primitives.Vector;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for class {@link geometries.impl.Sphere}.
 */
class SphereTests {

    /**
     * Test method for {@link geometries.impl.Sphere#getNormal(primitives.Point)}.
     */
    @Test
    void testGetNormal() {
        // Create a sphere with radius 5 around the center (0,0,0)
        Sphere sphere = new Sphere(new Point(0, 0, 0), 5);

        // ============ Equivalence Partitions Tests ==============
        // EP01: Normal at a point on the sphere
        // The normal from center (0,0,0) to point (0,0,5) should be the normalized vector (0,0,1)
        Vector expectedNormal = new Vector(0, 0, 1);
        assertEquals(expectedNormal, sphere.getNormal(new Point(0, 0, 5)),
                "ERROR: getNormal() wrong result for Sphere");
    }
}