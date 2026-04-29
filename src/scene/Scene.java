package scene;

import java.util.LinkedList;
import java.util.List;
import geometries.impl.Geometries;
import lighting.AmbientLight;
import primitives.Color;

/**
 * Scene class represents a 3D scene containing geometries,
 * background color, and ambient light.
 * This is a Passive Data Structure (PDS).
 */
public class Scene {
    /** The name of the scene */
    public final String name;
    /** The background color of the scene, initialized to Black */
    public Color background = Color.BLACK;
    /** The ambient light of the scene, initialized to NONE */
    public AmbientLight ambientLight = AmbientLight.NONE;
    /** The collection of geometries in the scene */
    public Geometries geometries = new Geometries();

    /**
     * Constructor that receives the scene name.
     * @param name the name of the scene
     */
    public Scene(String name) {
        this.name = name;
    }

    /**
     * Setter for background color - Method Chaining.
     * @param background the background color
     * @return the scene object itself
     */
    public Scene setBackground(Color background) {
        this.background = background;
        return this;
    }

    /**
     * Setter for ambient light - Method Chaining.
     * @param ambientLight the ambient light
     * @return the scene object itself
     */
    public Scene setAmbientLight(AmbientLight ambientLight) {
        this.ambientLight = ambientLight;
        return this;
    }

    /**
     * Setter for geometries - Method Chaining.
     * @param geometries the geometries collection
     * @return the scene object itself
     */
    public Scene setGeometries(Geometries geometries) {
        this.geometries = geometries;
        return this;
    }
}