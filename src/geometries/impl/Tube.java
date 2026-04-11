package geometries.impl;

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
     */
    public Tube(double radius, Ray axis) {
        super(radius);
        _axis = axis;
    }

    @Override
    public Vector getNormal(Point point) {
        // ... (הקוד של ה-Normal שכתבת כבר, תשאירי אותו כמו שהוא)
        Point p0 = _axis.origin();
        Vector v = _axis.direction();
        Vector p0ToPoint = point.subtract(p0);
        double t = v.dotProduct(p0ToPoint);
        if (isZero(t)) return p0ToPoint.normalize();
        Point o = p0.add(v.scale(t));
        return point.subtract(o).normalize();
    }

    @Override
    public List<Point> findIntersections(Ray ray) {
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

        // Step 5: Return only positive intersections
        if (t1 > 0 && t2 > 0) return List.of(ray.getPoint(t1), ray.getPoint(t2));
        if (t1 > 0) return List.of(ray.getPoint(t1));
        if (t2 > 0) return List.of(ray.getPoint(t2));

        return null;
    }
}