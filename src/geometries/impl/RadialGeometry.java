package geometries.impl;

import geometries.api.Geometry;

/**
 * Abstract class RadialGeometry is a base class for all geometries with a radius.
 */
public abstract class RadialGeometry extends Geometry {

    /**
     * The radius of the geometry
     */
    protected final double _radius;

    /**
     * The squared radius of the geometry
     */
    protected final double _radiusSquared;

    /**
     * Constructor to initialize the radius and calculate the squared radius.
     *
     * @param radius the radius of the geometry
     */
    public RadialGeometry(double radius) {
        _radius = radius;
        _radiusSquared = radius * radius;
    }
}