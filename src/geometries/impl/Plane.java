package geometries.impl;

import geometries.api.Geometry;
import primitives.Point;
import primitives.Ray;
import primitives.Vector;

import java.util.List;
import static primitives.Util.alignZero;

/**
 * Class Plane represents a two-dimensional flat surface in 3D space.
 */
public final class Plane extends Geometry {

    /**
     * A point on the plane
     */
    private final Point _q0;

    /**
     * The normal vector to the plane
     */
    private final Vector _normal;

    /**
     * Constructor to initialize a plane from three points.
     * Computes the normal vector based on the three points.
     *
     * @param p1 first point
     * @param p2 second point
     * @param p3 third point
     */
    public Plane(Point p1, Point p2, Point p3) {
        _q0 = p1;
        Vector u = p2.subtract(p1);
        Vector v = p3.subtract(p1);
        _normal = u.crossProduct(v).normalize();
    }

    /**
     * Constructor to initialize a plane from a point and a normal vector.
     * The normal vector is normalized before being saved.
     *
     * @param q0     the point on the plane
     * @param normal the normal vector
     */
    public Plane(Point q0, Vector normal) {
        _q0 = q0;
        _normal = normal.normalize();
    }

    @Override
    public Vector getNormal(Point point) {
        return _normal;
    }

    /**
     * Implementation of findIntersections for Plane.
     * Formula: t = n * (Q0 - P0) / (n * v)
     * * @param ray the ray to intersect with the plane
     * @return list containing one intersection point, or null
     */
    @Override
    public List<Point> findIntersections(Ray ray) {
        Point p0 = ray.origin();
        Vector v = ray.direction();
        Vector n = _normal;

        // n * v (denominator)
        double nv = n.dotProduct(v);

        // If the ray is parallel to the plane (nv == 0), there are no intersections
        if (alignZero(nv) == 0) {
            return null;
        }

        // Vector from ray origin to point on plane: u = Q0 - P0
        Vector u;
        try {
            u = _q0.subtract(p0);
        } catch (IllegalArgumentException e) {
            // Ray starts at the plane's reference point Q0
            return null;
        }

        // numerator = n * u
        double nQminusP0 = n.dotProduct(u);

        // t = (n * (Q0 - P0)) / (n * v)
        double t = alignZero(nQminusP0 / nv);

        // Only positive t (intersection in the ray's direction)
        if (t > 0) {
            // Refactoring: Use the new getPoint method from Ray class
            return List.of(ray.getPoint(t));
        }

        return null;
    }
}