package primitives;

/**
 * Class Vector is the basic object representing a direction and magnitude in a 3D system.
 * A vector is defined by its coordinates (x, y, z) relative to the origin (0,0,0).
 * It inherits from Point.
 * * @author [Your Name]
 */
public class Vector extends Point {

    /**
     * Constructor to initialize Vector based on its three coordinates.
     * * @param x first coordinate
     * @param y second coordinate
     * @param z third coordinate
     * @throws IllegalArgumentException if the vector is the zero vector
     */
    public Vector(double x, double y, double z) {
        super(x, y, z);
        if (_xyz.equals(Double3.ZERO)) {
            throw new IllegalArgumentException("Vector(0,0,0) is not allowed");
        }
    }

    /**
     * Constructor to initialize Vector based on a Double3 object.
     * * @param xyz triad of coordinates
     * @throws IllegalArgumentException if the vector is the zero vector
     */
    protected Vector(Double3 xyz) {
        super(xyz);
        if (_xyz.equals(Double3.ZERO)) {
            throw new IllegalArgumentException("Vector(0,0,0) is not allowed");
        }
    }

    /**
     * Scales this vector by a scalar value.
     * * @param rhs the scalar multiplier
     * @return a new vector scaled by rhs
     */
    public Vector scale(double rhs) {
        return new Vector(_xyz.scale(rhs));
    }

    /**
     * Calculates the dot product of this vector with another vector.
     * * @param other the other vector
     * @return the dot product result (scalar)
     */
    public double dotProduct(Vector other) {
        // Accessing record components using function calls: _d1(), _d2(), _d3()
        return _xyz._d1() * other._xyz._d1() +
                _xyz._d2() * other._xyz._d2() +
                _xyz._d3() * other._xyz._d3();
    }

    /**
     * Calculates the cross product of this vector with another vector.
     * * @param other the other vector
     * @return a new vector perpendicular to both vectors
     */
    public Vector crossProduct(Vector other) {
        return new Vector(
                _xyz._d2() * other._xyz._d3() - _xyz._d3() * other._xyz._d2(),
                _xyz._d3() * other._xyz._d1() - _xyz._d1() * other._xyz._d3(),
                _xyz._d1() * other._xyz._d2() - _xyz._d2() * other._xyz._d1()
        );
    }

    /**
     * Calculates the squared length of the vector.
     * * @return squared length
     */
    public double lengthSquared() {
        return dotProduct(this);
    }

    /**
     * Calculates the length of the vector.
     * * @return length (magnitude)
     */
    public double length() {
        return Math.sqrt(lengthSquared());
    }

    /**
     * Returns a normalized vector (unit vector) in the same direction.
     * * @return a new unit vector
     */
    public Vector normalize() {
        return scale(1d / length());
    }

    @Override
    public String toString() {
        return "Vector" + super.toString();
    }
}