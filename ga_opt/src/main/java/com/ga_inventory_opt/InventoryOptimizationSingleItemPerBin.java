package com.ga_inventory_opt;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import io.jenetics.DoubleChromosome;
import io.jenetics.DoubleGene;
import io.jenetics.Genotype;
import io.jenetics.Mutator;
import io.jenetics.Optimize;
import io.jenetics.Phenotype;
import io.jenetics.UniformCrossover;
import io.jenetics.engine.Constraint;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionResult;
import io.jenetics.engine.EvolutionStatistics;
import io.jenetics.util.Factory;

public class InventoryOptimizationSingleItemPerBin {

    // ---------- Instance Variables ----------
    private int numItems;
    private int numBins;
    
    // Weight for fitness calculation (importance of value vs area)
    private double W = 0.7;

    // [width, height, price]
    private double[][] items;
    
    // [width, height]
    private double[][] bins;
    
    // Cached calculations
    private double[] itemAreas;
    private double[] binAreas;
    private double totalBinArea;
    private double totalInventoryPrice;
    
    // Evolution parameters
    private int populationSize = 120;
    private int generations = 150;
    private double mutationRate = 0.2;
    private double crossoverRate = 0.3;

    /**
     * Constructor
     * @param items 2D array where each row is [width, height, price]
     * @param bins 2D array where each row is [width, height]
     */
    public InventoryOptimizationSingleItemPerBin(double[][] items, double[][] bins) {
        this.items = items;
        this.bins = bins;
        this.numItems = items.length;
        this.numBins = bins.length;
        
        // Calculate and store item areas
        this.itemAreas = new double[numItems];
        for (int i = 0; i < numItems; i++) {
            this.itemAreas[i] = items[i][0] * items[i][1];
        }
        
        // Calculate and store bin areas and total storage area
        this.binAreas = new double[numBins];
        this.totalBinArea = 0.0;
        for (int i = 0; i < numBins; i++) {
            this.binAreas[i] = bins[i][0] * bins[i][1];
            this.totalBinArea += this.binAreas[i];
        }
        
        // Calculate total item price
        this.totalInventoryPrice = 0.0;
        for (int i = 0; i < numItems; i++) {
            this.totalInventoryPrice += items[i][2];
        }
    }
    
    // Setters for evolution parameters
    public void setW(double w) { this.W = w; }
    public void setPopulationSize(int size) { this.populationSize = size; }
    public void setGenerations(int gen) { this.generations = gen; }
    public void setMutationRate(double rate) { this.mutationRate = rate; }
    public void setCrossoverRate(double rate) { this.crossoverRate = rate; }

    // ---------- Fitness Function ----------
    private double fitness(Genotype<DoubleGene> gt) {
        DoubleChromosome chromosome = (DoubleChromosome) gt.chromosome();

        // Track items in each bin
        List<List<Integer>> itemsInBins = new ArrayList<>();
        for (int i = 0; i <= numBins; i++) {
            itemsInBins.add(new ArrayList<>());
        }
        
        // Map items to bins based on chromosome
        for (int i = 0; i < numItems; i++) {
            int binIndex = (int) Math.round(chromosome.get(i).doubleValue());
            // Ensure bin index is valid (0 to numBins)
            binIndex = Math.max(0, Math.min(numBins, binIndex));
            itemsInBins.get(binIndex).add(i);
        }
        
        double priceOfStoredProducts = 0.0;
        double areaOfStoredProducts = 0.0;
        
        // Process each bin (skip bin 0 as it means "not in storage")
        // No need to check capacity here - constraint validator ensures all chromosomes are valid
        for (int binIdx = 1; binIdx <= numBins; binIdx++) {
            List<Integer> itemsInThisBin = itemsInBins.get(binIdx);
            
            for (int itemIdx : itemsInThisBin) {
                areaOfStoredProducts += itemAreas[itemIdx];
                priceOfStoredProducts += items[itemIdx][2];
            }
        }
        
        // Normalized scores
        double valueScore = (totalInventoryPrice == 0) ? 0.0 : priceOfStoredProducts / totalInventoryPrice;
        double areaScore = (totalBinArea == 0) ? 0.0 : areaOfStoredProducts / totalBinArea;

        // Core formula
        double F = (W + 1) * valueScore + (1 - W) * areaScore;
        return F;
    }
    
    // ---------- Constraint Validator ----------
    private boolean isValid(Phenotype<DoubleGene, Double> phenotype) {
        Genotype<DoubleGene> gt = phenotype.genotype();
        DoubleChromosome chromosome = (DoubleChromosome) gt.chromosome();
        
        // Track items in each bin
        List<List<Integer>> itemsInBins = new ArrayList<>();
        for (int i = 0; i <= numBins; i++) {
            itemsInBins.add(new ArrayList<>());
        }
        
        // Map items to bins based on chromosome
        for (int i = 0; i < numItems; i++) {
            int binIndex = (int) Math.round(chromosome.get(i).doubleValue());
            binIndex = Math.max(0, Math.min(numBins, binIndex));
            itemsInBins.get(binIndex).add(i);
        }
        
        // Check constraint for each bin (skip bin 0)
        for (int binIdx = 1; binIdx <= numBins; binIdx++) {
            List<Integer> itemsInThisBin = itemsInBins.get(binIdx);
            double binItemsArea = 0.0;
            
            for (int itemIdx : itemsInThisBin) {
                binItemsArea += itemAreas[itemIdx];
            }
            
            // If items area exceeds bin capacity, chromosome is invalid
            if (binItemsArea > binAreas[binIdx - 1]) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Result class to hold optimization results
     */
    public static class OptimizationResult {
        public final int[] itemToBinMapping;  // Index is item number, value is bin number (0 = not stored)
        public final double fitness;
        public final double totalStoredPrice;
        public final double totalStoredArea;
        public final EvolutionStatistics<Double, ?> statistics;
        
        public OptimizationResult(int[] itemToBinMapping, double fitness, 
                                double totalStoredPrice, double totalStoredArea,
                                EvolutionStatistics<Double, ?> statistics) {
            this.itemToBinMapping = itemToBinMapping;
            this.fitness = fitness;
            this.totalStoredPrice = totalStoredPrice;
            this.totalStoredArea = totalStoredArea;
            this.statistics = statistics;
        }
    }
    
    /**
     * Run the genetic algorithm optimization
     * @return OptimizationResult containing the best solution and statistics
     */
    public OptimizationResult optimize() {
        // Genotype factory: chromosome with numItems genes, each ranging from 0 to numBins
        // 0 = not in a bin, 1 to numBins = bin assignment
        Factory<Genotype<DoubleGene>> genotypeFactory =
                Genotype.of(DoubleChromosome.of(0, numBins, numItems));

        Engine<DoubleGene, Double> engine = Engine.builder(this::fitness, genotypeFactory)
                .optimize(Optimize.MAXIMUM)
                .populationSize(populationSize)
                .alterers(new Mutator<>(mutationRate), new UniformCrossover<>(crossoverRate))
                .constraint(Constraint.of(this::isValid))
                .build();

        EvolutionStatistics<Double, ?> stats = EvolutionStatistics.ofNumber();

        EvolutionResult<DoubleGene, Double> result = engine.stream()
                .limit(generations)
                .peek(stats)
                .collect(EvolutionResult.toBestEvolutionResult());

        DoubleChromosome best = (DoubleChromosome) result.bestPhenotype().genotype().chromosome();
        
        // Build item to bin mapping array
        int[] itemToBinMapping = new int[numItems];
        for (int i = 0; i < numItems; i++) {
            int binIndex = (int) Math.round(best.get(i).doubleValue());
            binIndex = Math.max(0, Math.min(numBins, binIndex));
            itemToBinMapping[i] = binIndex;
        }
        
        // Calculate total stored price and area
        List<List<Integer>> itemsInBins = new ArrayList<>();
        for (int i = 0; i <= numBins; i++) {
            itemsInBins.add(new ArrayList<>());
        }
        
        for (int i = 0; i < numItems; i++) {
            itemsInBins.get(itemToBinMapping[i]).add(i);
        }
        
        double totalStoredPrice = 0.0;
        double totalStoredArea = 0.0;
        
        for (int binIdx = 1; binIdx <= numBins; binIdx++) {
            List<Integer> itemsInThisBin = itemsInBins.get(binIdx);
            double binItemsArea = 0.0;
            double binItemsPrice = 0.0;
            
            for (int itemIdx : itemsInThisBin) {
                binItemsArea += itemAreas[itemIdx];
                binItemsPrice += items[itemIdx][2];
            }
            
            if (binItemsArea <= binAreas[binIdx - 1]) {
                totalStoredArea += binItemsArea;
                totalStoredPrice += binItemsPrice;
            }
        }
        
        return new OptimizationResult(itemToBinMapping, result.bestFitness(), 
                                      totalStoredPrice, totalStoredArea, stats);
    }
    
    /**
     * Print detailed results to console
     */
    public void printResults(OptimizationResult result) {
        System.out.println(result.statistics);
        System.out.println("\n=== BIN CONFIGURATIONS ===");
        for (int i = 0; i < numBins; i++) {
            System.out.printf("Bin %d: %.2f x %.2f (Area: %.2f)%n",
                    i + 1, bins[i][0], bins[i][1], binAreas[i]);
        }
        
        System.out.println("\n=== ITEM CONFIGURATIONS ===");
        for (int i = 0; i < numItems; i++) {
            System.out.printf("Item %d: %.2f x %.2f, Price=%.2f (Area: %.2f)%n",
                    i + 1, items[i][0], items[i][1], items[i][2], itemAreas[i]);
        }
        
        System.out.println("\n=== BEST SOLUTION ===");

        // Track items in each bin
        List<List<Integer>> itemsInBins = new ArrayList<>();
        for (int i = 0; i <= numBins; i++) {
            itemsInBins.add(new ArrayList<>());
        }
        
        for (int i = 0; i < numItems; i++) {
            itemsInBins.get(result.itemToBinMapping[i]).add(i);
        }
        
        // Display items not in storage (bin 0)
        if (!itemsInBins.get(0).isEmpty()) {
            System.out.println("\nNot in Storage:");
            for (int itemIdx : itemsInBins.get(0)) {
                System.out.printf("  Item %d (%.2fx%.2f, Price=%.2f)%n",
                        itemIdx + 1, items[itemIdx][0], items[itemIdx][1], items[itemIdx][2]);
            }
        }
        
        // Display items in each bin
        for (int binIdx = 1; binIdx <= numBins; binIdx++) {
            List<Integer> itemsInThisBin = itemsInBins.get(binIdx);
            
            if (!itemsInThisBin.isEmpty()) {
                System.out.printf("%nBin %d (%.2fx%.2f, Area: %.2f):%n",
                        binIdx, bins[binIdx - 1][0], bins[binIdx - 1][1], binAreas[binIdx - 1]);
                
                double binItemsArea = 0.0;
                double binItemsPrice = 0.0;
                
                for (int itemIdx : itemsInThisBin) {
                    binItemsArea += itemAreas[itemIdx];
                    binItemsPrice += items[itemIdx][2];
                    System.out.printf("  Item %d (%.2fx%.2f, Price=%.2f, Area=%.2f)%n",
                            itemIdx + 1, items[itemIdx][0], items[itemIdx][1], 
                            items[itemIdx][2], itemAreas[itemIdx]);
                }
                
                System.out.printf("  Total in bin: Area=%.2f/%.2f, Price=%.2f%n",
                        binItemsArea, binAreas[binIdx - 1], binItemsPrice);
            }
        }

        System.out.printf("%nTotal Stored Price: %.2f / %.2f (%.2f%%)%n", 
                result.totalStoredPrice, totalInventoryPrice, 
                (result.totalStoredPrice / totalInventoryPrice) * 100);
        System.out.printf("Total Stored Area: %.2f / %.2f (%.2f%%)%n", 
                result.totalStoredArea, totalBinArea, 
                (result.totalStoredArea / totalBinArea) * 100);
        System.out.printf("Fitness: %.6f%n", result.fitness);
    }
    
    // ---------- Main (for testing) ----------
    public static void main(String[] args) {
        Random random = new Random(42);
        
        // Generate random items for testing
        int numItems = 8;
        double[][] items = new double[numItems][3];
        for (int i = 0; i < numItems; i++) {
            items[i][0] = 2.0 + random.nextDouble() * 8.0; // width
            items[i][1] = 2.0 + random.nextDouble() * 8.0; // height
            items[i][2] = 10.0 + random.nextDouble() * 40.0; // price
        }
        
        // Generate random bins for testing
        int numBins = 4;
        double[][] bins = new double[numBins][2];
        for (int i = 0; i < numBins; i++) {
            bins[i][0] = 2.0 + random.nextDouble() * 38.0; // width: 2-40
            bins[i][1] = 2.0 + random.nextDouble() * 48.0; // height: 2-50
        }
        
        // Create optimizer instance
        InventoryOptimizationSingleItemPerBin optimizer = 
            new InventoryOptimizationSingleItemPerBin(items, bins);
        
        // Run optimization
        OptimizationResult result = optimizer.optimize();
        
        // Print results
        optimizer.printResults(result);
    }
}
