# Implementation Notes: Position-Based Genetic Algorithm for Inventory Optimization

## Overview
Updated the genetic algorithm representation to include explicit position coordinates and rotation for each item within bins, enabling precise 2D bin packing optimization.

## Key Changes

### 1. **New Chromosome Representation**
- **Old**: 1 gene per item (bin number only)
- **New**: 4 genes per item:
  1. **Bin number** (0 to numBins): 0 means "not in storage", 1+ are actual bins
  2. **Row coordinate** (0 to maxBinHeight): x-coordinate in the bin grid
  3. **Column coordinate** (0 to maxBinWidth): y-coordinate in the bin grid  
  4. **Rotation** (0 or 1): 0 = no rotation, 1 = 90° rotation

**Example**: For 4 items, chromosome length = 16 genes (4 genes × 4 items)

### 2. **Coordinate System**
- **Origin**: Bottom-left corner of each bin is (0, 0)
- **Top-right**: (bin_height, bin_width)
- **Position (x, y)**: x = row number, y = column number

### 3. **New Data Structures**

#### ItemPlacement Class
```java
public static class ItemPlacement {
    public final int itemIdx;
    public final int binIdx;
    public final double row;      // x coordinate
    public final double col;      // y coordinate
    public final boolean rotated; // 90 degree rotation
}
```

#### Updated OptimizationResult
- Added `itemPlacements` field to track position and rotation of each item

### 4. **Constraint Validation (isValid method)**

The constraint validator now performs comprehensive spatial validation:

#### Step 1: Parse Chromosome
- Break chromosome into groups of 4 genes per item
- Extract bin number, row, column, and rotation for each item
- Group items by their assigned bins

#### Step 2: Per-Bin Validation
For each bin (excluding bin 0 - "not in storage"):

1. **Boundary Check**: 
   - Reject if `row_coordinate > bin_height` OR `col_coordinate > bin_width`

2. **Calculate Item Ranges**:
   - If rotation = 0:
     - `h_range = (col, col + item_width)`
     - `v_range = (row, row + item_height)`
   - If rotation = 1:
     - `h_range = (col, col + item_height)`  
     - `v_range = (row, row + item_width)`

3. **Overlap Check**:
   - For each pair of items in the bin:
     - Calculate horizontal overlap between their h_ranges
     - Calculate vertical overlap between their v_ranges
     - If BOTH overlaps > 1 pixel → REJECT chromosome

4. **Bin Boundary Check**:
   - If any item's `h_range_end > bin_width` → REJECT
   - If any item's `v_range_end > bin_height` → REJECT

#### Step 3: Chromosome Decision
- If ALL bins pass all checks → chromosome is VALID
- If ANY bin fails ANY check → chromosome is REJECTED

### 5. **Fitness Function**
- **Unchanged**: Still uses the weighted formula
- `F = W × valueScore + (1 - W) × areaScore`
- Constraint validator ensures only valid placements are evaluated

### 6. **Genetic Algorithm Configuration**

The genotype factory now creates 4 chromosomes:
```java
Genotype.of(
    DoubleChromosome.of(0, numBins, numItems),        // Bin numbers
    DoubleChromosome.of(0, maxBinHeight, numItems),   // Row coordinates
    DoubleChromosome.of(0, maxBinWidth, numItems),    // Column coordinates
    DoubleChromosome.of(0, 1, numItems)               // Rotation flags
)
```

### 7. **Output Enhancement**
The `printResult` method now displays:
- Item position: `at Position(row, col)`
- Rotation status: `ROTATED 90deg` or `NO ROTATION`
- Effective dimensions after rotation: `[Effective: width x height]`

## Example Output
```
Bin 1 (30.00x40.00, Area: 1200.00):
  Item 5 (5.00x8.00, Price=25.00, Area=40.00) at Position(2.50,5.30) NO ROTATION [Effective: 5.00x8.00]
  Item 7 (6.00x4.00, Price=30.00, Area=24.00) at Position(12.00,8.50) ROTATED 90deg [Effective: 4.00x6.00]
```

## Benefits
1. **Precise Placement**: Items have exact coordinates, enabling visualization
2. **Rotation Handling**: Can optimize by rotating items 90°
3. **Realistic Constraints**: Checks actual physical overlaps, not just area
4. **Better Solutions**: More flexible representation can find better packings

## Usage
```java
InventoryOptimizationWithPositions optimizer = new InventoryOptimizationWithPositions();
optimizer.setPopulationSize(100);
optimizer.setMaxGenerations(100);

OptimizationResult result = optimizer.optimize(items, bins);
optimizer.printResult(result, items, bins);
```

## Notes
- The constraint validator is strict: any overlap > 1 pixel is rejected
- Rotation gene: 0 = no rotation, 1 = 90° clockwise rotation
- Bin 0 is reserved for items not placed in any storage bin
