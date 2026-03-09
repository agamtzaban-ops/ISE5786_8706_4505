package primitives;

import java.util.Objects;

/**
 * Class Ray represents a point and a direction vector.
 *
 * @author [Your Name]
 */
public class Ray {
    private final Point _p0;
    private final Vector _dir;

    public Ray(Point p0, Vector dir) {
        _p0 = p0;
        _dir = dir.normalize();
    }

    /**
     * Getter for the direction (Matches Main's expectation)
     *
     * @return the normalized direction vector
     */
    public Vector direction() {
        return _dir;
    }

    /**
     * Getter for the start point
     *
     * @return the start point
     */
    public Point getP0() {
        return _p0;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        return (obj instanceof Ray other) && _p0.equals(other._p0) && _dir.equals(other._dir);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_p0, _dir);
    }
}