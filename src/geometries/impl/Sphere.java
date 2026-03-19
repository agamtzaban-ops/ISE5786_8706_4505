package geometries.impl;

import primitives.Point;
import primitives.Vector;

/**
 * Class Sphere represents a 3D sphere shape.
 */
public final class Sphere extends RadialGeometry {

    /**
     * The center point of the sphere
     */
    private final Point _center;

    /**
     * Constructor to initialize a sphere with a center point and a radius.
     *
     * @param center the center point of the sphere
     * @param radius the radius of the sphere
     */
    public Sphere(Point center, double radius) {
        super(radius);
        _center = center;
    }

    @Override
    public Vector getNormal(Point point) {
        return point.subtract(_center).normalize();
    }
}