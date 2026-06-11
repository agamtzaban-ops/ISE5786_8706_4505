package lighting;

import primitives.Color;
import primitives.Point;
import primitives.Vector;

/**
 * Represents a directional light source — a light infinitely far away that
 * illuminates the scene from a fixed direction with uniform intensity.
 *
 * <p>Because a directional light has no physical position or size, it always
 * produces hard shadows and {@link #getSize()} always returns {@code 0}.</p>
 *
 * <p>Examples: sunlight, moonlight.</p>
 */
public class DirectionalLight extends Light implements LightSource {

    /** The normalized direction vector of the light. */
    private final Vector _direction;

    /**
     * Constructs a directional light with the given intensity and direction.
     *
     * @param intensity the color intensity of the light
     * @param direction the direction the light travels (need not be normalized)
     */
    public DirectionalLight(Color intensity, Vector direction) {
        super(intensity);
        this._direction = direction.normalize();
    }

    /**
     * Returns the light intensity at a given point.
     * For directional light, intensity is uniform and does not attenuate with distance.
     *
     * @param p the point in the scene (unused)
     * @return the constant light intensity
     */
    @Override
    public Color getIntensity(Point p) {
        return _intensity;
    }

    /**
     * Returns the normalized direction vector from the light toward the given point.
     * For directional light, this is constant across the entire scene.
     *
     * @param p the point in the scene (unused)
     * @return the fixed normalized light direction
     */
    @Override
    public Vector getL(Point p) {
        return _direction;
    }

    /**
     * Returns positive infinity, since a directional light is modelled as
     * being infinitely far away.
     *
     * @param point the point in the scene (unused)
     * @return {@link Double#POSITIVE_INFINITY}
     */
    @Override
    public double getDistance(Point point) {
        return Double.POSITIVE_INFINITY;
    }

    /**
     * Returns {@code 0} — directional lights have no physical position or area,
     * so they cannot produce soft shadows.
     *
     * @return {@code 0} always
     */
    @Override
    public double getSize() {
        return 0;
    }
}