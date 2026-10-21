package experiments;

import connectivity.Connectivity;
import graph.Graph;
import graph.GraphGenerator;
import graph.GraphGenerator.Type;
import graph.Metrics;
import mst.MinimumSpanningTree;
import shortestpath.Dijkstra;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Reproduces all tables and both plots using only the JDK. */
public class Experiments {
    private static final int WARMUPS = 20;
    private static final int REPEATS = 5;
    private static final int[][] SIZES = {{10,30}, {100,500}, {1000,5000}};
    private static final String[] NAMES = {"Kruskal", "Prim", "SCC DAG", "SCC cyclic",
            "Topological DAG", "Topological cyclic", "Dijkstra"};
    private record Run(Metrics metrics, long answer) { }
    private record Summary(String algorithm, int vertices, int edges, Type type, long seed,
                           double averageNs, long minNs, long maxNs, Run run) { }
    private static volatile Run consumed;

    public static void main(String[] args) throws IOException {
        Files.createDirectories(Path.of("results/tables"));
        Files.createDirectories(Path.of("results/plots"));
        List<Summary> summaries = new ArrayList<>();
        StringBuilder raw = new StringBuilder("algorithm,V,E,graph,seed,repeat,nanoseconds,")
                .append(Metrics.header()).append(",answer\n");
        for (int[] size : SIZES) {
            Graph[] graphs = new Graph[Type.values().length];
            for (Type type : Type.values()) {
                graphs[type.ordinal()] = GraphGenerator.generate(size[0], size[1],
                        GraphGenerator.seed(size[0], size[1], type), type);
            }
            Graph undirected = graphs[Type.UNDIRECTED.ordinal()];
            var kruskal = MinimumSpanningTree.kruskal(undirected);
            var prim = MinimumSpanningTree.prim(undirected);
            if (!kruskal.connected() || !prim.connected() || kruskal.weight() != prim.weight()) {
                throw new IllegalStateException("Generated MST results disagree");
            }
            for (String name : NAMES) {
                Type type = name.endsWith("DAG") ? Type.DAG
                        : name.endsWith("cyclic") ? Type.CYCLIC : Type.UNDIRECTED;
                summaries.add(measure(name, graphs[type.ordinal()], type, raw));
            }
        }
        String environment = "UTC run time: " + Instant.now() + "\nJava: "
                + System.getProperty("java.runtime.version") + " / " + System.getProperty("java.vm.name")
                + "\nOS: " + System.getProperty("os.name") + " " + System.getProperty("os.version")
                + " / " + System.getProperty("os.arch") + "\nCPU: " + System.getenv("PROCESSOR_IDENTIFIER")
                + "\nAvailable processors: " + Runtime.getRuntime().availableProcessors()
                + "\nBase seed: " + GraphGenerator.BASE_SEED + "\nWarm-ups per case: " + WARMUPS
                + "\nMeasured repetitions per case: " + REPEATS + "\n";
        Files.writeString(Path.of("results/tables/raw-runs.csv"), raw);
        StringBuilder csv = new StringBuilder("algorithm,V,E,graph,seed,average_ns,min_ns,max_ns,")
                .append(Metrics.header()).append(",answer\n");
        for (Summary s : summaries) {
            csv.append(String.format(Locale.ROOT, "%s,%d,%d,%s,%d,%.1f,%d,%d,%s,%d%n",
                    s.algorithm(), s.vertices(), s.edges(), s.type(), s.seed(), s.averageNs(),
                    s.minNs(), s.maxNs(), s.run().metrics().csv(), s.run().answer()));
        }
        Files.writeString(Path.of("results/tables/summary.csv"), csv);
        Files.writeString(Path.of("results/tables/environment.txt"), environment);
        String tables = report(summaries, environment);
        Files.writeString(Path.of("results/tables/summary.md"), tables);
        plot(summaries, true, Path.of("results/plots/time-vs-vertices.png"));
        plot(summaries, false, Path.of("results/plots/time-vs-edges.png"));
        updateReadme(tables);
        System.out.println("Completed 21 cases, 105 measured runs and 420 warm-up runs.");
        System.out.println("MST weights, result checksums and operation counts verified.");
        System.out.println("Saved raw runs, averages, operation counts, environment and two PNG plots.");
    }

    private static Summary measure(String name, Graph graph, Type type, StringBuilder raw) {
        long seed = GraphGenerator.seed(graph.vertices(), graph.edgeCount(), type);
        Run expected = execute(name, graph);
        if ((name.equals("Topological DAG") && expected.answer() != graph.vertices())
                || (name.equals("Topological cyclic") && expected.answer() != -1)
                || (name.equals("SCC DAG") && expected.answer() != graph.vertices())
                || (name.equals("SCC cyclic") && expected.answer() != 1)) {
            throw new IllegalStateException("Incorrect directed result");
        }
        for (int i = 0; i < WARMUPS; i++) consumed = execute(name, graph);
        long total = 0;
        long minimum = Long.MAX_VALUE;
        long maximum = 0;
        Run last = null;
        for (int repeat = 1; repeat <= REPEATS; repeat++) {
            long start = System.nanoTime();
            Run result = execute(name, graph);
            long elapsed = System.nanoTime() - start;
            consumed = result;
            // Validation, formatting and disk writes stay outside the timed interval.
            if (result.answer() != expected.answer()
                    || !result.metrics().csv().equals(expected.metrics().csv())) {
                throw new IllegalStateException("Non-reproducible result or operation count");
            }
            total += elapsed;
            minimum = Math.min(minimum, elapsed);
            maximum = Math.max(maximum, elapsed);
            raw.append(name).append(',').append(graph.vertices()).append(',')
                    .append(graph.edgeCount()).append(',').append(type).append(',').append(seed)
                    .append(',').append(repeat).append(',').append(elapsed).append(',')
                    .append(result.metrics().csv()).append(',').append(result.answer()).append('\n');
            last = result;
        }
        double average = total / (double) REPEATS;
        System.out.printf(Locale.ROOT, "%-19s V=%4d E=%4d average=%9.3f us%n",
                name, graph.vertices(), graph.edgeCount(), average / 1000.0);
        return new Summary(name, graph.vertices(), graph.edgeCount(), type, seed,
                average, minimum, maximum, last);
    }

    private static Run execute(String name, Graph graph) {
        return switch (name) {
            case "Kruskal" -> {
                var r = MinimumSpanningTree.kruskal(graph);
                yield new Run(r.metrics(), r.weight());
            }
            case "Prim" -> {
                var r = MinimumSpanningTree.prim(graph);
                yield new Run(r.metrics(), r.weight());
            }
            case "SCC DAG", "SCC cyclic" -> {
                var r = Connectivity.stronglyConnectedComponents(graph);
                yield new Run(r.metrics(), r.components().size());
            }
            case "Topological DAG", "Topological cyclic" -> {
                var r = Connectivity.topologicalSort(graph);
                yield new Run(r.metrics(), r.dag() ? r.order().size() : -1);
            }
            case "Dijkstra" -> {
                var r = Dijkstra.shortestPaths(graph, 0);
                yield new Run(r.metrics(), r.distanceTo(graph.vertices() - 1));
            }
            default -> throw new IllegalArgumentException("Unknown algorithm: " + name);
        };
    }

    private static String report(List<Summary> summaries, String environment) {
        StringBuilder text = new StringBuilder("### Recorded environment\n\n");
        for (String line : environment.split("\n")) text.append("- ").append(line).append('\n');
        text.append("\n### Execution time\n\n")
                .append("Average, minimum and maximum refer to the five measured runs. Unit: microseconds.\n\n")
                .append("| Algorithm | V | E | Average | Minimum | Maximum |\n")
                .append("|---|---:|---:|---:|---:|---:|\n");
        for (Summary s : summaries) {
            text.append(String.format(Locale.ROOT, "| %s | %d | %d | %.3f | %.3f | %.3f |%n",
                    s.algorithm(), s.vertices(), s.edges(), s.averageNs()/1000.0,
                    s.minNs()/1000.0, s.maxNs()/1000.0));
        }
        text.append("\n### Edge and queue operations\n\n")
                .append("Counters are identical in all five repeats; the per-run values below therefore also equal their averages.\n\n")
                .append("| Algorithm | V | Edges examined | Comparator calls | Queue adds | Queue removes | Relaxations | Vertex visits |\n")
                .append("|---|---:|---:|---:|---:|---:|---:|---:|\n");
        for (Summary s : summaries) {
            Metrics m = s.run().metrics();
            text.append(String.format(Locale.ROOT, "| %s | %d | %d | %d | %d | %d | %d | %d |%n",
                    s.algorithm(), s.vertices(), m.edgeExaminations, m.comparisons,
                    m.queueAdds, m.queueRemoves, m.relaxations, m.verticesProcessed));
        }
        text.append("\n### MST agreement and DSU operations\n\n")
                .append("| Algorithm | V | Forest/MST weight | Find calls | Union calls | Parent traversals |\n")
                .append("|---|---:|---:|---:|---:|---:|\n");
        for (Summary s : summaries) {
            if (!s.algorithm().equals("Kruskal") && !s.algorithm().equals("Prim")) continue;
            Metrics m = s.run().metrics();
            text.append(String.format(Locale.ROOT, "| %s | %d | %d | %d | %d | %d |%n",
                    s.algorithm(), s.vertices(), s.run().answer(), m.findCalls, m.unionCalls, m.parentTraversals));
        }
        text.append("\nPrim does not use a DSU, so its DSU counters are zero.\n");
        return text.toString();
    }

    /** Replace only the generated results section, preserving the written analysis. */
    private static void updateReadme(String tables) throws IOException {
        Path path = Path.of("README.md");
        if (!Files.exists(path)) return;
        String readme = Files.readString(path);
        String begin = "<!-- RESULTS START -->";
        String end = "<!-- RESULTS END -->";
        int a = readme.indexOf(begin);
        int b = readme.indexOf(end);
        if (a < 0 || b < a) throw new IOException("README results markers are missing");
        Files.writeString(path, readme.substring(0, a + begin.length()) + "\n\n"
                + tables + "\n" + readme.substring(b));
    }

    /** A small Java2D line chart; plotting is performed after every measurement. */
    private static void plot(List<Summary> summaries, boolean byVertices, Path path) throws IOException {
        int width = 1400, height = 850;
        int left = 115, right = 1020, top = 145, bottom = 715;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.setColor(new Color(30, 40, 55));
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        String axis = byVertices ? "vertices" : "edges";
        g.drawString("Execution time vs. number of " + axis, left, 48);
        g.setFont(new Font("SansSerif", Font.PLAIN, 17));
        g.drawString("Five measured runs after 20 warm-ups per case; fixed random seeds", left, 82);
        g.drawString("Time (microseconds, logarithmic scale)", left, 120);
        double minX = Math.log10(byVertices ? 10 : 30);
        double maxX = Math.log10(byVertices ? 1000 : 5000);
        double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        for (Summary s : summaries) {
            minY = Math.min(minY, Math.log10(Math.max(1, s.minNs()) / 1000.0));
            maxY = Math.max(maxY, Math.log10(Math.max(1, s.maxNs()) / 1000.0));
        }
        minY = Math.floor(minY);
        maxY = Math.max(minY + 1, Math.ceil(maxY));
        g.setStroke(new BasicStroke(1));
        for (int tick = (int) minY; tick <= (int) maxY; tick++) {
            int y = bottom - (int) ((tick - minY) / (maxY - minY) * (bottom - top));
            g.setColor(new Color(225, 229, 235));
            g.drawLine(left, y, right, y);
            g.setColor(Color.DARK_GRAY);
            g.drawString(String.format(Locale.ROOT, "%.3g", Math.pow(10, tick)), 30, y + 6);
        }
        for (int[] size : SIZES) {
            int value = byVertices ? size[0] : size[1];
            int x = left + (int) ((Math.log10(value) - minX) / (maxX - minX) * (right - left));
            g.setColor(new Color(225, 229, 235));
            g.drawLine(x, top, x, bottom);
            g.setColor(Color.DARK_GRAY);
            g.drawString(Integer.toString(value), x - 14, bottom + 29);
        }
        g.setColor(Color.DARK_GRAY);
        g.drawRect(left, top, right-left, bottom-top);
        g.drawString("Number of " + axis + " (logarithmic scale)", 390, bottom + 65);
        String[] colors = {"#0072B2", "#D55E00", "#009E73", "#CC79A7", "#E69F00", "#56B4E9", "#444444"};
        for (int i = 0; i < NAMES.length; i++) {
            g.setColor(Color.decode(colors[i]));
            g.setStroke(new BasicStroke(2.5f));
            int previousX = -1, previousY = -1;
            for (Summary s : summaries) {
                if (!s.algorithm().equals(NAMES[i])) continue;
                double value = byVertices ? s.vertices() : s.edges();
                int x = left + (int) ((Math.log10(value) - minX) / (maxX - minX) * (right-left));
                int y = bottom - (int) ((Math.log10(s.averageNs()/1000.0)-minY)/(maxY-minY)*(bottom-top));
                int low = bottom - (int) ((Math.log10(Math.max(1,s.minNs())/1000.0)-minY)/(maxY-minY)*(bottom-top));
                int high = bottom - (int) ((Math.log10(Math.max(1,s.maxNs())/1000.0)-minY)/(maxY-minY)*(bottom-top));
                if (previousX >= 0) g.drawLine(previousX, previousY, x, y);
                g.drawLine(x, low, x, high);
                g.drawLine(x-5, low, x+5, low);
                g.drawLine(x-5, high, x+5, high);
                g.fillOval(x-5, y-5, 10, 10);
                previousX = x;
                previousY = y;
            }
            int legendY = top + 20 + i * 42;
            g.drawLine(1055, legendY, 1080, legendY);
            g.drawString(NAMES[i], 1090, legendY + 6);
        }
        g.setColor(Color.DARK_GRAY);
        g.setFont(new Font("SansSerif", Font.PLAIN, 16));
        g.drawString("Markers: means. Vertical bars: minimum to maximum of the five runs.", left, 820);
        g.dispose();
        if (!ImageIO.write(image, "png", path.toFile())) throw new IOException("PNG writer unavailable");
    }
}
