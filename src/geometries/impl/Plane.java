package geometries.impl;

import geometries.api.Geometry;
import primitives.Point;
import primitives.Vector;

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
}