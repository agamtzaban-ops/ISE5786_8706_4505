package primitives;

/**
 * Class Vector represents a vector in 3D space, containing direction and magnitude.
 * It inherits from the Point class.
 */
public final class Vector extends Point {

    /**
     * Constant for the Z-axis unit vector
     */
    public static final Vector AXIS_Z = new Vector(0, 0, 1);

    /**
     * Constructor to initialize Vector with three double coordinates.
     * Throws an exception if the zero vector is created.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     * @throws IllegalArgumentException if the created vector is the zero vector
     */
    public Vector(double x, double y, double z) {
        super(x, y, z);
        if (_xyz.equals(Double3.ZERO)) {
            throw new IllegalArgumentException("Vector(0,0,0) is not allowed");
        }
    }

    /**
     * Constructor to initialize Vector with a Double3 object.
     * Throws an exception if the zero vector is created.
     *
     * @param xyz the Double3 containing the coordinates
     * @throws IllegalArgumentException if the created vector is the zero vector
     */
    public Vector(Double3 xyz) {
        super(xyz);
        if (_xyz.equals(Double3.ZERO)) {
            throw new IllegalArgumentException("Vector(0,0,0) is not allowed");
        }
    }

    /**
     * Adds a vector to this vector.
     *
     * @param other the vector to add
     * @return a new Vector representing the sum of the two vectors
     */
    public Vector add(Vector other) {
        return new Vector(_xyz.add(other._xyz));
    }

    /**
     * Scales the vector by a scalar number.
     *
     * @param rhs the scaling factor
     * @return a new scaled Vector
     */
    public Vector scale(double rhs) {
        return new Vector(_xyz.scale(rhs));
    }

    /**
     * Calculates the dot product of this vector and another vector.
     *
     * @param other the second vector
     * @return the algebraic dot product result
     */
    public double dotProduct(Vector other) {
        return _xyz._d1() * other._xyz._d1() +
                _xyz._d2() * other._xyz._d2() +
                _xyz._d3() * other._xyz._d3();
    }

    /**
     * Calculates the cross product of this vector and another vector.
     *
     * @param other the second vector
     * @return a new Vector representing the cross product (orthogonal to both)
     */
    public Vector crossProduct(Vector other) {
        return new Vector(
                _xyz._d2() * other._xyz._d3() - _xyz._d3() * other._xyz._d2(),
                _xyz._d3() * other._xyz._d1() - _xyz._d1() * other._xyz._d3(),
                _xyz._d1() * other._xyz._d2() - _xyz._d2() * other._xyz._d1()
        );
    }

    /**
     * Calculates the squared length (magnitude) of the vector.
     *
     * @return the squared length
     */
    public double lengthSquared() {
        return dotProduct(this);
    }

    /**
     * Calculates the exact length (magnitude) of the vector.
     *
     * @return the length
     */
    public double length() {
        return Math.sqrt(lengthSquared());
    }

    /**
     * Normalizes the vector (changes its length to 1 while keeping its direction).
     *
     * @return a new normalized Vector
     */
    public Vector normalize() {
        return scale(1d / length());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof Vector other) {
            return super.equals(other);
        }
        return false;
    }

    @Override
    public String toString() {
        return "Vector " + _xyz;
    }
}