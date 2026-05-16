package lighting;

import primitives.Color;
import primitives.Double3;

/**
 * AmbientLight represents a fixed-intensity, fixed-color light source
 * that affects all objects in the scene equally.
 */
public class AmbientLight extends Light {

    /**
     * A static constant representing "no light" (Black color).
     * Used as a default value to avoid null pointer exceptions.
     */
    public static final AmbientLight NONE = new AmbientLight(Color.BLACK, Double3.ZERO);

    /**
     * Primary constructor for AmbientLight.
     * Calculates the actual intensity as ia * ka.
     *
     * @param ia The original color/intensity of the ambient light
     * @param ka The attenuation constant for the ambient light
     */
    public AmbientLight(Color ia, Double3 ka) {
        super(ia.scale(ka));
    }

    /**
     * Constructor for AmbientLight with a default attenuation of 1 (ka = 1).
     * This is useful for simple cases or for the NONE constant.
     *
     * @param ia The original color/intensity of the ambient light
     */
    public AmbientLight(Color ia) {
        super(ia);
    }
}