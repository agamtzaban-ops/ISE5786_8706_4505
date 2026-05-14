package lighting;

import primitives.Color;

/**
 * AmbientLight represents a fixed-intensity, fixed-color light source
 * that affects all objects in the scene equally.
 */
public class AmbientLight {

    /** The intensity of the light */
    private final Color intensity;

    /**
     * A static constant representing "no light" (Black color).
     * Used as a default value to avoid null pointer exceptions.
     */
    public static final AmbientLight NONE = new AmbientLight(Color.BLACK);

    /**
     * Primary constructor for AmbientLight.
     *
     * @param intensity The color/intensity of the ambient light
     */
    public AmbientLight(Color intensity) {
        this.intensity = intensity;
    }

    /**
     * Getter for the light intensity.
     *
     * @return the intensity color
     */
    public Color getIntensity() {
        return intensity;
    }
}