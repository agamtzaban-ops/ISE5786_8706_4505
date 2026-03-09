package primitives;

/**
 * Class Vector represents a vector in 3D space.
 *
 * @author [Your Name]
 */
public class Vector extends Point {

    /**
     * Constant for the Z-axis unit vector
     */
    public static final Vector AXIS_Z = new Vector(0, 0, 1);

    /**
     * Constructor to initialize Vector with three coordinates
     *
     * @param x first coordinate
     * @param y second coordinate
     * @param z third coordinate
     */
    public Vector(double x, double y, double z) {
        super(x, y, z);
        if (_xyz.equals(Double3.ZERO)) {
            throw new IllegalArgumentException("Vector(0,0,0) is not allowed");
        }
    }

    /**
     * Constructor to initialize Vector with a Double3 object
     *
     * @param xyz triad of coordinates
     */
    public Vector(Double3 xyz) {
        super(xyz);
        if (_xyz.equals(Double3.ZERO)) {
            throw new IllegalArgumentException("Vector(0,0,0) is not allowed");
        }
    }

    public Vector scale(double rhs) {
        return new Vector(_xyz.scale(rhs));
    }

    public double dotProduct(Vector other) {
        return _xyz._d1() * other._xyz._d1() +
                _xyz._d2() * other._xyz._d2() +
                _xyz._d3() * other._xyz._d3();
    }

    public Vector crossProduct(Vector other) {
        return new Vector(
                _xyz._d2() * other._xyz._d3() - _xyz._d3() * other._xyz._d2(),
                _xyz._d3() * other._xyz._d1() - _xyz._d1() * other._xyz._d3(),
                _xyz._d1() * other._xyz._d2() - _xyz._d2() * other._xyz._d1()
        );
    }

    public double lengthSquared() {
        return dotProduct(this);
    }

    public double length() {
        return Math.sqrt(lengthSquared());
    }

    public Vector normalize() {
        return scale(1d / length());
    }
}