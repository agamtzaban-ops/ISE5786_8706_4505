package geometries.impl;

import geometries.api.Intersectable.Intersection;
import primitives.Point;
import primitives.Ray;
import primitives.Vector;
import java.util.List;
import static primitives.Util.alignZero;

/**
 * Class Sphere represents a 3D sphere shape.
 */
public final class Sphere extends RadialGeometry {

    /** The center point of the sphere */
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

    @Override
    protected List<Intersection> calcIntersectionsHelper(Ray ray) {
        Point p0 = ray.origin();
        Vector v = ray.direction();

        // Vector from ray origin to sphere center: u = O - P0
        Vector u;
        try {
            u = _center.subtract(p0);
        } catch (IllegalArgumentException e) {
            // Ray starts at the center of the sphere, so it hits the surface at t = radius
            return List.of(new Intersection(this, ray.getPoint(_radius)));
        }

        double tm = v.dotProduct(u);
        double dSquared = u.lengthSquared() - tm * tm;
        double thSquared = _radius * _radius - dSquared;

        // If distance from center to ray is greater than radius, no intersections
        if (alignZero(thSquared) <= 0) return null;

        double th = Math.sqrt(thSquared);

        // Distances from ray origin to intersection points
        double t1 = alignZero(tm - th);
        double t2 = alignZero(tm + th);

        // Only return points that are in the ray's direction (t > 0)
        if (t1 > 0 && t2 > 0) {
            return List.of(
                    new Intersection(this, ray.getPoint(t1)),
                    new Intersection(this, ray.getPoint(t2))
            );
        }
        if (t1 > 0) {
            return List.of(new Intersection(this, ray.getPoint(t1)));
        }
        if (t2 > 0) {
            return List.of(new Intersection(this, ray.getPoint(t2)));
        }

        return null;
    }
}