package geometries.impl;

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

    @Override
    protected List<Intersection> calcIntersectionsHelper(Ray ray,double maxDistance) {
        List<Intersection> result = new LinkedList<>();

        // Step 1: Get intersections with the infinite tube
        // Exceptional call to super's helper is required for finite logic in NVI
        List<Intersection> tubeIntersections = super.calcIntersectionsHelper(ray,maxDistance);

        // Step 2: Filter tube intersections by height boundaries
        if (tubeIntersections != null) {
            Point p0 = _axis.origin();
            Vector v = _axis.direction();
            for (Intersection intersection : tubeIntersections) {
                double t = alignZero(v.dotProduct(intersection.p.subtract(p0)));
                // Only keep points strictly within the cylinder's height
                if (t > 0 && t < _height) {
                    result.add(intersection);
                }
            }
        }

        return result.isEmpty() ? null : result;
    }
}