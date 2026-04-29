package primitives;
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
     * Finds the closest point to the ray origin from a list of points.
     * @param points list of intersection points, can be null
     * @return the closest point to the ray head, or null if the list is null
     */
    public Point findClosestPoint(List<Point> points) {
        if (points == null) {
            return null;
        }

        Point closest = null;
        // Using Double.POSITIVE_INFINITY to avoid double access to the first element
        double minDistance = Double.POSITIVE_INFINITY;

        for (Point p : points) {
            // Efficiency: Using squared distance instead of regular distance (avoids sqrt)
            double distanceSquared = _origin.distanceSquared(p);

            if (distanceSquared < minDistance) {
                minDistance = distanceSquared;
                closest = p;
            }
        }
        return closest;
    }
}