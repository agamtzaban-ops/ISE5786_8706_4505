package primitives;

import geometries.api.Intersectable.Intersection;
import java.util.List;
import java.util.Objects;

/**
 * Class Ray represents a semi-infinite line in space defined by a starting point and a direction.
 */
public final class Ray {

    /** The starting point of the ray */
    private final Point _origin;

    /** The normalized direction vector of the ray */
    private final Vector _direction;

    /**
     * Constructor for Ray.
     * The direction vector is normalized before it is saved.
     *
     * @param origin the starting point
     * @param direction the direction vector
     */
    public Ray(Point origin, Vector direction) {
        _origin = origin;
        _direction = direction.normalize();
    }

    /**
     * Gets the starting point of the ray.
     *
     * @return the origin point
     */
    public Point origin() {
        return _origin;
    }

    /**
     * Gets the normalized direction vector of the ray.
     *
     * @return the normalized direction vector
     */
    public Vector direction() {
        return _direction;
    }

    /**
     * Calculates a point on the ray's line at a given distance t from the head.
     * * @param t distance from ray origin
     * @return the calculated Point (P = P0 + t * v)
     */
    public Point getPoint(double t) {
        try {
            // Formula: P = P0 + t * v
            return _origin.add(_direction.scale(t));
        } catch (IllegalArgumentException e) {
            // If t is zero, scaling the vector fails (zero vector).
            // In this case, the point is simply the origin of the ray.
            return _origin;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        return (obj instanceof Ray other) &&
                _origin.equals(other._origin) &&
                _direction.equals(other._direction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_origin, _direction);
    }

    @Override
    public String toString() {
        return "Origin: " + _origin + ", Direction: " + _direction;
    }

    /**
     * Finds the closest intersection to the ray origin among a list of intersections.
     *
     * @param intersections list of intersections
     * @return the closest intersection, or null if the list is empty or null
     */
    public Intersection findClosestIntersection(List<Intersection> intersections) {
        if (intersections == null || intersections.isEmpty()) {
            return null;
        }

        Intersection closest = null;
        double minDistance = Double.POSITIVE_INFINITY;

        for (Intersection intersection : intersections) {
            // Efficiency: Using squared distance instead of regular distance (avoids sqrt)
            double distanceSquared = _origin.distanceSquared(intersection.p);

            if (distanceSquared < minDistance) {
                minDistance = distanceSquared;
                closest = intersection;
            }
        }

        return closest;
    }

    /**
     * Finds the closest point to the ray origin among a list of points.
     * Backward compatibility method that utilizes findClosestIntersection.
     *
     * @param points list of points, can be null
     * @return the closest point to the ray head, or null if the list is null
     */
    public Point findClosestPoint(List<Point> points) {
        return points == null ? null
                : findClosestIntersection(
                points.stream()
                        .map(point -> new Intersection(null, point))
                        .toList()
        ).p;
    }
    /**
     * Constant for shifting the ray origin to prevent self-intersection.
     */
    private static final double DELTA = 0.1;

    /**
     * Constructor for a secondary ray that shifts the origin slightly
     * along the normal to prevent self-intersection.
     *
     * @param head      The original intersection point.
     * @param direction The direction of the new ray.
     * @param n         The normal vector at the intersection point.
     */
    public Ray(Point head, Vector direction, Vector n) {
        double nv = primitives.Util.alignZero(n.dotProduct(direction));
        Vector delta = n.scale(nv > 0 ? DELTA : -DELTA);

        this._origin = nv == 0 ? head : head.add(delta);
        this._direction = direction.normalize();
    }
}