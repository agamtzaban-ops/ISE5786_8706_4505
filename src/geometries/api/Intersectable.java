package geometries.api;

import primitives.Point;
import primitives.Ray;
import java.util.List;

/**
 * Abstract class Intersectable is the base class for all geometries that can be intersected by a ray.
 */
public abstract class Intersectable {

    /**
     * Finds all intersection points between the geometry and the given ray.
     *
     * @param ray the ray intersecting the geometry
     * @return a list of intersection points, or null if there are no intersections
     */
    public abstract List<Point> findIntersections(Ray ray);
}
