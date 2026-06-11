package lighting;

import primitives.Color;

/**
 * Abstract base class representing a light source in the scene.
 *
 * <p>Holds the original intensity of the light. All concrete light types
 * ({@link AmbientLight}, {@link DirectionalLight}, {@link PointLight}, etc.)
 * extend this class.</p>
 */
abstract class Light {

    /**
     * The original color intensity of this light source.
     * Set once at construction and never mutated.
     */
    protected final Color _intensity;

    /**
     * Initializes the light with the given intensity.
     *
     * @param intensity the color intensity of the light (must not be {@code null})
     */
    protected Light(Color intensity) {
        this._intensity = intensity;
    }

    /**
     * Returns the original color intensity of this light source.
     *
     * @return the light's color intensity
     */
    public Color getIntensity() {
        return _intensity;
    }
}