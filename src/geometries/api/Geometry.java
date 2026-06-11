package geometries.api;

import primitives.Color;
import primitives.Material;
import primitives.Point;
import primitives.Vector;

/**
 * Abstract class Geometry is the base class for all geometric objects.
 */
public abstract class Geometry extends Intersectable {

    /**
     * The emission color of the geometry.
     * Initialized to black by default (no emission).
     */
    private Color _emission = Color.BLACK;

    /**
     * The material of the geometry.
     * Initialized to a default material to avoid breaking existing tests.
     */
    private Material _material = new Material();

    /**
     * Gets the emission color of the geometry.
     *
     * @return the emission color
     */
    public Color getEmission() {
        return _emission;
    }

    /**
     * Sets the emission color of the geometry.
     * Implements the chaining design pattern.
     *
     * @param emission the new emission color
     * @return this geometry instance
     */
    public Geometry setEmission(Color emission) {
        this._emission = emission;
        return this;
    }

    /**
     * Gets the material of the geometry.
     *
     * @return the material
     */
    public Material getMaterial() {
        return _material;
    }

    /**
     * Sets the material of the geometry.
     * Implements the chaining design pattern.
     *
     * @param material the new material
     * @return this geometry instance
     */
    public Geometry setMaterial(Material material) {
        this._material = material;
        return this;
    }

    /**
     * Marks this geometry as a light-source body (e.g. the Sun sphere).
     * Shadow rays are not blocked by light-source geometries: the Sun should
     * never cast a shadow on other objects in the scene.
     */
    private boolean _isLightSource = false;

    /** Returns {@code true} if this geometry is a light-source body. */
    public boolean isLightSource() { return _isLightSource; }

    /**
     * Marks this geometry as a light-source body so it is excluded from
     * shadow-ray intersection tests.
     *
     * @return this geometry (for method chaining)
     */
    public Geometry setLightSource() {
        _isLightSource = true;
        return this;
    }

    /**
     * Calculates the normal vector to the geometry at a given point.
     *
     * @param point the point on the geometry surface
     * @return the normal vector at the given point
     */
    public abstract Vector getNormal(Point point);
    /**
     * Calculates the U and V coordinates for a given intersection point.
     * This is used for 2D texture mapping on 3D geometries.
     *
     * @param p the 3D point on the geometry's surface
     * @return a double array of size 2 containing [u, v], where 0.0 <= u, v <= 1.0
     */
    public abstract double[] getUV(Point p);
}