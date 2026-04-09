package geometries.impl;

import org.junit.jupiter.api.Test;
import primitives.Point;
import primitives.Ray;
import primitives.Vector;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for class {@link geometries.impl.Triangle}.
 */
class TriangleTests {

    /** Error message for triangle intersection failures */
    private static final String ERROR_TRIANGLE_INTERSECTION = "Wrong triangle intersection result";

    // ================== STAGE 2 TESTS ==================

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

    // ================== STAGE 3 TESTS ==================

    /**
     * Test method for {@link geometries.impl.Triangle#findIntersections(primitives.Ray)}.
     */
    @Test
    void testFindIntersections() {
        // Simple triangle on the Z=0 plane (X-Y plane)
        Triangle triangle = new Triangle(new Point(0, 1, 0), new Point(1, 0, 0), new Point(-1, 0, 0));

        // ==========================================================
        //         TRIANGLE SPECIFIC TESTS (Based on Plane EP)
        // ==========================================================

        // ============ Equivalence Partitions Tests ==============

        // EP01: Intersection inside the triangle (1 point)
        List<Point> result = triangle.findIntersections(new Ray(new Point(0, 0.5, -1), new Vector(0, 0, 1)));
        assertNotNull(result, ERROR_TRIANGLE_INTERSECTION);
        assertEquals(1, result.size(), ERROR_TRIANGLE_INTERSECTION);
        assertEquals(List.of(new Point(0, 0.5, 0)), result, ERROR_TRIANGLE_INTERSECTION);

        // EP02: Intersection outside the triangle, against an edge (0 points)
        assertNull(triangle.findIntersections(new Ray(new Point(0, -0.5, -1), new Vector(0, 0, 1))),
                ERROR_TRIANGLE_INTERSECTION);

        // EP03: Intersection outside the triangle, against a vertex (0 points)
        assertNull(triangle.findIntersections(new Ray(new Point(2, 0, -1), new Vector(0, 0, 1))),
                ERROR_TRIANGLE_INTERSECTION);

        // =============== Boundary Values Tests ==================

        // BV01: Intersection exactly on an edge (0 points - we don't include borders)
        assertNull(triangle.findIntersections(new Ray(new Point(0.5, 0.5, -1), new Vector(0, 0, 1))),
                ERROR_TRIANGLE_INTERSECTION);

        // BV02: Intersection exactly in a vertex (0 points)
        assertNull(triangle.findIntersections(new Ray(new Point(1, 0, -1), new Vector(0, 0, 1))),
                ERROR_TRIANGLE_INTERSECTION);

        // BV03: Intersection on an edge's continuation (0 points)
        assertNull(triangle.findIntersections(new Ray(new Point(2, -1, -1), new Vector(0, 0, 1))),
                ERROR_TRIANGLE_INTERSECTION);

        // ==========================================================
        //             PLANE CASES APPLIED TO TRIANGLE
        // ==========================================================

        // ---- Plane "No Intersection" Cases (EP & BVA) ----

        // Plane EP: Ray does not intersect the plane at all
        assertNull(triangle.findIntersections(new Ray(new Point(0, 0.5, -1), new Vector(0, 0, -1))),
                ERROR_TRIANGLE_INTERSECTION);

        // Plane BV: Ray is parallel to the plane and included in it
        assertNull(triangle.findIntersections(new Ray(new Point(0, 0.5, 0), new Vector(1, 0, 0))),
                ERROR_TRIANGLE_INTERSECTION);

        // Plane BV: Ray is parallel to the plane but not included
        assertNull(triangle.findIntersections(new Ray(new Point(0, 0.5, 1), new Vector(1, 0, 0))),
                ERROR_TRIANGLE_INTERSECTION);

        // Plane BV: Ray is orthogonal to the plane and starts in the plane
        assertNull(triangle.findIntersections(new Ray(new Point(0, 0.5, 0), new Vector(0, 0, 1))),
                ERROR_TRIANGLE_INTERSECTION);

        // Plane BV: Ray is orthogonal to the plane and starts after the plane
        assertNull(triangle.findIntersections(new Ray(new Point(0, 0.5, 1), new Vector(0, 0, 1))),
                ERROR_TRIANGLE_INTERSECTION);

        // Plane BV: Ray begins in the plane (not orthogonal/parallel)
        assertNull(triangle.findIntersections(new Ray(new Point(0, 0.5, 0), new Vector(1, 1, 1))),
                ERROR_TRIANGLE_INTERSECTION);

        // ---- Plane "1 Intersection" Cases (BVA) ----

        // Plane BV: Ray is orthogonal to the plane and starts before the plane (hits inside triangle)
        result = triangle.findIntersections(new Ray(new Point(0, 0.5, -1), new Vector(0, 0, 1)));
        assertNotNull(result, ERROR_TRIANGLE_INTERSECTION);
        assertEquals(1, result.size(), ERROR_TRIANGLE_INTERSECTION);
        assertEquals(List.of(new Point(0, 0.5, 0)), result, ERROR_TRIANGLE_INTERSECTION);
    }
}