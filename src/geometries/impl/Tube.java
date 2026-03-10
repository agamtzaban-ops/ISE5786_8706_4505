package geometries.impl;

import primitives.Point;
import primitives.Ray;
import primitives.Vector;

/**
 * Class Tube represents an infinite cylinder-like tube in 3D space.
 */
public class Tube extends RadialGeometry {

    /** The axis ray of the tube */
    protected final Ray _axisRay;

    /**
     * Constructor to initialize a tube with a radius and an axis ray.
     *
     * @param radius the radius of the tube
     * @param axisRay the axis ray of the tube
     */
    public Tube(double radius, Ray axisRay) {
        super(radius);
        _axisRay = axisRay;
    }

    @Override
    public Vector getNormal(Point point) {
        // According to stage 1 requirements, this method returns null
        return null;
    }
}