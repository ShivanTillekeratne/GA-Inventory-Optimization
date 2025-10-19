# Chromosome-Specific Crossover Strategy

## Problem with UniformCrossover

Previously, using `UniformCrossover` would mix genes across **all chromosomes indiscriminately**:

```
Parent 1: [Bins: 1,2,3] [Rows: 5,10,15] [Cols: 3,6,9] [Rot: 0,1,0]
Parent 2: [Bins: 2,1,3] [Rows: 8,12,4]  [Cols: 7,2,5] [Rot: 1,0,1]
          
Offspring (UniformCrossover - BAD):
          [Bins: 1,1,3] [Rows: 8,10,4] [Cols: 7,6,5] [Rot: 1,1,1]
          ↑     ↑   ↑    ↑   ↑   ↑     ↑   ↑   ↑     ↑   ↑   ↑
          P1    P2  P1   P2  P1  P2    P2  P1  P2    P2  P1  P2
```

**Issue**: This crosses over randomly between bin genes, row genes, col genes, and rotation genes, potentially creating invalid combinations.

## Solution: SinglePointCrossover

`SinglePointCrossover` operates on **each chromosome independently**, ensuring:
- Bin genes only recombine with bin genes
- Row coordinates only recombine with row coordinates
- Column coordinates only recombine with column coordinates
- Rotation genes only recombine with rotation genes

### How It Works

For a genotype with 4 chromosomes, crossover happens **separately** on each:

```
Parent 1 Genotype:
  - Chromosome 0 (Bins):     [1, 2, 3, 2]
  - Chromosome 1 (Rows):     [5, 10, 15, 20]
  - Chromosome 2 (Cols):     [3, 6, 9, 12]
  - Chromosome 3 (Rotation): [0, 1, 0, 1]

Parent 2 Genotype:
  - Chromosome 0 (Bins):     [2, 1, 3, 1]
  - Chromosome 1 (Rows):     [8, 12, 4, 18]
  - Chromosome 2 (Cols):     [7, 2, 5, 10]
  - Chromosome 3 (Rotation): [1, 0, 1, 0]

Crossover at index 2 (independently on each chromosome):

Offspring 1:
  - Bins:     [1, 2 | 3, 1]  (first 2 from P1, rest from P2)
  - Rows:     [5, 10 | 4, 18]
  - Cols:     [3, 6 | 5, 10]
  - Rotation: [0, 1 | 1, 0]

Offspring 2:
  - Bins:     [2, 1 | 3, 2]  (first 2 from P2, rest from P1)
  - Rows:     [8, 12 | 15, 20]
  - Cols:     [7, 2 | 9, 12]
  - Rotation: [1, 0 | 0, 1]
```

### Implementation

```java
import io.jenetics.SinglePointCrossover;

Engine<DoubleGene, Double> engine = Engine.builder(this::fitness, genotypeFactory)
    .optimize(Optimize.MAXIMUM)
    .populationSize(populationSize)
    .alterers(
        new Mutator<>(mutationRate),
        new SinglePointCrossover<>(crossoverRate)  // <-- Chromosome-aware crossover
    )
    .constraint(Constraint.of(this::isValid, this::repair))
    .build();
```

## Benefits

### 1. **Semantic Integrity**
Bins stay with bins, rotations stay with rotations. No mixing of incompatible gene types.

### 2. **Meaningful Building Blocks**
Preserves useful combinations within each chromosome:
- Good bin assignments stay together
- Effective coordinate patterns are maintained
- Rotation patterns are preserved

### 3. **Reduced Need for Repair**
Since genes of the same type recombine:
- Bin genes remain close to integers
- Rotation genes remain close to 0 or 1
- Coordinates stay meaningful for their respective dimensions

### 4. **Better Convergence**
The GA can build up good solutions more systematically:
- Items with good bin assignments can be inherited together
- Spatial positioning patterns can be preserved
- Rotation strategies can evolve independently

## Comparison

| Aspect | UniformCrossover | SinglePointCrossover |
|--------|------------------|----------------------|
| **Scope** | Across all genes in genotype | Within each chromosome separately |
| **Gene mixing** | Any gene can mix with any gene | Only genes from same chromosome mix |
| **Semantic respect** | No - mixes different types | Yes - respects chromosome boundaries |
| **Building blocks** | Disrupted | Preserved |
| **Repair needed** | More frequent | Less frequent |
| **Search behavior** | More exploratory/random | More structured/focused |

## Other Crossover Options

If you want even more control, Jenetics provides:

### MultiPointCrossover
Multiple crossover points per chromosome:
```java
new MultiPointCrossover<>(crossoverRate, 2)  // 2 crossover points
```

### MeanCrossover
Takes the mean of parent values (good for coordinates):
```java
new MeanCrossover<>(crossoverRate)
```

### IntermediateCrossover
Weighted average of parent values:
```java
new IntermediateCrossover<>(crossoverRate)
```

## Example Scenario

### Problem Setup
- 4 items, 3 bins
- Item 0 fits well in Bin 1 at position (2, 5) without rotation
- Item 1 fits well in Bin 2 at position (10, 3) with rotation

### With UniformCrossover (Bad)
```
Parent 1: Item 0 → Bin 1, (2,5), Rot=0  ✓ Good
Parent 2: Item 0 → Bin 2, (8,4), Rot=1

Offspring: Item 0 → Bin 2, (2,5), Rot=0  ✗ Bad combo!
(Took bin from P2, position from P1, rotation from P1 - inconsistent)
```

### With SinglePointCrossover (Good)
```
Parent 1: Item 0 → Bin 1, (2,5), Rot=0  ✓ Good
Parent 2: Item 0 → Bin 2, (8,4), Rot=1

Offspring: Item 0 → Bin 1, (2,5), Rot=0  ✓ Inherits complete solution
(All genes for Item 0 come from same parent - consistent)
```

## Mutation Still Works Independently

Mutation operates on individual genes, so it still provides diversity:

```java
new Mutator<>(mutationRate)  // Mutates individual genes within any chromosome
```

The combination of:
- **SinglePointCrossover**: Preserves building blocks within chromosomes
- **Mutator**: Introduces variations at gene level
- **Repair**: Ensures validity

...creates a robust evolutionary process!

## Key Takeaway

`SinglePointCrossover` respects the **semantic structure** of your chromosome representation:
- Each of the 4 chromosomes represents a different property type
- Crossover within each chromosome preserves meaningful patterns
- This leads to better evolution of solutions in bin packing problems

```
✓ Bin genes ↔ Bin genes
✓ Row genes ↔ Row genes  
✓ Col genes ↔ Col genes
✓ Rot genes ↔ Rot genes

✗ Bin genes ↮ Row genes (prevented!)
✗ Col genes ↮ Rot genes (prevented!)
```
