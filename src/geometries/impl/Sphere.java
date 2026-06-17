package geometries.impl;

import geometries.api.AABB;
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
    /**
     * Calculates the U and V texture coordinates for a point on the sphere.
     * Uses spherical coordinates math to map the 3D point to a 2D texture space.
     *
     * @param p the 3D intersection point on the surface of the sphere
     * @return a double array containing [u, v], where:
     * u represents the horizontal axis (longitude) [0.0, 1.0]
     * v represents the vertical axis (latitude) [0.0, 1.0]
     */
    @Override
    public double[] getUV(Point p) {
        // Calculate the vector from the center of the sphere to the point
        Vector vToPoint = p.subtract(_center);

        // Normalize the vector to a unit length
        Vector normal = vToPoint.normalize();

        // Extract X, Y, Z components from the normalized vector
        double x = normal.getX();
        double y = normal.getY();
        double z = normal.getZ();

        // Calculate U (horizontal / longitude) mapping to [0.0, 1.0]
        double u = 0.5 + (Math.atan2(z, x) / (2 * Math.PI));

        // Calculate V (vertical / latitude) mapping to [0.0, 1.0]
        double v = 0.5 - (Math.asin(y) / Math.PI);

        return new double[]{u, v};
    }

    @Override
    public Vector getNormal(Point point) {
        return point.subtract(_center).normalize();
    }

    /**
     * Computes the bounding box of the sphere: a cube-like box of side
     * 2*radius, centered on the sphere's center. This is an exact (tight)
     * bounding box for a sphere — not an approximation.
     *
     * @return the axis-aligned bounding box of this sphere
     */
    @Override
    public AABB getBoundingBox() {
        return new AABB(
                _center.getX() - _radius, _center.getY() - _radius, _center.getZ() - _radius,
                _center.getX() + _radius, _center.getY() + _radius, _center.getZ() + _radius);
    }

    @Override
    protected List<Intersection> calcIntersectionsHelper(Ray ray,double maxDistance) {
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
            //Checking which of the distances are close enough
            boolean t1Valid = alignZero(t1 - maxDistance) <= 0;
            boolean t2Valid = alignZero(t2 - maxDistance) <= 0;
            if (t1Valid && t2Valid) {
                return List.of(new Intersection(this, ray.getPoint(t1)),
                        new Intersection(this, ray.getPoint(t2)));
            } else if (t1Valid) {
                return List.of(new Intersection(this, ray.getPoint(t1)));
            } else if (t2Valid) {
                return List.of(new Intersection(this, ray.getPoint(t2)));
            }
        } else if (t1 > 0) {
            if (alignZero(t1 - maxDistance) <= 0) {
                return List.of(new Intersection(this, ray.getPoint(t1)));
            }
        } else if (t2 > 0) {
            if (alignZero(t2 - maxDistance) <= 0) {
                return List.of(new Intersection(this, ray.getPoint(t2)));
            }
        }
        return null;
    }

}