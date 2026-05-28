package geometries;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import geometries.api.Intersectable.Intersection;
import geometries.impl.Plane;
import geometries.impl.Polygon;
import primitives.Point;
import primitives.Ray;
import primitives.Vector;

/**
 * Unit tests for class {@link Polygon}.
 * The tests verify:
 * <ul>
 * <li>Polygon constructor validity</li>
 * <li>{@link Polygon#getNormal(Point)}</li>
 * </ul>
 * Tests follow the methodology of
 * Equivalence Partitions (EP) and Boundary Values (BVA).
 */
class PolygonTests {
    /** Default constructor to satisfy JavaDoc generator */
    PolygonTests() { /* to satisfy JavaDoc generator */ }

    /** Vertex (1,0,0) used in polygon tests */
    private static final Point  POINT_X       = new Point(1, 0, 0);
    /** Vertex (0,1,0) used in polygon tests */
    private static final Point  POINT_Y       = new Point(0, 1, 0);
    /** Vertex (0,0,1) used in polygon tests */
    private static final Point  POINT_Z       = new Point(0, 0, 1);

    /** Additional vertex used for valid polygon construction */
    private static final Point  POINT1        = new Point(-1, 1, 1);
    /** Point not in the polygon plane */
    private static final Point  POINT2        = new Point(0, 2, 2);
    /** Point that creates a concave polygon */
    private static final Point  POINT3        = new Point(0.5, 0.25, 0.5);
    /** Point located on one of the polygon edges */
    private static final Point  POINT4        = new Point(0, 0.5, 0.5);

    /**
     * Delta value for accuracy when comparing double values.
     */
    private static final double DELTA         = 1e-6;

    /** Error message for wrong plane intersection */
    private static final String ERROR_PLANE   = "ERROR: wrong intersection with plane";
    /** Error message for wrong polygon intersection */
    private static final String ERROR_POLYGON = "ERROR: wrong polygon intersection";

    /**
     * Test method for {@link Polygon#Polygon(Point...)}.
     * Verifies correct and incorrect polygon constructions.
     */
    @Test
    void testConstructor() {

        // ============ Equivalence Partitions Tests ==============

        // TC01: Correct convex quadrilateral with vertices in correct order
        assertDoesNotThrow(() -> new Polygon(POINT_Z, POINT_X, POINT_Y, POINT1),
                "Failed constructing a correct polygon");

        // TC02: Wrong vertices order
        assertThrows(IllegalArgumentException.class, () -> new Polygon(POINT_Z, POINT_Y, POINT_X, POINT1),
                "Constructed a polygon with wrong order of vertices");

        // TC03: Vertices not in the same plane
        assertThrows(IllegalArgumentException.class, () -> new Polygon(POINT_Z, POINT_X, POINT_Y, POINT2),
                "Constructed a polygon with vertices that are not in the same plane");

        // TC04: Concave quadrilateral
        assertThrows(IllegalArgumentException.class, () -> new Polygon(POINT_Z, POINT_X, POINT_Y, POINT3),
                "Constructed a concave polygon");

        // =============== Boundary Values Tests ==================

        // TC11: Vertex on a side
        assertThrows(IllegalArgumentException.class, () -> new Polygon(POINT_Z, POINT_X, POINT_Y, POINT4),
                "Constructed a polygon with a vertex on a side");

        // TC12: Last point equals first point
        assertThrows(IllegalArgumentException.class, () -> new Polygon(POINT_Z, POINT_X, POINT_Y, POINT_Z),
                "Constructed a polygon with duplicate first/last vertex");

        // TC13: Co-located points
        assertThrows(IllegalArgumentException.class, () -> new Polygon(POINT_Z, POINT_X, POINT_Y, POINT_Y),
                "Constructed a polygon with co-located vertices");
    }

    /**
     * Test method for {@link Polygon#getNormal(Point)}.
     * Verifies that the returned normal vector is unit length and orthogonal
     * to all polygon edges.
     */
    @Test
    void testGetNormal() {
        // ============ Equivalence Partitions Tests ==============
        Point[] pts     =
                { POINT_Z, POINT_X, POINT_Y, POINT1 };
        Polygon polygon = new Polygon(pts);
        // Ensure method does not throw exception
        assertDoesNotThrow(() -> polygon.getNormal(POINT_Z), "getNormal() threw unexpected exception");
        Vector result = polygon.getNormal(POINT_Z);
        // Ensure |n| = 1
        assertEquals(1, result.length(), DELTA, "Polygon normal is not a unit vector");
        // Ensure normal is orthogonal to all edges
        for (int i = 0; i < pts.length; ++i) {
            Vector edge = pts[i].subtract(pts[i == 0 ? pts.length - 1 : i - 1]);
            assertEquals(0d, result.dotProduct(edge), DELTA, "Polygon normal is not orthogonal to an edge");
        }
    }

    // ================== STAGE 8 TESTS ==================

    /**
     * Test method for {@link Polygon#calcIntersections(primitives.Ray, double)}.
     */
    @Test
    void testCalcIntersectionsMaxDistance() {
        // Simple square polygon on the Z=0 plane (X-Y plane)
        Polygon polygon = new Polygon(
                new Point(0, 0, 0),
                new Point(2, 0, 0),
                new Point(2, 2, 0),
                new Point(0, 2, 0)
        );

        // Ray starts at (1, 1, -1) and goes straight up towards the polygon
        // The expected intersection point is (1, 1, 0) inside the polygon at distance t = 1
        Ray ray = new Ray(new Point(1, 1, -1), new Vector(0, 0, 1));
        Point expectedIntersection = new Point(1, 1, 0);

        // Case 1: maxDistance is smaller than the distance to the polygon (0.5 < 1)
        assertNull(polygon.calcIntersections(ray, 0.5),
                "Polygon intersection should be filtered out if maxDistance is before the polygon");

        // Case 2: maxDistance is exactly at the polygon intersection point (1.0 == 1)
        List<Intersection> resultExact = polygon.calcIntersections(ray, 1.0);
        assertNotNull(resultExact, "Should find 1 intersection when maxDistance is exactly on the polygon");
        assertEquals(1, resultExact.size(), "Wrong number of intersections for exact maxDistance");
        assertEquals(expectedIntersection, resultExact.get(0).p, "Wrong intersection point for exact maxDistance");

        // Case 3: maxDistance is beyond the polygon intersection point (2.0 > 1)
        List<Intersection> resultBeyond = polygon.calcIntersections(ray, 2.0);
        assertNotNull(resultBeyond, "Should find 1 intersection when maxDistance is beyond the polygon");
        assertEquals(1, resultBeyond.size(), "Wrong number of intersections for broad maxDistance");
        assertEquals(expectedIntersection, resultBeyond.get(0).p, "Wrong intersection point for broad maxDistance");
    }
}