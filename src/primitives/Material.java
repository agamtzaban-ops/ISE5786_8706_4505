package primitives;

/**
 * Material class for Phong Reflection Model.
 * Updated setter names to match the course's provided tests.
 */
public class Material {
    /** Diffuse reflection coefficient */
    public Double3 kD = Double3.ZERO;

    /** Specular reflection coefficient */
    public Double3 kS = Double3.ZERO;

    /** Transparency (Transmission) attenuation factor */
    public Double3 kT = Double3.ZERO;

    /** Reflection attenuation factor */
    public Double3 kR = Double3.ZERO;

    /** Shininess factor */
    public int nShininess = 0;

    /**
     * Setter for kD (Double3) - Method Chaining
     * @param kD diffuse coefficient
     * @return material object
     */
    public Material setKD(Double3 kD) {
        this.kD = kD;
        return this;
    }

    /**
     * Setter for kD (double) - Method Chaining
     * @param kD diffuse coefficient
     * @return material object
     */
    public Material setKD(double kD) {
        this.kD = new Double3(kD);
        return this;
    }

    /**
     * Setter for kS (Double3) - Method Chaining
     * @param kS specular coefficient
     * @return material object
     */
    public Material setKS(Double3 kS) {
        this.kS = kS;
        return this;
    }

    /**
     * Setter for kS (double) - Method Chaining
     * @param kS specular coefficient
     * @return material object
     */
    public Material setKS(double kS) {
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

    /**
     * Setter for kT (Double3) - Transparency - Method Chaining
     * @param kT transparency coefficient
     * @return material object
     */
    public Material setKT(Double3 kT) {
        this.kT = kT;
        return this;
    }

    /**
     * Setter for kT (double) - Transparency - Method Chaining
     * @param kT transparency coefficient
     * @return material object
     */
    public Material setKT(double kT) {
        this.kT = new Double3(kT);
        return this;
    }

    /**
     * Setter for kR (Double3) - Reflection - Method Chaining
     * @param kR reflection coefficient
     * @return material object
     */
    public Material setKR(Double3 kR) {
        this.kR = kR;
        return this;
    }

    /**
     * Setter for kR (double) - Reflection - Method Chaining
     * @param kR reflection coefficient
     * @return material object
     */
    public Material setKR(double kR) {
        this.kR = new Double3(kR);
        return this;
    }

    /* --- Backwards Compatibility for Tests & Loader --- */
    public Material setKA(Double3 ka) { return this; }
    public Material setKA(double ka) { return this; }

    /* --- Backwards Compatibility for Old Tests (Stages 6 & 7) --- */

    public Material setKd(Double3 kD) { return setKD(kD); }
    public Material setKd(double kD) { return setKD(kD); }

    public Material setKs(Double3 kS) { return setKS(kS); }
    public Material setKs(double kS) { return setKS(kS); }

    public Material setKt(Double3 kT) { return setKT(kT); }
    public Material setKt(double kT) { return setKT(kT); }

    public Material setKr(Double3 kR) { return setKR(kR); }
    public Material setKr(double kR) { return setKR(kR); }
    public Material setKa(Double3 kA) { return this; }
    public Material setKa(double kA) { return this; }
}