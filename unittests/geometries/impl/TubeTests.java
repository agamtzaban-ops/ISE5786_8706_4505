package geometries.impl;

import org.junit.jupiter.api.Test;
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
}