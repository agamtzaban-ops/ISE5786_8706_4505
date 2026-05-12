package geometries.impl;

import geometries.api.Intersectable.Intersection;
import primitives.Point;
import primitives.Ray;
import primitives.Vector;
import java.util.List;
import static primitives.Util.alignZero;

/**
 * Class Triangle represents a two-dimensional triangle in 3D space.
 * Inherits from Polygon.
 */
public final class Triangle extends Polygon {

    /**
     * Constructor to initialize a triangle with three points.
     *
     * @param p1 first point
     * @param p2 second point
     * @param p3 third point
     */
    public Triangle(Point p1, Point p2, Point p3) {
        // A triangle is simply a polygon with 3 vertices
        super(p1, p2, p3);
    }

    /**
     * Helper method for calculating intersections using the NVI pattern.
     * Overrides the method to provide a more efficient, triangle-specific intersection check.
     *
     * @param ray the ray to check for intersections
     * @return list containing the intersection object, or null if none
     */
    @Override
    protected List<Intersection> calcIntersectionsHelper(Ray ray) {
        // Step 1: Intersect with the plane containing the triangle
        // Utilizing the _plane field inherited from Polygon (DRY principle)
        List<Point> planeIntersections = _plane.findIntersections(ray);
        if (planeIntersections == null) {
            return null;
        }

        // Step 2: Check if the intersection point is inside the triangle
        Point p0 = ray.origin();
        Vector v = ray.direction();

        Point p1 = _vertices.get(0);
        Point p2 = _vertices.get(1);
        Point p3 = _vertices.get(2);

        Vector v1 = p1.subtract(p0);
        Vector v2 = p2.subtract(p0);
        Vector v3 = p3.subtract(p0);

        Vector n1 = v1.crossProduct(v2).normalize();
        Vector n2 = v2.crossProduct(v3).normalize();
        Vector n3 = v3.crossProduct(v1).normalize();

        double vn1 = alignZero(v.dotProduct(n1));
        double vn2 = alignZero(v.dotProduct(n2));
        double vn3 = alignZero(v.dotProduct(n3));

        // The point is inside the triangle if all dot products have the same sign
        if ((vn1 > 0 && vn2 > 0 && vn3 > 0) || (vn1 < 0 && vn2 < 0 && vn3 < 0)) {
            // Return the intersection point wrapped in an Intersection object linked to THIS triangle
            return List.of(new Intersection(this, planeIntersections.get(0)));
        }

        return null;
    }
}