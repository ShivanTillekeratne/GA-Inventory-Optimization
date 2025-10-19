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
import io.jenetics.SinglePointCrossover;
import io.jenetics.engine.Constraint;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionResult;
import io.jenetics.util.Factory;

public class InventoryOptimizationWithPositions {
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
    
    public static class ItemPlacement {
        public final int itemIdx;
        public final int binIdx;
        public final double row;      // x coordinate
        public final double col;      // y coordinate
        public final boolean rotated; // 90 degree rotation
        
        public ItemPlacement(int itemIdx, int binIdx, double row, double col, boolean rotated) {
            this.itemIdx = itemIdx;
            this.binIdx = binIdx;
            this.row = row;
            this.col = col;
            this.rotated = rotated;
        }
    }
    
    public static class OptimizationResult {
        public final List<List<Integer>> itemsInBins; // itemsInBins.get(i) = list of item numbers in bin i
        public final List<ItemPlacement> itemPlacements; // Position info for each item
        public final double fitness;
        public final double totalStoredPrice;
        public final double totalStoredArea;
        public final double valuePercentage;
        public final double areaPercentage;
        
        public OptimizationResult(List<List<Integer>> itemsInBins, List<ItemPlacement> itemPlacements,
                                 double fitness, double totalStoredPrice, double totalStoredArea,
                                 double valuePercentage, double areaPercentage) {
            this.itemsInBins = itemsInBins;
            this.itemPlacements = itemPlacements;
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
        
        // Find max bin dimensions for coordinate bounds
        double maxBinHeight = 0.0;
        double maxBinWidth = 0.0;
        for (int i = 0; i < numBins; i++) {
            if (bins[i][1] > maxBinHeight) maxBinHeight = bins[i][1];
            if (bins[i][0] > maxBinWidth) maxBinWidth = bins[i][0];
        }
        
        // Run genetic algorithm
        // Each item needs 4 genes: bin_number, row_coordinate, col_coordinate, rotation
        // Chromosome length = numItems * 4
        Factory<Genotype<DoubleGene>> genotypeFactory = Genotype.of(
            DoubleChromosome.of(0, numBins, numItems),           // Gene 0, 4, 8, 12... : bin number (0 to numBins)
            DoubleChromosome.of(0, maxBinHeight, numItems),      // Gene 1, 5, 9, 13... : row coordinate (0 to maxBinHeight)
            DoubleChromosome.of(0, maxBinWidth, numItems),       // Gene 2, 6, 10, 14...: col coordinate (0 to maxBinWidth)
            DoubleChromosome.of(0, 1, numItems)                  // Gene 3, 7, 11, 15...: rotation (0 or 1)
        );

        Engine<DoubleGene, Double> engine = Engine.builder(this::fitness, genotypeFactory)
                .optimize(Optimize.MAXIMUM)
                .populationSize(populationSize)
                .alterers(
                    new Mutator<>(mutationRate),
                    new SinglePointCrossover<>(crossoverRate)
                )
                .constraint(Constraint.of(this::isValid, this::repair))
                .build();

        EvolutionResult<DoubleGene, Double> result = engine.stream()
                .limit(maxGenerations)
                .collect(EvolutionResult.toBestEvolutionResult());

        // Extract and return results
        return extractResult(result);
    }
    
    // ---------- Fitness Function ----------
    private double fitness(Genotype<DoubleGene> gt) {
        // Extract the 4 chromosomes
        DoubleChromosome binChromosome = (DoubleChromosome) gt.get(0);
        DoubleChromosome rowChromosome = (DoubleChromosome) gt.get(1);
        DoubleChromosome colChromosome = (DoubleChromosome) gt.get(2);
        DoubleChromosome rotChromosome = (DoubleChromosome) gt.get(3);

        // Track items in each bin
        List<List<Integer>> itemsInBins = new ArrayList<>();
        for (int i = 0; i <= numBins; i++) {
            itemsInBins.add(new ArrayList<>());
        }
        
        // Map items to bins based on chromosome
        for (int i = 0; i < numItems; i++) {
            int binIndex = (int) Math.round(binChromosome.get(i).doubleValue());
            // Ensure bin index is valid (0 to numBins)
            binIndex = Math.max(0, Math.min(numBins, binIndex));
            itemsInBins.get(binIndex).add(i);
        }
        
        double priceOfStoredProducts = 0.0;
        double areaOfStoredProducts = 0.0;
        
        // Track area used in each bin for binOptimizedScore calculation
        double[] binUsedAreas = new double[numBins];
        
        // Process each bin (skip bin 0 as it means "not in storage")
        // No need to check capacity here - constraint validator ensures all chromosomes are valid
        for (int binIdx = 1; binIdx <= numBins; binIdx++) {
            List<Integer> itemsInThisBin = itemsInBins.get(binIdx);
            
            double binUsedArea = 0.0;
            for (int itemIdx : itemsInThisBin) {
                areaOfStoredProducts += itemAreas[itemIdx];
                priceOfStoredProducts += items[itemIdx][2];
                binUsedArea += itemAreas[itemIdx];
            }
            binUsedAreas[binIdx - 1] = binUsedArea;
        }
        
        // Calculate binOptimizedScore
        double binOptimizedScore = 0.0;
        
        // Get items outside storage (bin 0)
        List<Integer> itemsOutside = itemsInBins.get(0);
        
        if (!itemsOutside.isEmpty()) {
            int binScoreSum = 0;
            
            // For each bin, check if any outside item could fit
            for (int binIdx = 0; binIdx < numBins; binIdx++) {
                double freeArea = binAreas[binIdx] - binUsedAreas[binIdx];
                
                boolean canFitAnyOutsideItem = false;
                
                // Check if any item from outside can fit in this bin's free area
                for (int outsideItemIdx : itemsOutside) {
                    if (itemAreas[outsideItemIdx] <= freeArea) {
                        canFitAnyOutsideItem = true;
                        break;
                    }
                }
                
                // If no outside item can fit, this bin gets score of 1
                if (!canFitAnyOutsideItem) {
                    binScoreSum += 1;
                }
                // If an outside item could fit, this bin gets score of 0 (penalty)
            }
            
            binOptimizedScore = (double) binScoreSum / numBins;
        } else {
            // No items outside - perfect optimization, score = 1
            binOptimizedScore = 1.0;
        }
        
        // Normalized scores
        double valueScore = (totalInventoryPrice == 0) ? 0.0 : priceOfStoredProducts / totalInventoryPrice;
        double areaScore = (totalBinArea == 0) ? 0.0 : areaOfStoredProducts / totalBinArea;

        // Updated fitness formula
        double F = (W) * valueScore + ((1 - W) / 2.0) * areaScore + ((1 - W) / 2.0) * binOptimizedScore;
        return F;
    }
    
    // ---------- Constraint Validator ----------
    private boolean isValid(Phenotype<DoubleGene, Double> phenotype) {
        Genotype<DoubleGene> gt = phenotype.genotype();
        
        // Extract the 4 chromosomes
        DoubleChromosome binChromosome = (DoubleChromosome) gt.get(0);
        DoubleChromosome rowChromosome = (DoubleChromosome) gt.get(1);
        DoubleChromosome colChromosome = (DoubleChromosome) gt.get(2);
        DoubleChromosome rotChromosome = (DoubleChromosome) gt.get(3);
        
        // Helper class to store item placement info
        class ItemInfo {
            int itemIdx;
            double row;
            double col;
            boolean rotated;
            double hRangeStart;
            double hRangeEnd;
            double vRangeStart;
            double vRangeEnd;
            
            ItemInfo(int itemIdx, double row, double col, boolean rotated, double itemWidth, double itemHeight) {
                this.itemIdx = itemIdx;
                this.row = row;
                this.col = col;
                this.rotated = rotated;
                
                if (!rotated) {
                    // No rotation: width stays width, height stays height
                    this.hRangeStart = col;
                    this.hRangeEnd = col + itemWidth;
                    this.vRangeStart = row;
                    this.vRangeEnd = row + itemHeight;
                } else {
                    // 90 degree rotation: width becomes height, height becomes width
                    this.hRangeStart = col;
                    this.hRangeEnd = col + itemHeight;
                    this.vRangeStart = row;
                    this.vRangeEnd = row + itemWidth;
                }
            }
        }
        
        // Track items in each bin
        List<List<ItemInfo>> itemsInBins = new ArrayList<>();
        for (int i = 0; i <= numBins; i++) {
            itemsInBins.add(new ArrayList<>());
        }
        
        // Step 1: Break into groups and map items to bins
        for (int i = 0; i < numItems; i++) {
            int binIndex = (int) Math.round(binChromosome.get(i).doubleValue());
            binIndex = Math.max(0, Math.min(numBins, binIndex));
            
            double row = rowChromosome.get(i).doubleValue();
            double col = colChromosome.get(i).doubleValue();
            boolean rotated = Math.round(rotChromosome.get(i).doubleValue()) == 1;
            
            double itemWidth = items[i][0];
            double itemHeight = items[i][1];
            
            ItemInfo info = new ItemInfo(i, row, col, rotated, itemWidth, itemHeight);
            itemsInBins.get(binIndex).add(info);
        }
        
        // Step 2: Validate each bin (skip bin 0 - not in storage)
        for (int binIdx = 1; binIdx <= numBins; binIdx++) {
            List<ItemInfo> itemsInThisBin = itemsInBins.get(binIdx);
            
            if (itemsInThisBin.isEmpty()) {
                continue; // Empty bin is valid
            }
            
            double binWidth = bins[binIdx - 1][0];
            double binHeight = bins[binIdx - 1][1];
            
            // Check each item in this bin
            for (ItemInfo item : itemsInThisBin) {
                // Check if item coordinates are out of bin bounds
                if (item.row > binHeight || item.col > binWidth) {
                    return false; // Chromosome invalid
                }
                
                // Check if item extends beyond bin boundaries
                if (item.hRangeEnd > binWidth || item.vRangeEnd > binHeight) {
                    return false; // Item doesn't fit in bin
                }
            }
            
            // Check for overlaps between items in this bin
            for (int i = 0; i < itemsInThisBin.size(); i++) {
                ItemInfo item1 = itemsInThisBin.get(i);
                
                for (int j = i + 1; j < itemsInThisBin.size(); j++) {
                    ItemInfo item2 = itemsInThisBin.get(j);
                    
                    // Check horizontal overlap
                    double hOverlapStart = Math.max(item1.hRangeStart, item2.hRangeStart);
                    double hOverlapEnd = Math.min(item1.hRangeEnd, item2.hRangeEnd);
                    double hOverlap = hOverlapEnd - hOverlapStart;
                    
                    // Check vertical overlap
                    double vOverlapStart = Math.max(item1.vRangeStart, item2.vRangeStart);
                    double vOverlapEnd = Math.min(item1.vRangeEnd, item2.vRangeEnd);
                    double vOverlap = vOverlapEnd - vOverlapStart;
                    
                    // If both horizontal and vertical overlap by more than 1 pixel, items overlap
                    if (hOverlap > 1 && vOverlap > 1) {
                        return false; // Items overlap
                    }
                }
            }
        }
        
        return true; // All constraints satisfied
    }
    
    // ---------- Repair Function ----------
    private Phenotype<DoubleGene, Double> repair(Phenotype<DoubleGene, Double> phenotype, Long generation) {
        Genotype<DoubleGene> gt = phenotype.genotype();
        
        // Extract the 4 chromosomes
        DoubleChromosome binChromosome = (DoubleChromosome) gt.get(0);
        DoubleChromosome rowChromosome = (DoubleChromosome) gt.get(1);
        DoubleChromosome colChromosome = (DoubleChromosome) gt.get(2);
        DoubleChromosome rotChromosome = (DoubleChromosome) gt.get(3);
        
        // Get the original ranges from the chromosomes
        double binMin = binChromosome.min().doubleValue();
        double binMax = binChromosome.max().doubleValue();
        double rowMin = rowChromosome.min().doubleValue();
        double rowMax = rowChromosome.max().doubleValue();
        double colMin = colChromosome.min().doubleValue();
        double colMax = colChromosome.max().doubleValue();
        double rotMin = rotChromosome.min().doubleValue();
        double rotMax = rotChromosome.max().doubleValue();
        
        // Repair bin numbers: round to nearest integer and clamp to [0, numBins]
        DoubleGene[] repairedBinGenes = new DoubleGene[numItems];
        for (int i = 0; i < numItems; i++) {
            double value = binChromosome.get(i).doubleValue();
            int binValue = (int) Math.round(value);
            binValue = Math.max(0, Math.min(numBins, binValue));
            repairedBinGenes[i] = DoubleGene.of(binValue, binMin, binMax);
        }
        DoubleChromosome repairedBinChromosome = DoubleChromosome.of(repairedBinGenes);
        
        // Repair rotation values: round to 0 or 1
        DoubleGene[] repairedRotGenes = new DoubleGene[numItems];
        for (int i = 0; i < numItems; i++) {
            double value = rotChromosome.get(i).doubleValue();
            int rotValue = (int) Math.round(value);
            rotValue = Math.max(0, Math.min(1, rotValue));
            repairedRotGenes[i] = DoubleGene.of(rotValue, rotMin, rotMax);
        }
        DoubleChromosome repairedRotChromosome = DoubleChromosome.of(repairedRotGenes);
        
        // Repair coordinates: clamp to bin boundaries
        DoubleGene[] repairedRowGenes = new DoubleGene[numItems];
        DoubleGene[] repairedColGenes = new DoubleGene[numItems];
        
        for (int i = 0; i < numItems; i++) {
            int binIndex = (int) Math.round(repairedBinGenes[i].doubleValue());
            
            // If item is in bin 0 (not stored), coordinates don't matter much
            if (binIndex == 0) {
                repairedRowGenes[i] = rowChromosome.get(i);
                repairedColGenes[i] = colChromosome.get(i);
            } else {
                // Clamp coordinates to the specific bin's dimensions
                double binWidth = bins[binIndex - 1][0];
                double binHeight = bins[binIndex - 1][1];
                
                double rowValue = rowChromosome.get(i).doubleValue();
                double colValue = colChromosome.get(i).doubleValue();
                
                // Get item dimensions considering rotation
                boolean rotated = (int) Math.round(repairedRotGenes[i].doubleValue()) == 1;
                double itemWidth = items[i][0];
                double itemHeight = items[i][1];
                double effectiveWidth = rotated ? itemHeight : itemWidth;
                double effectiveHeight = rotated ? itemWidth : itemHeight;
                
                // Clamp so item fits within bin
                double maxRow = Math.max(0, binHeight - effectiveHeight);
                double maxCol = Math.max(0, binWidth - effectiveWidth);
                
                rowValue = Math.max(0, Math.min(maxRow, rowValue));
                colValue = Math.max(0, Math.min(maxCol, colValue));
                
                // Use original range bounds for gene creation
                repairedRowGenes[i] = DoubleGene.of(rowValue, rowMin, rowMax);
                repairedColGenes[i] = DoubleGene.of(colValue, colMin, colMax);
            }
        }
        
        DoubleChromosome repairedRowChromosome = DoubleChromosome.of(repairedRowGenes);
        DoubleChromosome repairedColChromosome = DoubleChromosome.of(repairedColGenes);
        
        // Create new genotype with repaired chromosomes
        Genotype<DoubleGene> repairedGenotype = Genotype.of(
            repairedBinChromosome,
            repairedRowChromosome,
            repairedColChromosome,
            repairedRotChromosome
        );
        
        // Return new phenotype with repaired genotype
        return Phenotype.of(repairedGenotype, generation);
    }
    
    // ---------- Result Extraction ----------
    private OptimizationResult extractResult(EvolutionResult<DoubleGene, Double> result) {
        Genotype<DoubleGene> bestGenotype = result.bestPhenotype().genotype();
        
        // Extract the 4 chromosomes
        DoubleChromosome binChromosome = (DoubleChromosome) bestGenotype.get(0);
        DoubleChromosome rowChromosome = (DoubleChromosome) bestGenotype.get(1);
        DoubleChromosome colChromosome = (DoubleChromosome) bestGenotype.get(2);
        DoubleChromosome rotChromosome = (DoubleChromosome) bestGenotype.get(3);
        
        // Track items in each bin
        List<List<Integer>> itemsInBins = new ArrayList<>();
        for (int i = 0; i <= numBins; i++) {
            itemsInBins.add(new ArrayList<>());
        }
        
        // Store placement information for all items
        List<ItemPlacement> itemPlacements = new ArrayList<>();
        
        for (int i = 0; i < numItems; i++) {
            int binIndex = (int) Math.round(binChromosome.get(i).doubleValue());
            binIndex = Math.max(0, Math.min(numBins, binIndex));
            itemsInBins.get(binIndex).add(i);
            
            double row = rowChromosome.get(i).doubleValue();
            double col = colChromosome.get(i).doubleValue();
            boolean rotated = Math.round(rotChromosome.get(i).doubleValue()) == 1;
            
            itemPlacements.add(new ItemPlacement(i, binIndex, row, col, rotated));
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
            
            // Only count if it fits (should always fit due to constraint)
            if (binItemsArea <= binAreas[binIdx - 1]) {
                totalStoredArea += binItemsArea;
                totalStoredPrice += binItemsPrice;
            }
        }
        
        double valuePercentage = (totalInventoryPrice > 0) ? (totalStoredPrice / totalInventoryPrice) * 100 : 0.0;
        double areaPercentage = (totalBinArea > 0) ? (totalStoredArea / totalBinArea) * 100 : 0.0;
        
        return new OptimizationResult(itemsInBins, itemPlacements, result.bestFitness(), 
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
                ItemPlacement placement = result.itemPlacements.get(itemIdx);
                System.out.printf("  Item %d (%.2fx%.2f, Price=%.2f)%n",
                        item.number, item.width, item.height, item.price);
            }
        }
        
        // Display items in each bin with position information
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
                    ItemPlacement placement = result.itemPlacements.get(itemIdx);
                    double itemArea = item.width * item.height;
                    binItemsArea += itemArea;
                    binItemsPrice += item.price;
                    
                    String rotationStr = placement.rotated ? "ROTATED 90deg" : "NO ROTATION";
                    double displayWidth = placement.rotated ? item.height : item.width;
                    double displayHeight = placement.rotated ? item.width : item.height;
                    
                    System.out.printf("  Item %d (%.2fx%.2f, Price=%.2f, Area=%.2f) at Position(%.2f,%.2f) %s [Effective: %.2fx%.2f]%n",
                            item.number, item.width, item.height, item.price, itemArea,
                            placement.row, placement.col, rotationStr, displayWidth, displayHeight);
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

        // Run optimization
        InventoryOptimizationWithPositions optimizer = new InventoryOptimizationWithPositions();
        optimizer.setPopulationSize(100);
        optimizer.setMaxGenerations(100);
        
        OptimizationResult result = optimizer.optimize(items, bins);
        optimizer.printResult(result, items, bins);
    }
}
