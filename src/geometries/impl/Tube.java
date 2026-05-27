package geometries.impl;

import geometries.api.Intersectable.Intersection;
import primitives.Point;
import primitives.Ray;
import primitives.Vector;
import java.util.List;
import static primitives.Util.alignZero;
import static primitives.Util.isZero;

/**
 * Class Tube represents an infinite cylinder-like tube in 3D space.
 */
public class Tube extends RadialGeometry {

    /** The axis ray of the tube */
    protected final Ray _axis;

    /**
     * Constructor to initialize a tube with a radius and an axis ray.
     * * @param radius the radius of the tube
     * @param axis   the axis ray of the tube
     */
    public Tube(double radius, Ray axis) {
        super(radius);
        _axis = axis;
    }

    @Override
    public Vector getNormal(Point point) {
        Point p0 = _axis.origin();
        Vector v = _axis.direction();
        Vector p0ToPoint = point.subtract(p0);
        double t = v.dotProduct(p0ToPoint);
        if (isZero(t)) return p0ToPoint.normalize();
        Point o = p0.add(v.scale(t));
        return point.subtract(o).normalize();
    }

    /**
     * Helper method for calculating intersections using NVI pattern.
     *
     * @param ray the ray to check for intersections
     * @return list of intersections, or null if none
     */
    @Override
    protected List<Intersection> calcIntersectionsHelper(Ray ray, double maxDistance) {
        Vector v = ray.direction();
        Point p0 = ray.origin();
        Vector va = _axis.direction();
        Point pa = _axis.origin();

        // Step 1: Vector from ray origin to tube axis origin
        Vector deltaP = null;
        try {
            deltaP = p0.subtract(pa);
        } catch (IllegalArgumentException e) {
            // p0 == pa
        }

        // Step 2: Calculate coefficient 'a'
        double vDotVa = v.dotProduct(va);
        Vector vMinusVVaVa = v;
        if (!isZero(vDotVa)) {
            try {
                vMinusVVaVa = v.subtract(va.scale(vDotVa));
            } catch (IllegalArgumentException e) {
                return null; // Ray parallel to axis
            }
        }
        double a = vMinusVVaVa.lengthSquared();

        // Step 3: Calculate coefficients 'b' and 'c'
        double b = 0;
        double c = -_radius * _radius;

        if (deltaP != null) {
            double dpDotVa = deltaP.dotProduct(va);
            Vector dpMinusDpVaVa = deltaP;

            if (!isZero(dpDotVa)) {
                try {
                    dpMinusDpVaVa = deltaP.subtract(va.scale(dpDotVa));
                } catch (IllegalArgumentException e) {
                    dpMinusDpVaVa = null;
                }
            }

            if (dpMinusDpVaVa != null) {
                b = 2 * vMinusVVaVa.dotProduct(dpMinusDpVaVa);
                c += dpMinusDpVaVa.lengthSquared();
            }
        }

        // Step 4: Solve the quadratic equation
        double discriminant = alignZero(b * b - 4 * a * c);
        if (discriminant <= 0) return null;

        double sqrtDisc = Math.sqrt(discriminant);
        double t1 = alignZero((-b - sqrtDisc) / (2 * a));
        double t2 = alignZero((-b + sqrtDisc) / (2 * a));

        // We will check for each point individually whether it is positive and within the range of maxDistance        boolean t1Valid = t1 > 0 && alignZero(t1 - maxDistance) <= 0;
        boolean t1Valid = t1 > 0 && alignZero(t1 - maxDistance) <= 0;
        boolean t2Valid = t2 > 0 && alignZero(t2 - maxDistance) <= 0;

        // If both points are in the allowed range - we will return both
        if (t1Valid && t2Valid) {
            return List.of(
                    new Intersection(this, ray.getPoint(t1)),
                    new Intersection(this, ray.getPoint(t2))
            );
        }

       // If only the first one is valid
       if (t1Valid) return List.of(new Intersection(this, ray.getPoint(t1)));

        // If only the second one is valid
        if (t2Valid) return List.of(new Intersection(this, ray.getPoint(t2)));

        // if both points are not valid return null
        return null;
    }
}