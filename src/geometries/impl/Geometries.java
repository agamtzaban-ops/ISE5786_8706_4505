package geometries.impl;

import geometries.api.Intersectable;
import primitives.Ray;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Composite class representing a collection of intersectable geometries.
 * This class uses the Composite Design Pattern.
 */
public class Geometries extends Intersectable {

    /** List to store all geometries in the collection */
    private final List<Intersectable> _geometries = new ArrayList<>();

    /** * Default constructor for an empty collection
     */
    public Geometries() {}

    /**
     * Constructor receiving a variable number of geometries.
     *
     * @param geometries objects to add to the collection
     */
    public Geometries(Intersectable... geometries) {
        add(geometries);
    }

    /**
     * Adds geometries to the collection.
     * Uses Collections.addAll for efficiency.
     *
     * @param geometries objects to add
     */
    public void add(Intersectable... geometries) {
        if (geometries != null) {
            Collections.addAll(_geometries, geometries);
        }
    }

    /**
     * Helper method for calculating intersections using the NVI pattern.
     * Evaluates intersections with all internal geometries.
     * Performance optimization: Lazy initialization of the result list.
     *
     * @param ray the ray to intersect with
     * @return list of intersection objects or null if none found
     */
    @Override
    protected List<Intersection> calcIntersectionsHelper(Ray ray,double maxDistance) {
        List<Intersection> result = null;

        // Using for-each loop to iterate through all geometries
        for (Intersectable geo : _geometries) {
            // NVI Pattern: Call the public method, NOT the helper!
            //add maxDistance
            List<Intersection> geoIntersections = geo.calcIntersections(ray,maxDistance);

            if (geoIntersections != null) {
                // Lazy initialization: create the list only when the first intersection is found
                if (result == null) {
                    result = new ArrayList<>(geoIntersections);
                } else {
                    result.addAll(geoIntersections);
                }
            }
        }

        return result;
    }
}