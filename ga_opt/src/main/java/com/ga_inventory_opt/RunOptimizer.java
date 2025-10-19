package com.ga_inventory_opt;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

public class RunOptimizer {

    static class Optimizer {
        public static String run(java.util.List<Item> items, java.util.List<Bin> bins) {
            // Placeholder for the actual optimization logic using Jenetics
            // For demonstration, we return a dummy best solution
            return "OptimizedSolutionPlaceholder";
        }
    }
    static class Item {
        public int number;
        public double width;
        public double height;
        public double price;

    }
    static class Bin {
        public int number;
        public double width;
        public double height;
    }
    static class OptimizationInput {
        public java.util.List<Item> items;
        public java.util.List<Bin> bins;
    }

    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        // Read JSON input from stdin
        OptimizationInput input = mapper.readValue(System.in, OptimizationInput.class);

        // Run your Jenetics algorithm
        String bestSolution = Optimizer.run(input.items, input.bins);

        // Output JSON result
        mapper.writeValue(System.out, Map.of("bestSolution", bestSolution));
    }
}
