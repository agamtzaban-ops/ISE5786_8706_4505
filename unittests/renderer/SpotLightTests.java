package renderer;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import lighting.SpotLight;
import primitives.*;

/**
 * Unit tests for SpotLight class
 */
class SpotLightTests {

    /**
     * Test method for {@link lighting.SpotLight#getL(primitives.Point)}.
     */
    @Test
    void testGetL() {
        Point position = new Point(1, 1, 1);
        Vector direction = new Vector(1, 0, 0);
        SpotLight light = new SpotLight(new Color(255, 255, 255),
                position,
                direction);
        Point p = new Point(2, 1, 1);

        // TC01: Test getL returns direction from source to point
        assertEquals(new Vector(1, 0, 0),
                light.getL(p),
                "getL() wrong result for SpotLight");
    }

    /**
     * Test method for {@link lighting.SpotLight#getIntensity(primitives.Point)}.
     */
    @Test
    void testGetIntensity() {
        Point position = new Point(0, 0, 0);
        Vector direction = new Vector(1, 0, 0);
        Color color = new Color(100, 100, 100);

        SpotLight light = new SpotLight(color, position, direction)
                .setKc(1).setKl(0).setKq(0);

        // TC01: Point is in the direction of the spotlight (cos = 1)
        Point p1 = new Point(1, 0, 0);
        assertEquals(color,
                light.getIntensity(p1),
                "getIntensity() wrong in spotlight beam");

        // TC02: Point is behind the spotlight (cos < 0)
        Point p2 = new Point(-1, 0, 0);
        if (light.getIntensity(p2) != null) {
            assertEquals(Color.BLACK,
                    light.getIntensity(p2),
                    "getIntensity() should be black behind spotlight");

        }
    }
}