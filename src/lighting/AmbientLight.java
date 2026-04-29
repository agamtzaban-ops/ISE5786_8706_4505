package lighting; // This is the new package you created

import primitives.Color;

/**
 * AmbientLight represents a fixed-intensity, fixed-color light source
 * that affects all objects in the scene equally.
 */
public class AmbientLight {

    /** The intensity of the light (its color and brightness) */
    private final Color intensity;

    /** * A static constant representing "no light" (Black color).
     * Used as a default value to avoid null pointer exceptions.
     */
    public static final AmbientLight NONE = new AmbientLight(Color.BLACK, 0);

    /**
     * Primary constructor for AmbientLight.
     * It calculates the final intensity by multiplying the color (Ia)
     * by the attenuation factor (Ka).
     * * @param iA The basic color of the light
     * @param kA The attenuation factor (double)
     */
    public AmbientLight(Color iA, double kA) {
        // The formula from the lecture: Ip = Ia * Ka
        this.intensity = iA.scale(kA);
    }

    /**
     * Internal constructor for the NONE constant.
     * @param intensity the pre-calculated intensity
     */
    public AmbientLight(Color intensity) {
        this.intensity = intensity;
    }

    /**
     * Getter for the light intensity.
     * @return the intensity color
     */
    public Color getIntensity() {
        return intensity;
    }
}