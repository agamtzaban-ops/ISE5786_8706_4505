package geometries.impl;

import primitives.Point;
import primitives.Ray;
import primitives.Vector;

/**
 * Class Tube represents an infinite cylinder-like tube in 3D space.
 */
public class Tube extends RadialGeometry {

    /** The axis ray of the tube */
    protected final Ray _axis;

    /**
     * Constructor to initialize a tube with a radius and an axis ray.
     *
     * @param radius the radius of the tube
     * @param axis the axis ray of the tube
     */
    public Tube(double radius, Ray axis) {
        super(radius);
        _axis = axis;
    }

    @Override
    public Vector getNormal(Point point) {
        Point p0 = _axis.origin();
        Vector v = _axis.direction();

        // The vector from the origin of the axis to the given point
        Vector p0ToPoint = point.subtract(p0);

        // The projection of p0ToPoint on the axis ray (t = v * p0ToPoint)
        double t = v.dotProduct(p0ToPoint);

        // If t is close to zero, the point is exactly opposite to the ray's origin
        if (primitives.Util.isZero(t)) {
            return p0ToPoint.normalize();
        }

        // Calculate the center point O on the axis that is opposite to the given point
        Point o = p0.add(v.scale(t));

        // The normal is the normalized vector from O to the given point
        return point.subtract(o).normalize();
    }
}