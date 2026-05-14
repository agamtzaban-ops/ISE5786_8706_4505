package primitives;

/**
 * PDS (Plain Data Structure) class representing the material of a geometry.
 * Holds the attenuation factors for various lighting components (Stage 6).
 */
public class Material {

    /** Ambient light attenuation factor */
    public Double3 kA = Double3.ONE;

    /** Diffuse light attenuation factor (Stage 6) */
    public Double3 kD = Double3.ZERO;

    /** Specular light attenuation factor (Stage 6) */
    public Double3 kS = Double3.ZERO;

    /** Shininess factor for specular highlights (Stage 6) */
    public int nShininess = 0;

    /** Default constructor to satisfy Javadoc generator */
    public Material() {}

    /**
     * Sets the ambient light attenuation factor.
     * @param kA the ambient attenuation factor as a Double3
     * @return this material instance
     */
    public Material setKa(Double3 kA) {
        this.kA = kA;
        return this;
    }

    /**
     * Sets the ambient light attenuation factor.
     * @param kA the ambient attenuation factor as a double
     * @return this material instance
     */
    public Material setKa(double kA) {
        this.kA = new Double3(kA);
        return this;
    }

    /**
     * Sets the diffuse light attenuation factor.
     * @param kD the diffuse attenuation factor as a Double3
     * @return this material instance
     */
    public Material setKd(Double3 kD) {
        this.kD = kD;
        return this;
    }

    /**
     * Sets the diffuse light attenuation factor.
     * @param kD the diffuse attenuation factor as a double
     * @return this material instance
     */
    public Material setKd(double kD) {
        this.kD = new Double3(kD);
        return this;
    }

    /**
     * Sets the specular light attenuation factor.
     * @param kS the specular attenuation factor as a Double3
     * @return this material instance
     */
    public Material setKs(Double3 kS) {
        this.kS = kS;
        return this;
    }

    /**
     * Sets the specular light attenuation factor.
     * @param kS the specular attenuation factor as a double
     * @return this material instance
     */
    public Material setKs(double kS) {
        this.kS = new Double3(kS);
        return this;
    }

    /**
     * Sets the shininess factor.
     * @param nShininess the shininess factor integer
     * @return this material instance
     */
    public Material setShininess(int nShininess) {
        this.nShininess = nShininess;
        return this;
    }
}