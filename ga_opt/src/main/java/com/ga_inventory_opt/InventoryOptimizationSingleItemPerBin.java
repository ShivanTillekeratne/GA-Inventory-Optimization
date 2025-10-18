package com.ga_inventory_opt;

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

        // weight W (importance of value vs area)
        final double W = 0.9;
        
        DoubleChromosome chromosome = (DoubleChromosome) gt.chromosome();

        // denominators
        double totalInventoryPrice = 0.0;
        for (int i = 0; i < NUM_ITEMS; i++) {
            totalInventoryPrice += items[i][2];
        }
        double totalBinArea = NUM_BINS * BIN_WIDTH * BIN_HEIGHT;

        // Numerators
        boolean[] binUsed = new boolean[NUM_BINS];
        double priceOfStoredProducts = 0.0;
        double areaOfStoredProducts = 0.0;

        for (int i = 0; i < NUM_ITEMS; i++) {
            int binIndex = (int) Math.round(chromosome.get(i).doubleValue()) % NUM_BINS;
            double itemW = items[i][0];
            double itemH = items[i][1];
            double price = items[i][2];

            // Check if the item is successfully stored
            if (!binUsed[binIndex] && itemW <= BIN_WIDTH && itemH <= BIN_HEIGHT) {
                priceOfStoredProducts += price;
                areaOfStoredProducts += itemW * itemH;
                binUsed[binIndex] = true;
            }
        }

        // Normalized scores
        double valueScore = (totalInventoryPrice == 0) ? 0.0 : priceOfStoredProducts / totalInventoryPrice;
        double areaScore = (totalBinArea == 0) ? 0.0 : areaOfStoredProducts / totalBinArea;

        // Core formula
        double F = (W + 1) * valueScore + (1 - W) * areaScore;
        return F;
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
