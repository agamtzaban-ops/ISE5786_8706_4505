# Mini-Project 2 — Performance Measurements

## Scene: Asteroid Belt Observatory
- Spheres: 407 (1 core + 6 planets + 400 asteroids)
- Triangles: 150 (crystal shards)
- Cylinders: 6 (pillars)
- Planes: 1 (backdrop, intentionally unbounded)
- Light sources: 5 (1 ambient, 1 directional, 2 point, 1 spot)
- Effects active: shadows, soft shadows, reflection, transparency, anti-aliasing

## Results

| Configuration                  | BVH | Multithreading | Render time | Speedup vs baseline |
|---------------------------------|-----|-----------------|-------------|----------------------|
| 1. Baseline                     | OFF | OFF             |  ___ ms     | x1.00                |
| 2. Multithreading only          | OFF | ON              |  ___ ms     | x____                |
| 3. BVH only                     | ON  | OFF             |  ___ ms     | x____                |
| 4. BVH + Multithreading         | ON  | ON              |  ___ ms     | x____                |

## Notes
- Multithreading speedup measured independently of the BVH speedup (rows 1→2 isolate threading; rows 1→3 isolate BVH).
- Machine: ___ (CPU, core count)