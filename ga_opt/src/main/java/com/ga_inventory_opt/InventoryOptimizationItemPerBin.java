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
import io.jenetics.util.Factory;

public class InventoryOptimizationItemPerBin {

    // ---------- Inner Classes for Data Transfer ----------
    public static class Item {
        public final int number;
        public final double width;
        public final double height;
        public final double price;
        
        public Item(int number, double width, double height, double price) {
            this.number = number;
            this.width = width;
            this.height = height;
            this.price = price;
        }
    }
    
    public static class Bin {
        public final int number;
        public final double width;
        public final double height;
        
        public Bin(int number, double width, double height) {
            this.number = number;
            this.width = width;
            this.height = height;
        }
    }
    
    public static class OptimizationResult {
        public final List<List<Integer>> itemsInBins; // itemsInBins.get(i) = list of item numbers in bin i
        public final double fitness;
        public final double totalStoredPrice;
        public final double totalStoredArea;
        public final double valuePercentage;
        public final double areaPercentage;
        
        public OptimizationResult(List<List<Integer>> itemsInBins, double fitness, 
                                 double totalStoredPrice, double totalStoredArea,
                                 double valuePercentage, double areaPercentage) {
            this.itemsInBins = itemsInBins;
            this.fitness = fitness;
            this.totalStoredPrice = totalStoredPrice;
            this.totalStoredArea = totalStoredArea;
            this.valuePercentage = valuePercentage;
            this.areaPercentage = areaPercentage;
        }
    }
    
    // ---------- Instance Variables ----------
    private int numItems;
    private int numBins;
    private double[][] items; // [width, height, price]
    private double[][] bins;  // [width, height]
    private double[] itemAreas;
    private double[] binAreas;
    private double totalBinArea;
    private double totalInventoryPrice;
    
    // Weight for fitness calculation (importance of value vs area)
    private double W = 0.7;
    
    // GA parameters
    private int populationSize = 120;
    private int maxGenerations = 150;
    private double mutationRate = 0.2;
    private double crossoverRate = 0.3;

    // ---------- Configuration Methods ----------
    public void setWeightW(double w) {
        this.W = w;
    }
    
    public void setPopulationSize(int size) {
        this.populationSize = size;
    }
    
    public void setMaxGenerations(int generations) {
        this.maxGenerations = generations;
    }
    
    public void setMutationRate(double rate) {
        this.mutationRate = rate;
    }
    
    public void setCrossoverRate(double rate) {
        this.crossoverRate = rate;
    }
    
    // ---------- Main Optimization Method ----------
    public OptimizationResult optimize(List<Item> itemList, List<Bin> binList) {
        // Initialize data structures
        numItems = itemList.size();
        numBins = binList.size();
        
        items = new double[numItems][3];
        bins = new double[numBins][2];
        
        // Populate items array
        for (int i = 0; i < numItems; i++) {
            Item item = itemList.get(i);
            items[i][0] = item.width;
            items[i][1] = item.height;
            items[i][2] = item.price;
        }
        
        // Populate bins array
        for (int i = 0; i < numBins; i++) {
            Bin bin = binList.get(i);
            bins[i][0] = bin.width;
            bins[i][1] = bin.height;
        }
        
        // Calculate areas and totals
        itemAreas = new double[numItems];
        for (int i = 0; i < numItems; i++) {
            itemAreas[i] = items[i][0] * items[i][1];
        }
        
        binAreas = new double[numBins];
        totalBinArea = 0.0;
        for (int i = 0; i < numBins; i++) {
            binAreas[i] = bins[i][0] * bins[i][1];
            totalBinArea += binAreas[i];
        }
        
        totalInventoryPrice = 0.0;
        for (int i = 0; i < numItems; i++) {
            totalInventoryPrice += items[i][2];
        }
        
        // Run genetic algorithm
        Factory<Genotype<DoubleGene>> genotypeFactory =
                Genotype.of(DoubleChromosome.of(0, numBins, numItems));

        Engine<DoubleGene, Double> engine = Engine.builder(this::fitness, genotypeFactory)
                .optimize(Optimize.MAXIMUM)
                .populationSize(populationSize)
                .alterers(new Mutator<>(mutationRate), new UniformCrossover<>(crossoverRate))
                .constraint(Constraint.of(this::isValid))
                .build();

        EvolutionResult<DoubleGene, Double> result = engine.stream()
                .limit(maxGenerations)
                .collect(EvolutionResult.toBestEvolutionResult());

        // Extract and return results
        return extractResult(result);
    }
    
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
        double F = (W) * valueScore + (1 - W) * areaScore;
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
    
    // ---------- Result Extraction ----------
    private OptimizationResult extractResult(EvolutionResult<DoubleGene, Double> result) {
        DoubleChromosome best = (DoubleChromosome) result.bestPhenotype().genotype().chromosome();
        
        // Track items in each bin
        List<List<Integer>> itemsInBins = new ArrayList<>();
        for (int i = 0; i <= numBins; i++) {
            itemsInBins.add(new ArrayList<>());
        }
        
        for (int i = 0; i < numItems; i++) {
            int binIndex = (int) Math.round(best.get(i).doubleValue());
            binIndex = Math.max(0, Math.min(numBins, binIndex));
            itemsInBins.get(binIndex).add(i);
        }
        
        double totalStoredPrice = 0.0;
        double totalStoredArea = 0.0;
        
        // Calculate totals
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
        
        double valuePercentage = (totalInventoryPrice > 0) ? (totalStoredPrice / totalInventoryPrice) * 100 : 0.0;
        double areaPercentage = (totalBinArea > 0) ? (totalStoredArea / totalBinArea) * 100 : 0.0;
        
        return new OptimizationResult(itemsInBins, result.bestFitness(), 
                                     totalStoredPrice, totalStoredArea,
                                     valuePercentage, areaPercentage);
    }

    // ---------- Utility Method to Print Results ----------
    public void printResult(OptimizationResult result, List<Item> itemList, List<Bin> binList) {
        System.out.println("\n=== BIN CONFIGURATIONS ===");
        for (Bin bin : binList) {
            System.out.printf("Bin %d: %.2f x %.2f (Area: %.2f)%n",
                    bin.number, bin.width, bin.height, bin.width * bin.height);
        }
        
        System.out.println("\n=== ITEM CONFIGURATIONS ===");
        for (Item item : itemList) {
            System.out.printf("Item %d: %.2f x %.2f, Price=%.2f (Area: %.2f)%n",
                    item.number, item.width, item.height, item.price, item.width * item.height);
        }
        
        System.out.println("\n=== BEST SOLUTION ===");
        
        // Display items not in storage (bin 0)
        if (!result.itemsInBins.get(0).isEmpty()) {
            System.out.println("\nNot in Storage:");
            for (int itemIdx : result.itemsInBins.get(0)) {
                Item item = itemList.get(itemIdx);
                System.out.printf("  Item %d (%.2fx%.2f, Price=%.2f)%n",
                        item.number, item.width, item.height, item.price);
            }
        }
        
        // Display items in each bin
        for (int binIdx = 1; binIdx <= binList.size(); binIdx++) {
            List<Integer> itemsInThisBin = result.itemsInBins.get(binIdx);
            
            if (!itemsInThisBin.isEmpty()) {
                Bin bin = binList.get(binIdx - 1);
                System.out.printf("%nBin %d (%.2fx%.2f, Area: %.2f):%n",
                        bin.number, bin.width, bin.height, bin.width * bin.height);
                
                double binItemsArea = 0.0;
                double binItemsPrice = 0.0;
                
                for (int itemIdx : itemsInThisBin) {
                    Item item = itemList.get(itemIdx);
                    double itemArea = item.width * item.height;
                    binItemsArea += itemArea;
                    binItemsPrice += item.price;
                    System.out.printf("  Item %d (%.2fx%.2f, Price=%.2f, Area=%.2f)%n",
                            item.number, item.width, item.height, item.price, itemArea);
                }
                
                System.out.printf("  Total in bin: Area=%.2f/%.2f, Price=%.2f%n",
                        binItemsArea, bin.width * bin.height, binItemsPrice);
            }
        }

        System.out.printf("%nTotal Stored Price: %.2f (%.2f%%)%n", 
                result.totalStoredPrice, result.valuePercentage);
        System.out.printf("Total Stored Area: %.2f (%.2f%%)%n", 
                result.totalStoredArea, result.areaPercentage);
        System.out.printf("Fitness: %.6f%n", result.fitness);
    }
    
    // ---------- Main (Example Usage) ----------
    public static void main(String[] args) {
        // Create sample items and bins
        Random random = new Random(42);
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            double width = 2.0 + random.nextDouble() * 8.0;
            double height = 2.0 + random.nextDouble() * 8.0;
            double price = 10.0 + random.nextDouble() * 40.0;
            items.add(new Item(i + 1, width, height, price));
        }
        
        List<Bin> bins = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            double width = 2.0 + random.nextDouble() * 38.0;
            double height = 2.0 + random.nextDouble() * 48.0;
            bins.add(new Bin(i + 1, width, height));
        }
        
        // Create optimizer instance and run
        InventoryOptimizationItemPerBin optimizer = new InventoryOptimizationItemPerBin();
        optimizer.setWeightW(0.7);
        optimizer.setPopulationSize(120);
        optimizer.setMaxGenerations(150);
        
        OptimizationResult result = optimizer.optimize(items, bins);
        optimizer.printResult(result, items, bins);
    }
}
