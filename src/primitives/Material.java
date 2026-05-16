package primitives;

/**
 * Material class for Phong Reflection Model.
 */
public class Material {
    /** Diffuse reflection coefficient */
    public Double3 kD = Double3.ZERO;
    /** Specular reflection coefficient */
    public Double3 kS = Double3.ZERO;
    /** Shininess factor */
    public int nShininess = 0;

    /**
     * Setter for kD (Double3) - Method Chaining
     * @param kD diffuse coefficient
     * @return material object
     */
    public Material setKd(Double3 kD) {
        this.kD = kD;
        return this;
    }

    /**
     * Setter for kD (double) - Method Chaining
     * @param kD diffuse coefficient
     * @return material object
     */
    public Material setKd(double kD) {
        this.kD = new Double3(kD);
        return this;
    }

    /**
     * Setter for kS (Double3) - Method Chaining
     * @param kS specular coefficient
     * @return material object
     */
    public Material setKs(Double3 kS) {
        this.kS = kS;
        return this;
    }

    /**
     * Setter for kS (double) - Method Chaining
     * @param kS specular coefficient
     * @return material object
     */
    public Material setKs(double kS) {
        this.kS = new Double3(kS);
        return this;
    }

    /**
     * Setter for nShininess - Method Chaining
     * @param nShininess shininess factor
     * @return material object
     */
    public Material setShininess(int nShininess) {
        this.nShininess = nShininess;
        return this;
    }
    /* --- Backwards Compatibility for Tests & Loader --- */
    // We add these but don't save the value to keep the class "clean" as per instructions
    public Material setKa(Double3 ka) { return this; }
    public Material setKa(double ka) { return this; }

}