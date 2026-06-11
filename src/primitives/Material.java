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

    // ── Texture ──────────────────────────────────────────────────────────────

    /**
     * Optional diffuse texture map.
     * When non-null, the texture color replaces the geometry's fixed emission
     * as the base color for the Phong diffuse term.
     * Leave null (default) to keep the original emission-based shading.
     */
    public Texture texture = null;

    /**
     * Sets a diffuse texture map on this material.
     * The texture will modulate the Phong diffuse term (realistic planet mode).
     * @param texture the texture to apply (may be null to remove)
     * @return this material (for method chaining)
     */
    public Material setTexture(Texture texture) {
        this.texture = texture;
        return this;
    }

    // ── Glow falloff ─────────────────────────────────────────────────────────

    /**
     * Limb-darkening / glow-falloff exponent for self-luminous bodies (e.g. Sun).
     *
     * <p>When &gt; 0, the emission colour is multiplied by {@code |N·V|^glowFalloff}
     * before being returned.  This creates a smooth fade from a bright centre
     * (where the surface normal points straight toward the camera) to black at
     * the silhouette edge, mimicking a star's atmospheric limb-darkening and
     * corona without any hard geometric boundaries.</p>
     *
     * <ul>
     *   <li>0.0  – no falloff (flat emission, default)</li>
     *   <li>0.25 – very wide, barely-darkened edges (inner core)</li>
     *   <li>0.5  – moderate gradient (inner corona)</li>
     *   <li>1.0  – linear fade (outer haze)</li>
     *   <li>1.5+ – sharp edge — only the very centre is bright</li>
     * </ul>
     */
    public double glowFalloff = 0.0;

    /**
     * Sets the limb-darkening exponent — see {@link #glowFalloff}.
     * @param exponent falloff power (0 = disabled)
     * @return this material (for method chaining)
     */
    public Material setGlowFalloff(double exponent) {
        this.glowFalloff = exponent;
        return this;
    }

    /**
     * When true, the texture replaces the geometry's emission color and is
     * NOT affected by scene lighting (always shown at full brightness).
     * Use this for self-lit surfaces: skybox, star-fields, sun face.
     *
     * When false (default), the texture modulates the diffuse (kD) term:
     * lit side shows full texture colors, dark side fades to black.
     * Use this for all physically shaded planets.
     */
    public boolean emissionTexture = false;

    /**
     * Marks this texture as an emission (self-lit) texture — see {@link #emissionTexture}.
     * @return this material (for method chaining)
     */
    public Material setEmissionTexture() {
        this.emissionTexture = true;
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