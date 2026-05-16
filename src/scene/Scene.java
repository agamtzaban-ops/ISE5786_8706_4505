package scene;

import geometries.impl.Geometries;
import lighting.AmbientLight;
import lighting.LightSource;
import primitives.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Scene class represents a 3D scene.
 */
public class Scene {
    /** The name of the scene */
    public final String name;
    /** The background color of the scene */
    public Color background = Color.BLACK;
    /** The ambient light of the scene */
    public AmbientLight ambientLight = AmbientLight.NONE;
    /** The collection of geometries in the scene */
    public geometries.impl.Geometries geometries = new geometries.impl.Geometries();
    /** Light sources in the scene */
    public List<LightSource> lights = new ArrayList<>();

    /**
     * Constructor.
     * @param name the name of the scene
     */
    public Scene(String name) {
        this.name = name;
    }

    /**
     * Setter for background color.
     * @param background the background color
     * @return the scene object itself
     */
    public Scene setBackground(Color background) {
        this.background = background;
        return this;
    }

    /**
     * Setter for ambient light.
     * @param ambientLight the ambient light
     * @return the scene object itself
     */
    public Scene setAmbientLight(AmbientLight ambientLight) {
        this.ambientLight = ambientLight;
        return this;
    }

    /**
     * Setter for geometries.
     * @param geometries the geometries collection
     * @return the scene object itself
     */
    public Scene setGeometries(geometries.impl.Geometries geometries) {
        this.geometries = geometries;
        return this;
    }

    /**
     * Adds light sources to the scene.
     * @param lights variable number of light sources
     * @return the scene itself
     */
    public Scene setLights(LightSource... lights) {
        if (lights != null)
        {
            for (LightSource light : lights)
            {
                this.lights.add(light);
            }
        }
        return this;
    }
}