package geometries.impl;

import geometries.api.AABB;
import geometries.api.Intersectable;
import primitives.Ray;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Composite class representing a collection of intersectable geometries.
 * This class uses the Composite Design Pattern.
 *
 * <p>Since Mini-Project 2, this class can optionally accelerate its own
 * intersection tests using a Bounding Volume Hierarchy (BVH) built from its
 * children. The BVH is entirely an implementation detail of this class:
 * nothing above it (Scene, RayTracerBase, SimpleRayTracer, Camera) needs to
 * know it exists. A test enables it by calling {@link #buildBVH()} after all
 * geometries have been added, and disables it by calling
 * {@link #disableBVH()} — both with no other code changes required.</p>
 */
public class Geometries extends Intersectable {

    /** List to store all geometries in the collection */
    private final List<Intersectable> _geometries = new ArrayList<>();

    // ========================= BVH Acceleration (Mini-Project 2) =========================

    /**
     * Whether intersection tests should be routed through the BVH
     * ({@code true}) or through the original brute-force linear scan
     * ({@code false}, the default). Toggled exclusively by {@link #buildBVH()}
     * / {@link #disableBVH()} — never hard-coded.
     */
    private boolean _useBVH = false;

    /**
     * Root of the BVH tree built from this collection's bounded children
     * (everything whose {@link Intersectable#getBoundingBox()} is non-null).
     * Null if BVH is disabled, or if there were no bounded children to build from.
     */
    private Intersectable _bvhRoot;

    /**
     * Children that cannot be placed in the BVH because they are unbounded
     * (e.g. an infinite {@code Plane}). These must always be tested directly,
     * regardless of where the ray points, since no bounding box can ever
     * reject them.
     */
    private List<Intersectable> _unboundedChildren;

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
     * <p>Note: adding geometries after calling {@link #buildBVH()} will NOT
     * automatically rebuild the tree — call {@link #buildBVH()} again after
     * all additions are done, exactly once, right before rendering.</p>
     *
     * @param geometries objects to add
     */
    public void add(Intersectable... geometries) {
        if (geometries != null) {
            Collections.addAll(_geometries, geometries);
        }
    }

    // ========================= BVH Activation API =========================

    /**
     * Builds (or rebuilds) a BVH acceleration structure from this
     * collection's current children, using the default leaf size
     * ({@link BVHNode#DEFAULT_MAX_LEAF_SIZE}), and switches intersection
     * testing to use it.
     *
     * <p>Call this once, from test code, after all geometries have been
     * added to the scene and before rendering. No other class needs to
     * change: {@code SimpleRayTracer} keeps calling
     * {@code scene.geometries.calcIntersections(...)} exactly as before.</p>
     *
     * @return this collection (for method chaining)
     */
    public Geometries buildBVH() {
        return buildBVH(BVHNode.DEFAULT_MAX_LEAF_SIZE);
    }

    /**
     * Builds (or rebuilds) a BVH acceleration structure from this
     * collection's current children, with an explicit leaf-size parameter,
     * and switches intersection testing to use it.
     *
     * <p>Children with no bounding box (e.g. an infinite {@code Plane}) are
     * kept out of the tree and tested directly on every ray, since no box
     * could ever safely reject them.</p>
     *
     * @param maxLeafSize maximum number of primitives stored directly in a
     *                    BVH leaf before splitting further (must be &gt;= 1)
     * @return this collection (for method chaining)
     */
    public Geometries buildBVH(int maxLeafSize) {
        List<Intersectable> bounded = new ArrayList<>();
        List<Intersectable> unbounded = new ArrayList<>();

        for (Intersectable geo : _geometries) {
            if (geo.getBoundingBox() == null) unbounded.add(geo);
            else bounded.add(geo);
        }

        _bvhRoot = bounded.isEmpty() ? null : BVHNode.build(bounded, maxLeafSize);
        _unboundedChildren = unbounded;
        _useBVH = true;
        return this;
    }

    /**
     * Disables the BVH and reverts to brute-force linear intersection
     * testing against every child. The previously built tree (if any) is
     * discarded; calling {@link #buildBVH()} again later rebuilds it.
     *
     * @return this collection (for method chaining)
     */
    public Geometries disableBVH() {
        _useBVH = false;
        _bvhRoot = null;
        _unboundedChildren = null;
        return this;
    }

    /**
     * Returns whether BVH acceleration is currently active for this collection.
     * Exposed mainly so tests/measurements can confirm and log which mode
     * a given render actually ran in.
     *
     * @return true if BVH is active
     */
    public boolean isBVHEnabled() {
        return _useBVH;
    }

    // ========================= Bounding Box =========================

    /**
     * Returns the union bounding box of all children, or {@code null} if
     * this collection is empty or contains at least one unbounded child
     * (e.g. an infinite {@code Plane}) — in either case the group as a
     * whole cannot be safely bounded, so it must be propagated as
     * "unbounded" to any BVH being built one level up.
     *
     * @return the union bounding box, or null if unbounded/empty
     */
    @Override
    public AABB getBoundingBox() {
        AABB result = null;
        for (Intersectable geo : _geometries) {
            AABB box = geo.getBoundingBox();
            if (box == null) return null; // an unbounded child makes the whole group unbounded
            result = AABB.union(result, box);
        }
        return result;
    }

    // ========================= Intersection Testing =========================

    /**
     * Helper method for calculating intersections using the NVI pattern.
     * Routes to the BVH-accelerated path or the original brute-force path
     * depending on {@link #_useBVH} — set exclusively via {@link #buildBVH()}
     * / {@link #disableBVH()}.
     *
     * @param ray the ray to intersect with
     * @return list of intersection objects or null if none found
     */
    @Override
    protected List<Intersection> calcIntersectionsHelper(Ray ray, double maxDistance) {
        return _useBVH ? calcIntersectionsBVH(ray, maxDistance) : calcIntersectionsLinear(ray, maxDistance);
    }

    /**
     * Original brute-force intersection test: every child is tested against
     * the ray directly, regardless of where it points. This is the baseline
     * behavior used whenever BVH is disabled (the default), and is exactly
     * the Mini-Project 1 implementation — unchanged.
     *
     * @param ray the ray to intersect with
     * @return list of intersection objects or null if none found
     */
    private List<Intersection> calcIntersectionsLinear(Ray ray, double maxDistance) {
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

    /**
     * BVH-accelerated intersection test: bounded children are tested through
     * the {@link #_bvhRoot} tree (which internally skips whole branches the
     * ray cannot hit), while unbounded children (which cannot be placed in
     * any box) are still tested directly, exactly as in the linear path.
     *
     * @param ray the ray to intersect with
     * @return list of intersection objects or null if none found
     */
    private List<Intersection> calcIntersectionsBVH(Ray ray, double maxDistance) {
        List<Intersection> result = null;

        if (_bvhRoot != null) {
            List<Intersection> treeResult = _bvhRoot.calcIntersections(ray, maxDistance);
            if (treeResult != null) result = new ArrayList<>(treeResult);
        }

        for (Intersectable geo : _unboundedChildren) {
            List<Intersection> geoIntersections = geo.calcIntersections(ray, maxDistance);
            if (geoIntersections != null) {
                if (result == null) result = new ArrayList<>(geoIntersections);
                else result.addAll(geoIntersections);
            }
        }

        return result;
    }
}