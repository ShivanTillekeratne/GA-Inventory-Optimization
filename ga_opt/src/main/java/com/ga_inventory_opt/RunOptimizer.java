package com.ga_inventory_opt;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ga_inventory_opt.InventoryOptimizationItemPerBin.Bin;
import com.ga_inventory_opt.InventoryOptimizationItemPerBin.Item;
import com.ga_inventory_opt.InventoryOptimizationItemPerBin.OptimizationResult;

public class RunOptimizer {
    Map<Integer, List<Integer>> run(List<ItemType> itemTypes, List<BinType> binTypes, double fitnessWeight, int populationSize, int maxGenerations) {
        List<Item> items = new ArrayList<>();
        int itemIndex = 0;
        for (ItemType itemType : itemTypes) {
            itemIndex++;
            for (int i = 0; i < itemType.quantity; i++) {
                Item item = new Item(itemIndex, itemType.width, itemType.height, itemType.price);
                items.add(item);
            }
        }
        // To avoid type conversion issue
        List<Bin> bins = new ArrayList<>();
        for (BinType binType : binTypes) {
            Bin bin = new Bin(binType.number, binType.width, binType.height);
            bins.add(bin);
        }

        // Create optimizer and configure
        InventoryOptimizationItemPerBin internalOptimizer = new InventoryOptimizationItemPerBin();
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

        return binMap;
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
        Map<Integer, List<Integer>> itemToBinAssignment = optimizer.run(input.itemTypes, input.binTypes, 0.75, 1200, 150);

        // Output JSON result
        mapper.writeValue(System.out, itemToBinAssignment);
    }

}
