package geometries.api;

import primitives.Point;
import primitives.Vector;

/**
 * Abstract class Geometry is the base class for all geometric objects.
 */
public abstract class Geometry extends Intersectable {

    /**
     * Calculates the normal vector to the geometry at a given point.
     *
     * @param point the point on the geometry surface
     * @return the normal vector at the given point
     */
    public abstract Vector getNormal(Point point);
}