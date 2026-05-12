package primitives;

/**
 * PDS (Plain Data Structure) class representing the material of a geometry.
 * Holds the attenuation factors for various lighting components.
 */
public class Material {

    /**
     * Ambient light attenuation factor.
     * Initialized to 1 (Double3.ONE) by default so existing tests won't break.
     */
    public Double3 kA = Double3.ONE;

    /**
     * Default constructor to satisfy Javadoc generator.
     */
    public Material() {}

    /**
     * Sets the ambient light attenuation factor.
     * Implements the chaining design pattern.
     *
     * @param kA the ambient attenuation factor as a Double3
     * @return this material instance
     */
    public Material setKa(Double3 kA) {
        this.kA = kA;
        return this;
    }

    /**
     * Sets the ambient light attenuation factor.
     * Implements the chaining design pattern.
     *
     * @param kA the ambient attenuation factor as a double
     * @return this material instance
     */
    public Material setKa(double kA) {
        this.kA = new Double3(kA);
        return this;
    }
}