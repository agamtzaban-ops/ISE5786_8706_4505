package geometries.impl;

import primitives.Point;
import primitives.Ray;
import primitives.Vector;

/**
 * Class Cylinder represents a finite tube with a defined height.
 */
public final class Cylinder extends Tube {

    /** The height of the cylinder */
    private final double _height;

    /**
     * Constructor to initialize a cylinder with a radius, an axis ray, and a height.
     *
     * @param radius the radius of the cylinder
     * @param axis the axis ray of the cylinder
     * @param height the height of the cylinder
     */
    public Cylinder(double radius, Ray axis, double height) {
        super(radius, axis);
        _height = height;
    }

    @Override
    public Vector getNormal(Point point) {
        Point p0 = _axis.origin();
        Vector v = _axis.direction();

        // If the point is exactly at the origin of the axis (center of bottom base)
        if (point.equals(p0)) {
            return v.scale(-1);
        }

        Vector p0ToPoint = point.subtract(p0);
        double t = v.dotProduct(p0ToPoint);

        // If the point is on the bottom base (t is 0)
        if (primitives.Util.isZero(t)) {
            return v.scale(-1); // The normal is opposite to the direction of the tube
        }

        // If the point is on the top base (t is exactly the height)
        if (primitives.Util.isZero(t - _height)) {
            return v; // The normal is in the same direction as the tube
        }

        // Otherwise, the point is on the side of the cylinder, so we use Tube's getNormal
        return super.getNormal(point);
    }
}