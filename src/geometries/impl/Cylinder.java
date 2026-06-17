package geometries.impl;

import geometries.api.AABB;
import geometries.api.Intersectable.Intersection;
import primitives.Point;
import primitives.Ray;
import primitives.Vector;
import java.util.LinkedList;
import java.util.List;
import static primitives.Util.alignZero;
import static primitives.Util.isZero;

/**
 * Class Cylinder represents a finite cylinder in 3D space.
 * Inherits from Tube and adds height for finitude.
 */
public class Cylinder extends Tube {
    /** The height of the cylinder */
    private final double _height;

    /**
     * Constructor for Cylinder.
     * @param axis   the axis ray of the cylinder
     * @param radius the radius of the cylinder
     * @param height the height of the cylinder
     */
    public Cylinder(Ray axis, double radius, double height) {
        super(radius, axis);
        this._height = height;
    }

    @Override
    public Vector getNormal(Point point) {
        Point p0 = _axis.origin();
        Vector v = _axis.direction();

        // BV01: Check if the point is exactly at the bottom base center
        if (point.equals(p0)) return v.scale(-1);

        // Now it's safe to subtract
        Vector p0ToPoint = point.subtract(p0);

        // Calculate the projection distance on the axis
        double t = v.dotProduct(p0ToPoint);

        // Case 1: Point is on the bottom base (t is zero)
        if (isZero(t)) return v.scale(-1);

        // Case 2: Point is on the top base (t equals height)
        if (isZero(t - _height)) return v;

        // Case 3: Point is on the side shell
        Point o = p0.add(v.scale(t));
        return point.subtract(o).normalize();
    }

    /**
     * Computes a conservative (slightly oversized, but always valid) bounding
     * box for the finite cylinder.
     *
     * <p>An exact bounding box for a cylinder tilted arbitrarily in space
     * requires projecting the swept circle onto each axis, which is more
     * involved than is justified here. Instead, this method takes the union
     * of two spheres of the cylinder's radius, centered at the two cap
     * centers — a simple, always-correct superset of the real cylinder
     * volume. It is slightly looser than the tightest possible box, but
     * still dramatically smaller than "no box at all", which is what matters
     * for BVH culling.</p>
     *
     * @return a conservative axis-aligned bounding box for this cylinder
     */
    @Override
    public AABB getBoundingBox() {
        Point bottom = _axis.origin();
        Point top = bottom.add(_axis.direction().scale(_height));

        double minX = Math.min(bottom.getX(), top.getX()) - _radius;
        double minY = Math.min(bottom.getY(), top.getY()) - _radius;
        double minZ = Math.min(bottom.getZ(), top.getZ()) - _radius;
        double maxX = Math.max(bottom.getX(), top.getX()) + _radius;
        double maxY = Math.max(bottom.getY(), top.getY()) + _radius;
        double maxZ = Math.max(bottom.getZ(), top.getZ()) + _radius;

        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    protected List<Intersection> calcIntersectionsHelper(Ray ray, double maxDistance) {
        List<Intersection> result = new LinkedList<>();

        Point p0 = _axis.origin();
        Vector v  = _axis.direction();

        // Step 1: Curved side — filter tube intersections to the finite height band
        List<Intersection> tubeIntersections = super.calcIntersectionsHelper(ray, maxDistance);
        if (tubeIntersections != null) {
            for (Intersection intersection : tubeIntersections) {
                double t = alignZero(v.dotProduct(intersection.p.subtract(p0)));
                if (t > 0 && t < _height)
                    result.add(intersection);
            }
        }

        // Step 2: End caps — each cap is a disk lying in a plane with normal v.
        // Ray P(s) = origin + s*dir hits the plane (P - center)·v = 0
        // at s = (center - origin)·v / (dir·v).
        double dv = ray.direction().dotProduct(v);
        if (!isZero(dv)) {
            checkCap(ray, maxDistance, p0,                    v, dv, result);
            checkCap(ray, maxDistance, p0.add(v.scale(_height)), v, dv, result);
        }

        return result.isEmpty() ? null : result;
    }

    /**
     * Tests whether the ray hits the circular cap centered at {@code capCenter}
     * (lying in the plane with normal {@code v}) and, if so, adds the
     * intersection to {@code result}.
     */
    private void checkCap(Ray ray, double maxDistance, Point capCenter, Vector v,
                          double dv, List<Intersection> result) {
        double numer;
        try {
            numer = capCenter.subtract(ray.origin()).dotProduct(v);
        } catch (IllegalArgumentException e) {
            return; // ray origin coincides with cap center
        }
        double t = alignZero(numer / dv);
        if (t <= 0 || alignZero(t - maxDistance) > 0) return;

        Point p = ray.getPoint(t);
        double distSq;
        try {
            distSq = p.subtract(capCenter).lengthSquared();
        } catch (IllegalArgumentException e) {
            distSq = 0; // hit exactly at cap center — within radius
        }
        if (alignZero(distSq - _radius * _radius) <= 0)
            result.add(new Intersection(this, p));
    }
}