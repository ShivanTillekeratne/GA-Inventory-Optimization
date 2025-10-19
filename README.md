# GA Inventory Optimization

A genetic algorithm-based inventory optimization system that solves bin packing problems. The system determines the optimal placement of items into bins to maximize both value and space utilization.

## Overview

This project uses a genetic algorithm to optimize inventory placement across multiple storage bins. It considers:
- Item dimensions (width × height)
- Item values/prices
- Bin capacities
- Multiple item quantities

The optimizer finds the best configuration to maximize stored value while efficiently using available bin space.

## Project Structure

```
ga_opt/
├── agent/
│   ├── bridge.py           # Python bridge to call Java optimizer
│   └── requirements.txt    # Python dependencies
├── src/main/java/          # Java genetic algorithm implementation
└── target/                 # Compiled JAR file
```

## Requirements

- **Java**: JDK 8 or higher
- **Python**: 3.7 or higher
- **Maven**: For building the Java project

## Installation

1. **Build the Java optimizer:**
   ```bash
   cd ga_opt
   mvn clean package
   ```
   This creates `target/optimizer-1.0.jar`

2. **Install Python dependencies:**
   ```bash
   cd agent
   pip install -r requirements.txt
   ```

## Usage

### Python Interface

```python
from bridge import call_java_optimizer

params = {
    "itemTypes": [
        {"number": 1, "width": 5.0, "height": 3.0, "price": 25.0, "quantity": 2},
        {"number": 2, "width": 10.0, "height": 15.0, "price": 55.0, "quantity": 1}
    ],
    "binTypes": [
        {"number": 1, "width": 20.0, "height": 30.0},
        {"number": 2, "width": 50.0, "height": 50.0}
    ]
}

result = call_java_optimizer(params)
print(result)
```

### Direct Java Execution

```bash
java -jar ga_opt/target/optimizer-1.0.jar < input.json
```

## Input Format

```json
{
  "itemTypes": [
    {
      "number": 1,
      "width": 5.0,
      "height": 3.0,
      "price": 25.0,
      "quantity": 20
    }
  ],
  "binTypes": [
    {
      "number": 1,
      "width": 120.0,
      "height": 30.0
    }
  ]
}
```

## Output Format

Returns a mapping of bins to item type counts:
```json
{
  "1": {"1": 5, "2": 3},
  "2": {"3": 2}
}
```

This means:
- Bin 1 contains 5 items of type 1 and 3 items of type 2
- Bin 2 contains 2 items of type 3

## Algorithm Parameters

The genetic algorithm uses:
- **Population Size**: 1200
- **Generations**: 150
- **Fitness Weight**: 0.75 (balance between value and space utilization)
- **Mutation Rate**: 0.2
- **Crossover Rate**: 0.3

## License

MIT
