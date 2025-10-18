import io.jenetics.*;
import io.jenetics.engine.*;
import io.jenetics.util.*;
import java.util.*;

public class InventoryOptimizationSingleItemPerBin {

    // ---------- Parameters ----------
    private static final int NUM_ITEMS = 8;
    private static final int NUM_BINS = 8;

    private static final double BIN_WIDTH = 10.0;
    private static final double BIN_HEIGHT = 10.0;

    // [width, height, price]
    private static final double[][] items = new double[NUM_ITEMS][3];

    static {
        Random random = new Random(42);
        for (int i = 0; i < NUM_ITEMS; i++) {
            items[i][0] = 2.0 + random.nextDouble() * 8.0; // width
            items[i][1] = 2.0 + random.nextDouble() * 8.0; // height
            items[i][2] = 10.0 + random.nextDouble() * 40.0; // price
        }
    }

    // ---------- Fitness Function ----------
    private static double fitness(Genotype<DoubleGene> gt) {
        DoubleChromosome chromosome = (DoubleChromosome) gt.chromosome();
        boolean[] binUsed = new boolean[NUM_BINS];
        double totalCost = 0.0;
        double penalty = 0.0;

        for (int i = 0; i < NUM_ITEMS; i++) {
            int binIndex = (int) Math.round(chromosome.get(i).doubleValue()) % NUM_BINS;
            double itemW = items[i][0];
            double itemH = items[i][1];
            double price = items[i][2];

            // If bin already occupied, penalize heavily
            if (binUsed[binIndex]) {
                penalty += 200.0;
                continue;
            }

            binUsed[binIndex] = true;

            // If item doesn't fit in bin, penalize
            if (itemW > BIN_WIDTH || itemH > BIN_HEIGHT) {
                penalty += 100.0;
            } else {
                totalCost += price;
            }
        }

        // Fitness: lower cost & fewer penalties = higher fitness
        double totalPenalty = totalCost + penalty;
        return 1.0 / (1.0 + totalPenalty);
    }

    // ---------- Main ----------
    public static void main(String[] args) {
        Factory<Genotype<DoubleGene>> genotypeFactory =
                Genotype.of(DoubleChromosome.of(0, NUM_BINS - 1, NUM_ITEMS));

        Engine<DoubleGene, Double> engine = Engine.builder(InventoryOptimizationSingleItemPerBin::fitness, genotypeFactory)
                .optimize(Optimize.MAXIMUM)
                .populationSize(120)
                .alterers(new Mutator<>(0.2), new UniformCrossover<>(0.3))
                .build();

        EvolutionStatistics<Double, ?> stats = EvolutionStatistics.ofNumber();

        EvolutionResult<DoubleGene, Double> result = engine.stream()
                .limit(150)
                .peek(stats)
                .collect(EvolutionResult.toBestEvolutionResult());

        System.out.println(stats);
        System.out.println("\n=== BEST SOLUTION ===");

        DoubleChromosome best = (DoubleChromosome) result.bestPhenotype().genotype().chromosome();
        boolean[] usedBins = new boolean[NUM_BINS];
        double totalCost = 0.0;

        for (int i = 0; i < NUM_ITEMS; i++) {
            int bin = (int) Math.round(best.get(i).doubleValue()) % NUM_BINS;
            double w = items[i][0], h = items[i][1], price = items[i][2];

            System.out.printf("Item %d (%.1fx%.1f, Price=%.1f) → Bin %d%n",
                    i + 1, w, h, price, bin);

            if (!usedBins[bin] && w <= BIN_WIDTH && h <= BIN_HEIGHT) {
                usedBins[bin] = true;
                totalCost += price;
            }
        }

        System.out.printf("%nTotal Cost (Valid Items Only): %.2f%n", totalCost);
        System.out.printf("Fitness: %.6f%n", result.bestFitness());
    }
}
