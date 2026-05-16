package renderer;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import lighting.DirectionalLight;
import primitives.*;

/**
 * Unit tests for DirectionalLight class
 */
class DirectionalLightTests {

    /**
     * Test method for {@link lighting.DirectionalLight#getL(primitives.Point)}.
     */
    @Test
    void testGetL() {
        Vector dir = new Vector(1, 1, -1);
        DirectionalLight light = new DirectionalLight(new Color(255, 255, 255), dir);
        Point p = new Point(1, 2, 3);

        // TC01: Test getL returns the normalized direction vector
        assertEquals(dir.normalize(),
                light.getL(p),
                "getL() wrong result for DirectionalLight");
    }

    /**
     * Test method for {@link lighting.DirectionalLight#getIntensity(primitives.Point)}.
     */
    @Test
    void testGetIntensity() {
        Color color = new Color(255, 100, 100);
        DirectionalLight light = new DirectionalLight(color, new Vector(1, 1, -1));
        Point p = new Point(1, 2, 3);

        // TC01: Test getIntensity returns the constant intensity
        assertEquals(color,
                light.getIntensity(p),
                "getIntensity() wrong result for DirectionalLight");
    }
}