### Recorded environment

- UTC run time: 2026-10-21T12:53:56.562565Z
- Java: 25.0.1+8-27 / OpenJDK 64-Bit Server VM
- OS: Windows 11 10.0 / amd64
- CPU: Intel64 Family 6 Model 154 Stepping 3, GenuineIntel
- Available processors: 12
- Base seed: 20261021
- Warm-ups per case: 20
- Measured repetitions per case: 5

### Execution time

Average, minimum and maximum refer to the five measured runs. Unit: microseconds.

| Algorithm | V | E | Average | Minimum | Maximum |
|---|---:|---:|---:|---:|---:|
| Kruskal | 10 | 30 | 40.620 | 16.700 | 66.700 |
| Prim | 10 | 30 | 42.640 | 36.800 | 50.400 |
| SCC DAG | 10 | 30 | 58.440 | 55.600 | 64.900 |
| SCC cyclic | 10 | 30 | 31.280 | 20.900 | 52.400 |
| Topological DAG | 10 | 30 | 24.220 | 18.300 | 36.800 |
| Topological cyclic | 10 | 30 | 5.040 | 3.000 | 11.600 |
| Dijkstra | 10 | 30 | 12.520 | 11.300 | 13.400 |
| Kruskal | 100 | 500 | 158.480 | 126.600 | 222.900 |
| Prim | 100 | 500 | 233.040 | 229.800 | 238.400 |
| SCC DAG | 100 | 500 | 153.280 | 131.400 | 180.200 |
| SCC cyclic | 100 | 500 | 120.420 | 113.000 | 124.900 |
| Topological DAG | 100 | 500 | 69.720 | 66.400 | 75.600 |
| Topological cyclic | 100 | 500 | 35.900 | 21.200 | 81.300 |
| Dijkstra | 100 | 500 | 213.320 | 192.400 | 244.300 |
| Kruskal | 1000 | 5000 | 1293.900 | 1145.300 | 1686.700 |
| Prim | 1000 | 5000 | 1048.740 | 904.000 | 1162.600 |
| SCC DAG | 1000 | 5000 | 390.600 | 274.000 | 471.000 |
| SCC cyclic | 1000 | 5000 | 308.600 | 293.300 | 338.500 |
| Topological DAG | 1000 | 5000 | 142.500 | 136.900 | 149.900 |
| Topological cyclic | 1000 | 5000 | 41.600 | 37.600 | 43.500 |
| Dijkstra | 1000 | 5000 | 708.540 | 453.200 | 866.300 |

### Edge and queue operations

Counters are identical in all five repeats; the per-run values below therefore also equal their averages.

| Algorithm | V | Edges examined | Comparator calls | Queue adds | Queue removes | Relaxations | Vertex visits |
|---|---:|---:|---:|---:|---:|---:|---:|
| Kruskal | 10 | 10 | 104 | 0 | 0 | 0 | 0 |
| Prim | 10 | 60 | 198 | 30 | 30 | 0 | 10 |
| SCC DAG | 10 | 90 | 0 | 0 | 0 | 0 | 20 |
| SCC cyclic | 10 | 90 | 0 | 0 | 0 | 0 | 20 |
| Topological DAG | 10 | 60 | 0 | 10 | 10 | 0 | 10 |
| Topological cyclic | 10 | 30 | 0 | 0 | 0 | 0 | 0 |
| Dijkstra | 10 | 60 | 109 | 21 | 21 | 20 | 10 |
| Kruskal | 100 | 249 | 3836 | 0 | 0 | 0 | 0 |
| Prim | 100 | 1000 | 7610 | 500 | 500 | 0 | 100 |
| SCC DAG | 100 | 1500 | 0 | 0 | 0 | 0 | 200 |
| SCC cyclic | 100 | 1500 | 0 | 0 | 0 | 0 | 200 |
| Topological DAG | 100 | 1000 | 0 | 100 | 100 | 0 | 100 |
| Topological cyclic | 100 | 500 | 0 | 0 | 0 | 0 | 0 |
| Dijkstra | 100 | 1000 | 2154 | 186 | 186 | 185 | 100 |
| Kruskal | 1000 | 3410 | 55129 | 0 | 0 | 0 | 0 |
| Prim | 1000 | 10000 | 111737 | 5000 | 5000 | 0 | 1000 |
| SCC DAG | 1000 | 15000 | 0 | 0 | 0 | 0 | 2000 |
| SCC cyclic | 1000 | 15000 | 0 | 0 | 0 | 0 | 2000 |
| Topological DAG | 1000 | 10000 | 0 | 1000 | 1000 | 0 | 1000 |
| Topological cyclic | 1000 | 5000 | 0 | 0 | 0 | 0 | 0 |
| Dijkstra | 1000 | 10000 | 35812 | 1941 | 1941 | 1940 | 1000 |

### MST agreement and DSU operations

| Algorithm | V | Forest/MST weight | Find calls | Union calls | Parent traversals |
|---|---:|---:|---:|---:|---:|
| Kruskal | 10 | 102 | 20 | 10 | 18 |
| Prim | 10 | 102 | 0 | 0 | 0 |
| Kruskal | 100 | 1314 | 498 | 249 | 844 |
| Prim | 100 | 1314 | 0 | 0 | 0 |
| Kruskal | 1000 | 12330 | 6820 | 3410 | 12492 |
| Prim | 1000 | 12330 | 0 | 0 | 0 |

Prim does not use a DSU, so its DSU counters are zero.
