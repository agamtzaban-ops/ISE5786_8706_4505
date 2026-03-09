package primitives;

import java.util.Objects;

/**
 * Class Ray represents a set of points on a line that starts at a point P0
 * and goes in a specific direction V.
 * @author [השם שלך]
 */
public class Ray {
    /** The starting point of the ray */
    private final Point _p0;
    /** The direction of the ray (normalized) */
    private final Vector _dir;

    /**
     * Constructor to initialize Ray with a point and a direction.
     * The direction vector is automatically normalized.
     * @param p0  the starting point
     * @param dir the direction vector
     */
    public Ray(Point p0, Vector dir) {
        _p0 = p0;
        _dir = dir.normalize();
    }

    /**
     * Getter for the starting point.
     * @return the starting point
     */
    public Point getP0() {
        return _p0;
    }

    /**
     * Getter for the direction.
     * @return the direction vector
     */
    public Vector getDir() {
        return _dir;
    }

    /**
     * Calculates a point on the ray at a certain distance.
     * P = p0 + t*v
     * @param t distance from p0
     * @return the point
     */
    public Point getPoint(double t) {
        return _p0.add(_dir.scale(t));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        return (obj instanceof Ray other) &&
                _p0.equals(other._p0) &&
                _dir.equals(other._dir);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_p0, _dir);
    }

    @Override
    public String toString() {
        return "Ray: p0=" + _p0 + ", dir=" + _dir;
    }
}