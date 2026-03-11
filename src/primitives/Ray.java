package primitives;

import java.util.Objects;

/**
 * Class Ray represents a semi-infinite line in space defined by a starting point and a direction.
 */
public final class Ray {

    /**
     * The starting point of the ray
     */
    private final Point _origin;

    /**
     * The normalized direction vector of the ray
     */
    private final Vector _direction;

    /**
     * Constructor for Ray.
     * The direction vector is normalized before it is saved.
     *
     * @param origin    the starting point
     * @param direction the direction vector
     */
    public Ray(Point origin, Vector direction) {
        _origin = origin;
        _direction = direction.normalize();
    }

    /**
     * Gets the normalized direction vector of the ray.
     * This method is provided specifically for testing purposes.
     *
     * @return the normalized direction vector
     */
    public Vector direction() {
        return _direction;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        return (obj instanceof Ray other) && _origin.equals(other._origin) && _direction.equals(other._direction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_origin, _direction);
    }

    @Override
    public String toString() {
        return "Ray [Origin=" + _origin + ", Direction=" + _direction + "]";
    }
}