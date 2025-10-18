
import io.jenetics.*;
import io.jenetics.engine.*;
import io.jenetics.util.*;
import java.util.*;

public class InventoryOptimizationReal{

    // Define item data (IDs: 1=A, 2=B, 3=C, 4=D)
    static final Map<Integer, Integer> PRICE = Map.of(
        1, 100,  // A
        2, 150,  // B
        3, 70,   // C
        4, 300   // D
    );

    static final Map<Integer, Integer> QUANTITY = Map.of(
        1, 10,  // A
        2, 20,  // B
        3, 5,   // C
        4, 10   // D
    );

    // Number of bins (inventory spaces)
    static final int BIN_COUNT = 20;

    // Fitness function (maximize total value, minimize penalty)
    static double fitness(Genotype<DoubleGene> gt) {
        DoubleChromosome chromosome = (DoubleChromosome) gt.chromosome();

        // Round to nearest integer for item ID
        List<Integer> itemIDs = new ArrayList<>();
        for (DoubleGene gene : chromosome) {
            int id = (int) Math.round(gene.doubleValue());
            id = Math.min(Math.max(id, 1), 4); // clamp to 1–4
            itemIDs.add(id);
        }

        // Count usage of each item
        Map<Integer, Integer> used = new HashMap<>();
        for (int id = 1; id <= 4; id++) used.put(id, 0);
        for (int id : itemIDs) used.put(id, used.get(id) + 1);

        // Calculate total value
        double totalValue = 0;
        for (int id : itemIDs) totalValue += PRICE.get(id);

        // Penalty if quantity exceeded
        double penalty = 0;
        for (int id = 1; id <= 4; id++) {
            int excess = used.get(id) - QUANTITY.get(id);
            if (excess > 0) {
                penalty += excess * PRICE.get(id) * 0.5;
            }
        }

        return totalValue - penalty;
    }

    public static void main(String[] args) {
        // Define chromosome (each bin holds one real number between 1–4)
        Factory<Genotype<DoubleGene>> gtf = Genotype.of(
            DoubleChromosome.of(1, 4, BIN_COUNT)
        );

        // Build the evolutionary engine
        Engine<DoubleGene, Double> engine = Engine.builder(InventoryOptimizationReal::fitness, gtf)
            .populationSize(100)
            .selector(new TournamentSelector<>(3))
            .alterers(
                new Mutator<>(0.1),
                new MeanAlterer<>(0.3)
            )
            .build();

        // Run for 100 generations
        EvolutionResult<DoubleGene, Double> result = engine.stream()
            .limit(100)
            .collect(EvolutionResult.toBestEvolutionResult());

        // Print best result
        System.out.println("=== Best Inventory Plan (Real-Valued Chromosome) ===");
        DoubleChromosome bestChromosome = (DoubleChromosome) result.bestPhenotype().genotype().chromosome();

        for (int i = 0; i < BIN_COUNT; i++) {
            int itemID = (int) Math.round(bestChromosome.get(i).doubleValue());
            System.out.printf("Bin %02d -> Item %d%n", i + 1, itemID);
        }

        System.out.println("Fitness (Total Value): " + result.bestFitness());
    }
}
