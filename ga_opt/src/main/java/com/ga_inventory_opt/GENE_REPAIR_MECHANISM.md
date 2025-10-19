# Gene Repair Mechanism for Valid Chromosome Representation

## Problem
After crossover and mutation operations in the genetic algorithm, gene values can become invalid:
- **Bin gene**: May have non-integer values or values outside the valid range [0, numBins]
- **Rotation gene**: May have values other than 0 or 1
- **Coordinate genes**: May place items outside bin boundaries

## Solution: Repair Function

A `repair()` method is applied to fix invalid chromosomes after genetic operations.

### Function Signature
```java
private Phenotype<DoubleGene, Double> repair(
    Phenotype<DoubleGene, Double> phenotype, 
    Long generation
)
```

### How It Works

#### 1. **Bin Number Repair**
```java
// Round to nearest integer and clamp to valid range [0, numBins]
int binValue = (int) Math.round(value);
binValue = Math.max(0, Math.min(numBins, binValue));
```

**Example:**
- Input: 2.7 → Output: 3
- Input: -0.3 → Output: 0
- Input: 5.8 (when numBins=4) → Output: 4

#### 2. **Rotation Repair**
```java
// Round to nearest integer and clamp to {0, 1}
int rotValue = (int) Math.round(value);
rotValue = Math.max(0, Math.min(1, rotValue));
```

**Example:**
- Input: 0.3 → Output: 0
- Input: 0.6 → Output: 1
- Input: 1.2 → Output: 1
- Input: -0.1 → Output: 0

#### 3. **Coordinate Repair**
The repair ensures items stay within bin boundaries after considering rotation:

```java
// Calculate effective dimensions based on rotation
double effectiveWidth = rotated ? itemHeight : itemWidth;
double effectiveHeight = rotated ? itemWidth : itemHeight;

// Calculate maximum valid coordinates
double maxRow = Math.max(0, binHeight - effectiveHeight);
double maxCol = Math.max(0, binWidth - effectiveWidth);

// Clamp coordinates to valid range
rowValue = Math.max(0, Math.min(maxRow, rowValue));
colValue = Math.max(0, Math.min(maxCol, colValue));
```

**Why This Matters:**
- If an item is 5x8 and bin is 20x30:
  - Without rotation: maxRow = 30-8 = 22, maxCol = 20-5 = 15
  - With rotation: maxRow = 30-5 = 25, maxCol = 20-8 = 12

**Example:**
- Item: 5x8, Bin: 20x30, Rotation: 0
- Invalid row: 25 → Repaired: 22 (max valid is binHeight - itemHeight)
- Invalid col: 18 → Repaired: 15 (max valid is binWidth - itemWidth)

### Integration with Genetic Algorithm

```java
Engine<DoubleGene, Double> engine = Engine.builder(this::fitness, genotypeFactory)
    .optimize(Optimize.MAXIMUM)
    .populationSize(populationSize)
    .alterers(new Mutator<>(mutationRate), new UniformCrossover<>(crossoverRate))
    .constraint(Constraint.of(this::isValid, this::repair))  // <-- Repair function
    .build();
```

### Workflow

```
Initial Population
        ↓
    Selection
        ↓
    Crossover ←─────┐
        ↓           │
    Mutation        │
        ↓           │
[REPAIR GENES]      │
        ↓           │
[VALIDATE]          │
  (isValid)         │
        ↓           │
  Valid? ──No──> Retry with repair
        │           │
       Yes          │
        ↓           │
  Add to next ──────┘
   generation
```

### Benefits

1. **Ensures Valid Bin Numbers**: All items are assigned to valid bins (0 to numBins)
2. **Discrete Rotation**: Rotation is always exactly 0 or 1, never fractional
3. **Boundary Compliance**: Items are positioned within bin boundaries
4. **Maintains Search Space**: Allows exploration while ensuring feasibility
5. **Reduces Rejection**: Fewer chromosomes rejected, faster convergence

### Special Cases

#### Bin 0 (Not in Storage)
For items assigned to bin 0, coordinates are not repaired since they're not actually placed in any bin.

```java
if (binIndex == 0) {
    // Coordinates don't matter for non-stored items
    repairedRowGenes[i] = rowChromosome.get(i);
    repairedColGenes[i] = colChromosome.get(i);
}
```

#### Empty Bins
Bins with no items are considered valid and no repair is needed.

### Example Repair Scenario

**Before Repair (After Mutation):**
```
Item 3:
  Bin: 2.7
  Row: 45.2
  Col: 22.8
  Rotation: 0.75
```

**After Repair:**
```
Item 3:
  Bin: 3 (rounded from 2.7)
  Row: 42.0 (clamped to fit in Bin 3: height=50, item height=8)
  Col: 15.0 (clamped to fit in Bin 3: width=20, item width=5)
  Rotation: 1 (rounded from 0.75)
```

### Performance Impact

- **Time Complexity**: O(numItems) per repair operation
- **Frequency**: Applied after every crossover and mutation
- **Trade-off**: Slight computational overhead for guaranteed validity

### Important Notes

1. **Repair happens BEFORE validation**: The repaired chromosome is then checked by `isValid()`
2. **Rotation-aware**: Coordinates consider the item's orientation
3. **Bin-specific**: Each item's coordinates are clamped to its assigned bin's dimensions
4. **Deterministic**: Same invalid chromosome always produces the same repaired version

## Testing the Repair Function

To verify the repair function is working:

```java
// Check console output during evolution
// Valid chromosomes should pass through validation
// Rejected chromosomes should be rare after repair
```

## Summary

The repair mechanism ensures that after crossover and mutation:
- ✅ Bin numbers are integers in [0, numBins]
- ✅ Rotation values are exactly 0 or 1
- ✅ Coordinates place items within bin boundaries
- ✅ Item dimensions (with rotation) are considered
- ✅ Genetic diversity is maintained while ensuring feasibility
