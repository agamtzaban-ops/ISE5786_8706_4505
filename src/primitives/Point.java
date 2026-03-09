package primitives;

import java.util.Objects;

/**
 * Class Point is the basic class representing a point in a 3D system.
 * The point is represented by three coordinates (x, y, z).
 * @author [Your Name]
 */
public class Point {
    /** Coordinates of the point */
    protected final Double3 _xyz;

    /** Constant for the origin point (0,0,0) */
    public static final Point ZERO = new Point(Double3.ZERO);

    /**
     * Constructor to initialize Point based object with its three coordinates
     * @param x first coordinate
     * @param y second coordinate
     * @param z third coordinate
     */
    public Point(double x, double y, double z) {
        _xyz = new Double3(x, y, z);
    }

    /**
     * Constructor to initialize Point based object with a Double3 object
     * @param xyz triad of coordinates
     */
    protected Point(Double3 xyz) {
        _xyz = xyz;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        return (obj instanceof Point other)
                && _xyz.equals(other._xyz);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_xyz);
    }

    @Override
    public String toString() {
        return "Point" + _xyz;
    }

    /**
     * Subtracts two points into a new vector from the second point to the first point
     * @param other the second point
     * @return the vector from other to this
     */
    public Vector subtract(Point other) {
        return new Vector(_xyz.subtract(other._xyz));
    }

    /**
     * Adds a vector to the point
     * @param vector the vector to add
     * @return a new point
     */
    public Point add(Vector vector) {
        return new Point(_xyz.add(vector._xyz));
    }

    /**
     * Calculates the squared distance between two points
     * @param other the second point
     * @return the squared distance
     */
    public double distanceSquared(Point other) {
        double dx = _xyz._d1() - other._xyz._d1();
        double dy = _xyz._d2() - other._xyz._d2();
        double dz = _xyz._d3() - other._xyz._d3();
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Calculates the distance between two points
     * @param other the second point
     * @return the distance
     */
    public double distance(Point other) {
        return Math.sqrt(distanceSquared(other));
    }
}