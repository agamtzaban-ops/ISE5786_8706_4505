package primitives;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for class {@link primitives.Vector}.
 */
class VectorTests {

    /** Delta value for accuracy when comparing double values. */
    private static final double DELTA = 0.000001;

    /** Vector (1, 2, 3) used for tests */
    private static final Vector v1 = new Vector(1, 2, 3);
    /** Vector (-2, -4, -6) used for tests */
    private static final Vector v2 = new Vector(-2, -4, -6);
    /** Vector (0, 3, -2) used for tests (orthogonal to v1) */
    private static final Vector v3 = new Vector(0, 3, -2);

    /**
     * Test method for {@link primitives.Vector#add(primitives.Vector)}.
     */
    @Test
    void testAdd() {
        // ============ Equivalence Partitions Tests ==============
        // EP01: Simple addition of two vectors
        Vector v1AddV2 = new Vector(-1, -2, -3);
        assertEquals(v1AddV2, v1.add(v2), "ERROR: add() wrong result");

        // =============== Boundary Values Tests ==================
        // BV01: Addition resulting in zero vector
        assertThrows(IllegalArgumentException.class, () -> v1.add(new Vector(-1, -2, -3)),
                "ERROR: add() should throw an exception if the result is a zero vector");
    }

    /**
     * Test method for {@link primitives.Vector#subtract(primitives.Point)}.
     */
    @Test
    void testSubtract() {
        // ============ Equivalence Partitions Tests ==============
        // EP01: Simple subtraction
        Vector v1SubV2 = new Vector(3, 6, 9);
        assertEquals(v1SubV2, v1.subtract(v2), "ERROR: subtract() wrong result");

        // =============== Boundary Values Tests ==================
        // BV01: Subtraction of a vector from itself (zero vector)
        assertThrows(IllegalArgumentException.class, () -> v1.subtract(v1),
                "ERROR: subtract() should throw an exception if the result is a zero vector");
    }

    /**
     * Test method for {@link primitives.Vector#scale(double)}.
     */
    @Test
    void testScale() {
        // ============ Equivalence Partitions Tests ==============
        // EP01: Simple scaling
        assertEquals(new Vector(2, 4, 6), v1.scale(2), "ERROR: scale() wrong result");

        // =============== Boundary Values Tests ==================
        // BV01: Scaling by zero
        assertThrows(IllegalArgumentException.class, () -> v1.scale(0),
                "ERROR: scale() by zero should throw an exception");
    }

    /**
     * Test method for {@link primitives.Vector#dotProduct(primitives.Vector)}.
     */
    @Test
    void testDotProduct() {
        // ============ Equivalence Partitions Tests ==============
        // EP01: Simple dot product
        assertEquals(-28d, v1.dotProduct(v2), DELTA, "ERROR: dotProduct() wrong result");

        // =============== Boundary Values Tests ==================
        // BV01: Dot product of orthogonal vectors
        assertEquals(0d, v1.dotProduct(v3), DELTA, "ERROR: dotProduct() for orthogonal vectors is not zero");
    }

    /**
     * Test method for {@link primitives.Vector#crossProduct(primitives.Vector)}.
     */
    @Test
    void testCrossProduct() {
        // ============ Equivalence Partitions Tests ==============
        // EP01: Cross product of two vectors
        Vector vr = v1.crossProduct(v3);
        // Test that length of cross-product is proper (orthogonal vectors taken for simplicity)
        assertEquals(v1.length() * v3.length(), vr.length(), DELTA, "ERROR: crossProduct() wrong result length");
        // Test cross-product result orthogonality to its operands
        assertEquals(0, vr.dotProduct(v1), DELTA, "ERROR: crossProduct() result is not orthogonal to 1st operand");
        assertEquals(0, vr.dotProduct(v3), DELTA, "ERROR: crossProduct() result is not orthogonal to 2nd operand");

        // =============== Boundary Values Tests ==================
        // BV01: Cross product of parallel vectors
        assertThrows(IllegalArgumentException.class, () -> v1.crossProduct(v2),
                "ERROR: crossProduct() for parallel vectors does not throw an exception");
    }

    /**
     * Test method for {@link primitives.Vector#lengthSquared()}.
     */
    @Test
    void testLengthSquared() {
        // ============ Equivalence Partitions Tests ==============
        // EP01: Simple length squared
        assertEquals(14d, v1.lengthSquared(), DELTA, "ERROR: lengthSquared() wrong result");
    }

    /**
     * Test method for {@link primitives.Vector#length()}.
     */
    @Test
    void testLength() {
        // ============ Equivalence Partitions Tests ==============
        // EP01: Simple length
        assertEquals(5d, new Vector(0, 3, 4).length(), DELTA, "ERROR: length() wrong result");
    }

    /**
     * Test method for {@link primitives.Vector#normalize()}.
     */
    @Test
    void testNormalize() {
        // ============ Equivalence Partitions Tests ==============
        // EP01: Simple normalization
        Vector v = new Vector(0, 3, 4);
        Vector n = v.normalize();

        // Ensure the length is 1
        assertEquals(1d, n.length(), DELTA, "ERROR: normalize() result length is not 1");

        // Ensure the normalized vector is parallel to the original
        assertThrows(IllegalArgumentException.class, () -> v.crossProduct(n),
                "ERROR: normalized vector is not parallel to the original one");

        // Ensure the normalized vector is in the same direction
        assertTrue(v.dotProduct(n) > 0, "ERROR: normalized vector is in the opposite direction");
    }
}