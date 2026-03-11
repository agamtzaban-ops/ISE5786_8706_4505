package primitives;

import java.util.Objects;

/**
 * Class Point represents a basic point in a 3D coordinate system.
 */
public class Point {

    /**
     * The 3D coordinates of the point
     */
    protected final Double3 _xyz;

    /**
     * Constant representing the origin point (0,0,0)
     */
    public static final Point ZERO = new Point(Double3.ZERO);

    /**
     * Constructor to initialize Point with three double coordinates.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     */
    public Point(double x, double y, double z) {
        _xyz = new Double3(x, y, z);
    }

    /**
     * Constructor to initialize Point with a Double3 object.
     *
     * @param xyz the Double3 containing the coordinates
     */
    public Point(Double3 xyz) {
        _xyz = xyz;
    }

    /**
     * Subtracts another point from this point to create a new vector.
     *
     * @param other the point to subtract
     * @return a new Vector representing the difference from the other point to this point
     */
    public Vector subtract(Point other) {
        return new Vector(_xyz.subtract(other._xyz));
    }

    /**
     * Adds a vector to this point.
     *
     * @param vector the vector to add
     * @return a new Point after the addition
     */
    public Point add(Vector vector) {
        return new Point(_xyz.add(vector._xyz));
    }

    /**
     * Calculates the squared distance between this point and another point.
     *
     * @param other the second point
     * @return the squared distance between the two points
     */
    public double distanceSquared(Point other) {
        double dx = _xyz._d1() - other._xyz._d1();
        double dy = _xyz._d2() - other._xyz._d2();
        double dz = _xyz._d3() - other._xyz._d3();
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Calculates the distance between this point and another point.
     *
     * @param other the second point
     * @return the exact distance between the two points
     */
    public double distance(Point other) {
        return Math.sqrt(distanceSquared(other));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        return (obj instanceof Point other) && _xyz.equals(other._xyz);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_xyz);
    }

    @Override
    public String toString() {
        return "Point " + _xyz;
    }
}