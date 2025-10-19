package com.ga_inventory_opt;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ga_inventory_opt.InventoryOptimizationWithPositions.Bin;
import com.ga_inventory_opt.InventoryOptimizationWithPositions.Item;
import com.ga_inventory_opt.InventoryOptimizationWithPositions.OptimizationResult;

public class RunOptimizer {
    Map<Integer, Map<Integer, Integer>> run(List<ItemType> itemTypes, List<BinType> binTypes, double fitnessWeight, int populationSize, int maxGenerations) {
        List<Item> items = new ArrayList<>();
        Map<Integer, Integer> itemToType = new HashMap<>();  // itemId to itemTypeNumber
        int itemIndex = 0;
        for (ItemType itemType : itemTypes) {
            for (int i = 0; i < itemType.quantity; i++) {
                itemIndex++;
                Item item = new Item(itemIndex, itemType.width, itemType.height, itemType.price);
                items.add(item);
                itemToType.put(itemIndex, itemType.number);
            }
        }
        // To avoid type conversion issue
        List<Bin> bins = new ArrayList<>();
        for (BinType binType : binTypes) {
            Bin bin = new Bin(binType.number, binType.width, binType.height);
            bins.add(bin);
        }

        // Create optimizer and configure
        InventoryOptimizationWithPositions internalOptimizer = new InventoryOptimizationWithPositions();
        internalOptimizer.setWeightW(fitnessWeight);
        internalOptimizer.setPopulationSize(populationSize);
        internalOptimizer.setMaxGenerations(maxGenerations);

        // Run optimization
        OptimizationResult result = internalOptimizer.optimize(items, bins);

        List<List<Integer>> processedBins = new ArrayList<>();
        for (int i = 1; i < result.itemsInBins.size(); i++) {  // Skip index 0 (not in storage)
            List<Integer> binItems = result.itemsInBins.get(i);
            List<Integer> incrementedItems = new ArrayList<>();
            for (int itemIdx : binItems) {
                incrementedItems.add(itemIdx + 1);  // Increment each value by 1
            }
            processedBins.add(incrementedItems);
        }

        Map<Integer, List<Integer>> binMap = new HashMap<>();
        for (int i = 0; i < processedBins.size(); i++) {
            binMap.put((i + 1), processedBins.get(i));
        }

        // MAPPING LOGIC: Bin to Type Counts
        // =================================
        // Item to type
        Map<Integer, Map<Integer, Integer>> binToTypeCounts = new HashMap<>();

        // Iterate over each entry (binId, itemList) in the first map
        for (Map.Entry<Integer, List<Integer>> binEntry : binMap.entrySet()) {
            Integer binId = binEntry.getKey();
            List<Integer> itemsInBin = binEntry.getValue();

            // Create a temporary map to hold type counts for THIS bin only
            Map<Integer, Integer> typeCountsInBin = new HashMap<>();

            // Now, iterate over the items within the current bin
            for (Integer itemId : itemsInBin) {
                Integer type = itemToType.get(itemId);

                // Check if the type exists to avoid errors
                if (type != null) {
                    // Increment the count for this type.
                    // getOrDefault is a clean way to handle the first time we see a type.
                    typeCountsInBin.put(type, typeCountsInBin.getOrDefault(type, 0) + 1);
                }
            }
            // Add the completed type counts map for the bin to our final result
            binToTypeCounts.put(binId, typeCountsInBin);
        }

        return binToTypeCounts;
    }
    static class ItemType {
        public int number;
        public double width;
        public double height;
        public double price;
        public int quantity;
    }

    static class BinType {
        public int number;
        public double width;
        public double height;
    }

    static class OptimizationInput {
        public java.util.List<ItemType> itemTypes;
        public java.util.List<BinType> binTypes;
    }

    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        // Read JSON input from stdin
        OptimizationInput input = mapper.readValue(System.in, OptimizationInput.class);

        // Run Jenetics algorithm
        RunOptimizer optimizer = new RunOptimizer();
        Map<Integer, Map<Integer, Integer>> itemToBinAssignment = optimizer.run(input.itemTypes, input.binTypes, 0.75, 1200, 150);

        // Output JSON result
        mapper.writeValue(System.out, itemToBinAssignment);
    }

}
