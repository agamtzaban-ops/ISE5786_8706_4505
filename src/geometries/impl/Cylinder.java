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
        return null;
    }
}