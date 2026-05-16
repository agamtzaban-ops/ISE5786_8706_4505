package lighting;

import primitives.Color;

/**
 * Abstract class representing a light source.
 * It holds the original intensity of the light.
 */
abstract class Light {
    /**
     * The original intensity of the light source.
     * Moved from AmbientLight, set to protected and final.
     */
    protected final Color _intensity;

    /**
     * Protected constructor to initialize the light intensity field.
     *
     * @param intensity The original color intensity of the light.
     */
    protected Light(Color intensity) {
        this._intensity = intensity;
    }

    /**
     * Public getter for the light intensity.
     *
     * @return The original color intensity.
     */
    public Color getIntensity() {
        return _intensity;
    }
}