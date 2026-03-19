package geometries.impl;

import org.junit.jupiter.api.Test;
import primitives.Point;
import primitives.Ray;
import primitives.Vector;
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
}