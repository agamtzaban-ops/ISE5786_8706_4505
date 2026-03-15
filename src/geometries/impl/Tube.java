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
        return null;
    }
}