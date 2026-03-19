package geometries.impl;

import org.junit.jupiter.api.Test;
import primitives.Point;
import primitives.Ray;
import primitives.Vector;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for class {@link geometries.impl.Cylinder}.
 */
class CylinderTests {

    /**
     * Test method for {@link geometries.impl.Cylinder#getNormal(primitives.Point)}.
     */
    @Test
    void testGetNormal() {
        Cylinder cylinder = new Cylinder(1.0, new Ray(new Point(0, 0, 0), new Vector(0, 0, 1)), 5.0);
        Vector expectedNormalSide = new Vector(1, 0, 0);
        Vector expectedNormalBottom = new Vector(0, 0, -1);
        Vector expectedNormalTop = new Vector(0, 0, 1);

        // ============ Equivalence Partitions Tests ==============
        // EP01: Point on the side of the cylinder
        assertEquals(expectedNormalSide, cylinder.getNormal(new Point(1, 0, 2)),
                "ERROR: getNormal() wrong result for Cylinder side (EP)");

        // EP02: Point on the bottom base of the cylinder
        assertEquals(expectedNormalBottom, cylinder.getNormal(new Point(0.5, 0, 0)),
                "ERROR: getNormal() wrong result for Cylinder bottom base (EP)");

        // EP03: Point on the top base of the cylinder
        assertEquals(expectedNormalTop, cylinder.getNormal(new Point(0.5, 0, 5)),
                "ERROR: getNormal() wrong result for Cylinder top base (EP)");

        // =============== Boundary Values Tests ==================
        // BV01: Point at the center of the bottom base
        assertEquals(expectedNormalBottom, cylinder.getNormal(new Point(0, 0, 0)),
                "ERROR: getNormal() wrong result for Cylinder bottom base center (BV)");

        // BV02: Point at the center of the top base
        assertEquals(expectedNormalTop, cylinder.getNormal(new Point(0, 0, 5)),
                "ERROR: getNormal() wrong result for Cylinder top base center (BV)");

        // BV03: Point on the edge between the side and the bottom base
        assertEquals(expectedNormalBottom, cylinder.getNormal(new Point(1, 0, 0)),
                "ERROR: getNormal() wrong result for Cylinder bottom edge (BV)");

        // BV04: Point on the edge between the side and the top base
        assertEquals(expectedNormalTop, cylinder.getNormal(new Point(1, 0, 5)),
                "ERROR: getNormal() wrong result for Cylinder top edge (BV)");
    }
}