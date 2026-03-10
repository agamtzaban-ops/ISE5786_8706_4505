package geometries.impl;

import primitives.Point;

/**
 * Class Triangle represents a polygon with exactly three vertices.
 */
public final class Triangle extends Polygon {

    /**
     * Constructor to initialize a triangle with three points.
     * Calls the super constructor of Polygon.
     *
     * @param p1 first point
     * @param p2 second point
     * @param p3 third point
     */
    public Triangle(Point p1, Point p2, Point p3) {
        super(p1, p2, p3);
    }
}