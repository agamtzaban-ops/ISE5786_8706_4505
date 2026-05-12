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
     * Calculates the normal vector to the geometry at a given point.
     *
     * @param point the point on the geometry surface
     * @return the normal vector at the given point
     */
    public abstract Vector getNormal(Point point);
}