package geometries.impl;

import org.junit.jupiter.api.Test;
import primitives.Point;
import primitives.Vector;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for class {@link geometries.impl.Triangle}.
 */
class TriangleTests {

    /**
     * Test method for {@link geometries.impl.Triangle#getNormal(primitives.Point)}.
     */
    @Test
    void testGetNormal() {
        Triangle triangle = new Triangle(new Point(0, 0, 0), new Point(1, 0, 0), new Point(0, 1, 0));
        Vector expectedNormal = new Vector(0, 0, 1);

        // ============ Equivalence Partitions Tests ==============
        // EP01: Normal at a point inside the triangle
        assertEquals(expectedNormal, triangle.getNormal(new Point(0.25, 0.25, 0)),
                "ERROR: getNormal() wrong result for Triangle");
    }
}