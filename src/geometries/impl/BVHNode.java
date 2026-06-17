package geometries.impl;

import geometries.api.AABB;
import geometries.api.Intersectable;
import geometries.api.Intersectable.Intersection;
import primitives.Ray;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * BVHNode - a single node in a Bounding Volume Hierarchy acceleration tree.
 *
 * <p>Every node caches the union bounding box of everything beneath it, so a
 * ray that misses the box can skip the entire sub-tree with a single cheap
 * test ({@link AABB#intersects}), instead of testing every geometry inside
 * it individually.</p>
 *
 * <p>A node is one of two kinds:</p>
 * <ul>
 *   <li><b>Leaf</b> — holds a small list of geometries directly, tested with
 *       brute-force linear search once the ray is known to hit the leaf's box.</li>
 *   <li><b>Internal</b> — holds exactly two children ({@code left}, {@code right}),
 *       each itself an arbitrary {@link Intersectable} (typically another
 *       {@code BVHNode}, but it can be any geometry or {@code Geometries} group —
 *       this is what makes manual hierarchy construction possible).</li>
 * </ul>
 *
 * <p>Because {@code BVHNode} itself implements {@code Intersectable}, it is
 * indistinguishable from the outside from any other geometry or composite:
 * {@code Geometries} (and everything above it in the pipeline) calls
 * {@code calcIntersections(ray, maxDistance)} exactly the same way regardless
 * of whether BVH is active.</p>
 */
public class BVHNode extends Intersectable {

    /** Default maximum number of primitives stored directly in a leaf before splitting further. */
    public static final int DEFAULT_MAX_LEAF_SIZE = 2;

    /** Bounding box of everything under this node. Computed once at construction time. */
    private final AABB _box;

    /** Left child (null for a leaf node). */
    private final Intersectable _left;

    /** Right child (null for a leaf node). */
    private final Intersectable _right;

    /** Geometries stored directly at this node (null for an internal node). */
    private final List<Intersectable> _leafItems;

    // ========================= Manual Hierarchy =========================

    /**
     * MANUAL hierarchy constructor — builds an internal node from two
     * explicitly given children.
     *
     * <p>This lets a test (or a developer) decide the tree shape by hand,
     * e.g. to verify that the ray-vs-box culling and the recursive
     * intersection logic behave correctly on a small, controlled example,
     * before relying on the automatic builder for a real scene.</p>
     *
     * @param left  the left sub-tree (any Intersectable: a single geometry,
     *              a {@code Geometries} group, or another {@code BVHNode})
     * @param right the right sub-tree
     */
    public BVHNode(Intersectable left, Intersectable right) {
        _left = left;
        _right = right;
        _leafItems = null;
        _box = AABB.union(left.getBoundingBox(), right.getBoundingBox());
    }

    /**
     * Private LEAF constructor — stores a small list of geometries directly,
     * to be tested by brute force once the ray is known to hit {@code box}.
     *
     * @param items the geometries stored at this leaf
     * @param box   the precomputed union bounding box of {@code items}
     */
    private BVHNode(List<Intersectable> items, AABB box) {
        _leafItems = items;
        _left = null;
        _right = null;
        _box = box;
    }

    // ========================= Automatic Hierarchy =========================

    /**
     * Builds a BVH tree automatically from a flat list of bounded geometries,
     * using the default leaf size ({@value #DEFAULT_MAX_LEAF_SIZE}).
     *
     * <p>All items passed in MUST have a non-null bounding box (see
     * {@link Intersectable#getBoundingBox()}) — unbounded geometries
     * (e.g. an infinite Plane) cannot be placed in a BVH and must be handled
     * separately by the caller (see {@code Geometries.buildBVH()}).</p>
     *
     * @param items the bounded geometries to organize into a hierarchy
     * @return the root of the built tree, or {@code null} if {@code items} is empty
     */
    public static Intersectable build(List<Intersectable> items) {
        return build(items, DEFAULT_MAX_LEAF_SIZE);
    }

    /**
     * Builds a BVH tree automatically from a flat list of bounded geometries.
     *
     * <p>Algorithm (Median Split):</p>
     * <ol>
     *   <li>If the list is small enough ({@code <= maxLeafSize}), stop and
     *       create a leaf holding all of it.</li>
     *   <li>Otherwise, compute the union bounding box of the whole list and
     *       find its longest axis — splitting along the longest axis tends
     *       to produce more balanced, tighter sub-boxes than a fixed axis.</li>
     *   <li>Sort the items by their bounding-box centroid along that axis,
     *       and split the sorted list in half.</li>
     *   <li>Recurse on each half and combine the two resulting sub-trees
     *       into a new internal node.</li>
     * </ol>
     *
     * @param items       the bounded geometries to organize into a hierarchy
     * @param maxLeafSize maximum number of items stored directly in a leaf
     *                    (must be &gt;= 1) — exposed as a parameter rather
     *                    than a hard-coded constant, so tests can tune the
     *                    tree depth/branching trade-off
     * @return the root of the built tree, or {@code null} if {@code items} is empty
     * @throws IllegalArgumentException if {@code maxLeafSize < 1}
     */
    public static Intersectable build(List<Intersectable> items, int maxLeafSize) {
        if (maxLeafSize < 1)
            throw new IllegalArgumentException("maxLeafSize must be >= 1");
        if (items.isEmpty())
            return null;

        AABB box = unionOfAll(items);

        if (items.size() <= maxLeafSize)
            return new BVHNode(new ArrayList<>(items), box);

        int axis = box.longestAxis();
        List<Intersectable> sorted = new ArrayList<>(items);
        sorted.sort(Comparator.comparingDouble(it -> it.getBoundingBox().centroid(axis)));

        int mid = sorted.size() / 2;
        Intersectable left = build(sorted.subList(0, mid), maxLeafSize);
        Intersectable right = build(sorted.subList(mid, sorted.size()), maxLeafSize);

        return new BVHNode(left, right);
    }

    /**
     * Computes the union bounding box of a list of (already bounded) items.
     *
     * @param items the items to union
     * @return the union box
     */
    private static AABB unionOfAll(List<Intersectable> items) {
        AABB result = null;
        for (Intersectable item : items)
            result = AABB.union(result, item.getBoundingBox());
        return result;
    }

    // ========================= Intersectable API =========================

    /**
     * Returns the cached bounding box of this node (the union of everything
     * beneath it). Never null for a tree built from bounded geometries.
     *
     * @return this node's bounding box
     */
    @Override
    public AABB getBoundingBox() {
        return _box;
    }

    /**
     * Helper method for calculating intersections using the NVI pattern.
     *
     * <p>First rejects the entire sub-tree with a single box test. Only if
     * the ray actually crosses this node's box does it either test the
     * leaf's items directly, or recurse into both children (an internal
     * node cannot skip a child just because the ray hit the parent box —
     * the ray may still miss one of the two smaller child boxes).</p>
     *
     * @param ray         the ray to check
     * @param maxDistance the maximum allowed distance
     * @return list of intersections found beneath this node, or null if none
     */
    @Override
    protected List<Intersection> calcIntersectionsHelper(Ray ray, double maxDistance) {
        // Cheap early rejection: skip this whole branch if the ray misses our box.
        if (_box != null && !_box.intersects(ray, maxDistance))
            return null;

        if (_leafItems != null)
            return calcLeafIntersections(ray, maxDistance);

        // Internal node — NVI: call the public method on each child so that
        // a child which is itself a BVHNode gets its own box test too.
        List<Intersection> leftResult = _left.calcIntersections(ray, maxDistance);
        List<Intersection> rightResult = _right.calcIntersections(ray, maxDistance);

        if (leftResult == null) return rightResult;
        if (rightResult == null) return leftResult;

        // Both lists may be the immutable List.of(...) returned by leaf
        // geometries (Sphere/Triangle/...) — must copy before combining.
        List<Intersection> combined = new ArrayList<>(leftResult);
        combined.addAll(rightResult);
        return combined;
    }

    /**
     * Brute-force tests this leaf's items once the ray is already known to
     * hit the leaf's bounding box.
     */
    private List<Intersection> calcLeafIntersections(Ray ray, double maxDistance) {
        List<Intersection> result = null;
        for (Intersectable item : _leafItems) {
            List<Intersection> sub = item.calcIntersections(ray, maxDistance);
            if (sub != null) {
                if (result == null) result = new ArrayList<>(sub);
                else result.addAll(sub);
            }
        }
        return result;
    }
}