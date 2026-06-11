package lighting;

import primitives.Color;
import primitives.Point;
import primitives.Vector;

/**
 * Interface for all light sources in the scene.
 *
 * <p>Provides methods to calculate intensity, direction, distance, and physical
 * size at a specific point. The {@link #getSize()} method enables Soft Shadows:
 * a size of 0 means the light is a point source (hard shadows only), while a
 * positive size defines the radius of the area light used for shadow sampling.</p>
 */
public interface LightSource {

    /**
     * Calculates the intensity of the light at a given point.
     * Takes into account attenuation based on distance.
     *
     * @param p the point in the scene
     * @return the color intensity at point {@code p}
     */
    Color getIntensity(Point p);

    /**
     * Returns the normalized direction vector from the light source to a point.
     *
     * @param p the point in the scene
     * @return normalized direction vector from the light to {@code p}
     */
    Vector getL(Point p);

    /**
     * Returns the distance between the light source and a given point.
     * Used for shadow ray length clamping and attenuation calculations.
     *
     * @param point the point to measure distance to
     * @return distance from the light source to {@code point}
     */
    double getDistance(Point point);

    /**
     * Returns the physical size (radius) of this light source's area.
     *
     * <p>Used by the Soft Shadows feature to define the sampling target area
     * around the light's position:</p>
     * <ul>
     *   <li>0 – point light (no soft shadows, hard shadow boundary)</li>
     *   <li>&gt;0 – area light; larger values produce wider, softer shadow penumbras</li>
     * </ul>
     *
     * <p>Note: {@link DirectionalLight} always returns 0 because it has no
     * physical position and therefore cannot produce soft shadows.</p>
     *
     * @return the radius of the light source area (0 = point/directional light)
     */
    double getSize();
}