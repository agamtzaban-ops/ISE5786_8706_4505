package renderer;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import lighting.PointLight;
import primitives.*;

/**
 * Unit tests for the PointLight class propagation model (Stage 7).
 * Tests getL(Point) and getIntensity(Point) directly without rendering.
 */
class PointLightTests {

    /** Default constructor to satisfy JavaDoc generator */
    PointLightTests() { /* to satisfy JavaDoc generator */ }

    // Constants for the test cases
    private static final Color LIGHT_COLOR = new Color(500, 500, 500);
    private static final Point LIGHT_POS   = new Point(0, 0, 100);

    /**
     * Test method for {@link lighting.PointLight#getL(primitives.Point)}.
     */
    @Test
    void testGetL() {
        PointLight pointLight = new PointLight(LIGHT_COLOR, LIGHT_POS);

        // ============ Equivalence Partitions Tests ==============
        // EP01: Standard point at a distance from the light source
        Point targetPoint = new Point(0, 0, 0);
        Vector expectedVector = new Vector(0, 0, -1); // From (0,0,100) to (0,0,0) -> normalized

        assertEquals(expectedVector, pointLight.getL(targetPoint),
                "ERROR: getL() wrong vector direction or not normalized (EP)"); //

        // =============== Boundary Values Tests ==================
        // BV01: Point coincides with the light position
        // Expected behavior: subtraction results in Vector.ZERO, throwing an exception
        assertThrows(IllegalArgumentException.class, () -> pointLight.getL(LIGHT_POS),
                "ERROR: getL() should throw IllegalArgumentException when point coincides with light position (BV)"); //
    }

    /**
     * Test method for {@link lighting.PointLight#getIntensity(primitives.Point)}.
     */
    @Test
    void testGetIntensity() {
        // Point light with attenuation coefficients
        // Base Formula: I_L = I_0 / (kC + kL * d + kQ * d^2)
        PointLight pointLight = new PointLight(LIGHT_COLOR, LIGHT_POS)
                .setKl(0.01)
                .setKq(0.002); //

        // ============ Equivalence Partitions Tests ==============
        // EP01: Distance is exactly 10 units away
        // d = 10 -> denominator: kC(1) + kL(0.01)*10 + kQ(0.002)*100 = 1 + 0.1 + 0.2 = 1.3
        // Expected intensity = I_0 / 1.3 = 500 / 1.3 ≈ 384.615
        Point targetPoint = new Point(0, 0, 90); // Distance = 10 units
        Color expectedColor = LIGHT_COLOR.scale(1.0 / 1.3);
        assertEquals(expectedColor, pointLight.getIntensity(targetPoint),
                "ERROR: getIntensity() wrong calculation for distance attenuation (EP)"); //

        // =============== Boundary Values Tests ==================
        // BV01: Point coincides with the light position (d = 0)
        // Denominator should be kC = 1. Expected intensity = I_0
        assertEquals(LIGHT_COLOR, pointLight.getIntensity(LIGHT_POS),
                "ERROR: getIntensity() should return original intensity when distance is 0 (BV)"); //
    }
}