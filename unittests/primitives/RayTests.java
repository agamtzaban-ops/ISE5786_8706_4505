package primitives;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for class {@link primitives.Ray}.
 */
class RayTests {

    public static final double DELTA = 0.000001;

    /**
     * Test method for constructor {@link primitives.Ray#Ray(primitives.Point, primitives.Vector)}.
     */
    @Test
    void testConstructor() {
        // ============ Equivalence Partitions Tests ==============
        // EP01: Test that the constructor normalizes the direction vector
        Ray ray = new Ray(new Point(1, 2, 3), new Vector(0, 3, 4));
        assertEquals(1d, ray.direction().length(), DELTA,
                "ERROR: Ray constructor does not normalize the direction vector");
    }
    @Test
    void testGetPoint() {
        Ray ray = new Ray(new Point(1, 0, 0), new Vector(1, 0, 0));

        // EP01: t > 0
        assertEquals(new Point(3, 0, 0), ray.getPoint(2), "getPoint error for t > 0");

        // EP02: t < 0
        assertEquals(new Point(0, 0, 0), ray.getPoint(-1), "getPoint error for t < 0");

        // BV01: t = 0
        assertEquals(new Point(1, 0, 0), ray.getPoint(0), "getPoint error for t = 0");
    }
    /**
     * Test method for {@link primitives.Ray#findClosestPoint(java.util.List)}.
     */
    @Test
    void testFindClosestPoint() {
        Ray ray = new Ray(new Point(0, 0, 10), new Vector(1, 1, 1));
        Point p1 = new Point(1, 1, 11);
        Point p2 = new Point(2, 2, 12);
        Point p3 = new Point(3, 3, 13);

        // =========================================================
        // EP: A list of at least 3 points, the middle point is the closest
        // =========================================================
        List<Point> listMiddle = List.of(p2, p1, p3);
        assertEquals(p1, ray.findClosestPoint(listMiddle),
                "The middle point should be the closest to the ray head");

        // =========================================================
        // BV: Empty list (null value) - should return null
        // =========================================================
        assertNull(ray.findClosestPoint(null),
                "A null list must return null");

        // =========================================================
        // BV: A list of at least 3 points, the first point is the closest
        // =========================================================
        List<Point> listFirst = List.of(p1, p2, p3);
        assertEquals(p1, ray.findClosestPoint(listFirst),
                "The first point should be the closest to the ray head");

        // =========================================================
        // BV: A list of at least 3 points, the last point is the closest
        // =========================================================
        List<Point> listLast = List.of(p3, p2, p1);
        assertEquals(p1, ray.findClosestPoint(listLast),
                "The last point should be the closest to the ray head");
    }
}