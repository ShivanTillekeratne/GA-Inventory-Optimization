package com.ga_inventory_opt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ga_inventory_opt.InventoryOptimizationSingleItemPerBin.Bin;
import com.ga_inventory_opt.InventoryOptimizationSingleItemPerBin.Item;
import com.ga_inventory_opt.InventoryOptimizationSingleItemPerBin.OptimizationResult;

public class TestOptimizer {
    
    public static void main(String[] args) {
        // Create your items and bins
        List<Item> items = new ArrayList<>();
        items.add(new Item(1, 5.0, 3.0, 25.0));
        items.add(new Item(2, 7.0, 4.0, 35.0));
        items.add(new Item(3, 6.0, 5.0, 30.0));
        items.add(new Item(4, 4.0, 3.5, 20.0));
        items.add(new Item(5, 8.0, 6.0, 45.0));
        items.add(new Item(6, 3.5, 2.5, 15.0));
        items.add(new Item(7, 5.5, 4.5, 28.0));
        items.add(new Item(8, 6.5, 5.5, 38.0));

        List<Bin> bins = new ArrayList<>();
        bins.add(new Bin(1, 20.0, 30.0));
        bins.add(new Bin(2, 15.0, 25.0));
        bins.add(new Bin(3, 18.0, 28.0));
        bins.add(new Bin(4, 25.0, 35.0));

        // Create optimizer and configure
        InventoryOptimizationSingleItemPerBin optimizer = new InventoryOptimizationSingleItemPerBin();
        optimizer.setWeightW(0.75);
        optimizer.setPopulationSize(120);
        optimizer.setMaxGenerations(150);

        System.out.println("Starting inventory optimization...\n");

        // Run optimization
        OptimizationResult result = optimizer.optimize(items, bins);

        // Print or use results
        optimizer.printResult(result, items, bins);
        
        System.out.println("\n=== Sending result ===");

        System.out.println(result.itemsInBins);
        System.out.println("\n=== SUMMARY ===");
        System.out.println("Items stored successfully: " + 
            (items.size() - result.itemsInBins.get(0).size()) + " out of " + items.size());
        System.out.println("Bins utilized: " + countUtilizedBins(result) + " out of " + bins.size());

        List<List<Integer>> processedBins = new ArrayList<>();
        for (int i = 1; i < result.itemsInBins.size(); i++) {  // Skip index 0 (not in storage)
            List<Integer> binItems = result.itemsInBins.get(i);
            List<Integer> incrementedItems = new ArrayList<>();
            for (int itemIdx : binItems) {
                incrementedItems.add(itemIdx + 1);  // Increment each value by 1
            }
            processedBins.add(incrementedItems);
        }

        System.out.println("Processed bins (2D array format):" + processedBins);
        // for (int i = 0; i < processedBins.size(); i++) {
        //     System.out.println("Bin " + (i + 1) + ": " + processedBins.get(i));
        // }

        Map<String, List<Integer>> binMap = new HashMap<>();
        for (int i = 0; i < processedBins.size(); i++) {
            binMap.put("bin" + (i + 1), processedBins.get(i));
        }

        // Example usage:
        System.out.println("Bin map: " + binMap);
    }
    
    private static int countUtilizedBins(OptimizationResult result) {
        int count = 0;
        for (int i = 1; i < result.itemsInBins.size(); i++) {
            if (!result.itemsInBins.get(i).isEmpty()) {
                count++;
            }
        }
        return count;
    }
}
