package geometries.impl;

import primitives.Point;
import primitives.Ray;
import primitives.Vector;
import java.util.List;
import static primitives.Util.alignZero;

/**
 * Class Triangle represents a polygon with exactly three vertices.
 */
public final class Triangle extends Polygon {

    /**
     * Constructor to initialize a triangle with three points.
     * @param p1 first point
     * @param p2 second point
     * @param p3 third point
     */
    public Triangle(Point p1, Point p2, Point p3) {
        super(p1, p2, p3);
    }

    @Override
    public List<Point> findIntersections(Ray ray) {
        // Step 1: Get vertices from the parent Polygon class
        // This follows the DRY principle (Don't Repeat Yourself)
        Point p1 = _vertices.get(0);
        Point p2 = _vertices.get(1);
        Point p3 = _vertices.get(2);

        // Step 2: Check intersection with the plane containing the triangle
        // Use the plane field from Polygon if it's protected,
        // or create a temporary one if needed for the calculation.
        Plane plane = new Plane(p1, p2, p3);
        List<Point> intersections = plane.findIntersections(ray);
        if (intersections == null) return null;

        Point p0 = ray.origin();
        Vector v = ray.direction();

        // Step 3: Inside/Outside test using cross products
        Vector v1 = p1.subtract(p0);
        Vector v2 = p2.subtract(p0);
        Vector v3 = p3.subtract(p0);

        Vector n1 = v1.crossProduct(v2).normalize();
        Vector n2 = v2.crossProduct(v3).normalize();
        Vector n3 = v3.crossProduct(v1).normalize();

        double d1 = alignZero(v.dotProduct(n1));
        double d2 = alignZero(v.dotProduct(n2));
        double d3 = alignZero(v.dotProduct(n3));

        // The point is inside if all signs are the same
        if ((d1 > 0 && d2 > 0 && d3 > 0) || (d1 < 0 && d2 < 0 && d3 < 0)) {
            return intersections;
        }

        return null;
    }
}