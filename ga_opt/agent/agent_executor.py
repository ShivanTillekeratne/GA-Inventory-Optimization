import os
import json
from typing import List
from pydantic import BaseModel, Field
from agents import Agent, Runner, function_tool
from dotenv import load_dotenv

load_dotenv()
OPENAI_API_KEY = os.getenv('OPENAI_API_KEY')
if not OPENAI_API_KEY:
    raise ValueError("Error: OPENAI_API_KEY not found in .env file.")


# Define Pydantic models for structured output
class Item(BaseModel):
    number: int = Field(description="Unique identifier for the item")
    width: float = Field(description="Width of the item")
    height: float = Field(description="Height of the item")
    price: float = Field(description="Price of the item")
    quantity: int = Field(description="Quantity of items")


class Bin(BaseModel):
    number: int = Field(description="Unique identifier for the bin")
    width: float = Field(description="Width of the bin")
    height: float = Field(description="Height of the bin")


class WarehouseData(BaseModel):
    items: List[Item] = Field(description="List of items to be packed")
    bins: List[Bin] = Field(description="List of available bins")


class WarehouseAgent:
    """Single agent to handle all warehouse optimization tasks"""
    
    def __init__(self):
        self.agent = Agent(
            name="Warehouse Assistant",
            instructions="""
            You are a warehouse optimization assistant that handles two main tasks:
            
            1. PARSING INPUT: When given a description of items and bins, parse it into structured format.
               - Extract item dimensions (e.g., '5x3' means width=5, height=3)
               - Extract item prices and quantities
               - Extract bin dimensions
               - Assign unique sequential numbers starting from 1
            
            2. CREATING VISUALIZATIONS: When given bin allocation data, create a markdown table.
               - Format as a clean table with 'Bin' and 'Items' columns
               - Each row shows one bin and its assigned items
               - Items should be comma-separated (e.g., "1, 4, 6")
            
            Always provide clear, well-structured output based on the task requested.
            """,
            model="gpt-4o"
        )
    
    def parse_user_input_to_json(self, user_text: str) -> dict:
        """Parse natural language input into structured warehouse data"""
        result = Runner.run_sync(
            starting_agent=self.agent,
            input=f"""Parse this warehouse data into a structured JSON format with 'items' and 'bins' arrays.
            
User input: {user_text}

Return a JSON object with:
- items: array of objects with number, width, height, price, quantity
- bins: array of objects with number, width, height

Be precise with number extraction and assign sequential numbers starting from 1."""
        )
        
        # Parse the text response as JSON
        try:
            return json.loads(result.final_output)
        except json.JSONDecodeError:
            # If response contains markdown code blocks, extract JSON
            text = result.final_output
            if "```json" in text:
                text = text.split("```json")[1].split("```")[0].strip()
            elif "```" in text:
                text = text.split("```")[1].split("```")[0].strip()
            return json.loads(text)
    
    def parse_java_hashmap(self, hashmap_str: str) -> dict:
        """Parse Java-style HashMap string to Python dict"""
        hashmap_str = hashmap_str.strip().replace("{", "").replace("}", "")
        parts = hashmap_str.split("],")
        hashmap_dict = {}
        
        for part in parts:
            part = part.strip()
            if "=" in part:
                key, val = part.split("=", 1)
                key = key.strip()
                val = val.strip().replace("[", "").replace("]", "")
                items = [x.strip() for x in val.split(",") if x.strip()]
                hashmap_dict[key] = items
        
        return hashmap_dict
    
    def create_markdown_table(self, java_hashmap_str: str) -> str:
        """Generate markdown table from bin allocation data"""
        bins_data = self.parse_java_hashmap(java_hashmap_str)
        
        # Format the data for the agent
        formatted_data = "\n".join([
            f"{bin_name}: items {', '.join(items)}"
            for bin_name, items in bins_data.items()
        ])
        
        result = Runner.run_sync(
            starting_agent=self.agent,
            input=f"""Create a clean markdown table for this bin allocation.
            
Allocation data:
{formatted_data}

Create a table with columns 'Bin' and 'Items'. Each row should show the bin name and its assigned items as a comma-separated list.
Return ONLY the markdown table, no other text."""
        )
        
        return result.final_output.strip()


def collect_input_with_loops() -> str:
    """Interactive function to collect item and bin information from user"""
    item_descriptions = []
    bin_descriptions = []
    
    try:
        num_items = int(input("How many item types do you have? "))
        for i in range(num_items):
            dims = input(f"  Enter dimensions for item #{i+1} (e.g., '5x3'): ")
            price = input(f"  Enter price for item #{i+1}: ")
            quantity = input(f"  Enter quantity for item #{i+1}: ")
            item_descriptions.append(
                f"item {i+1} is {dims} with a price of {price} and quantity of {quantity}. "
            )
    except ValueError:
        print("Invalid number. Please enter a whole number.")
        return None
    
    try:
        num_bins = int(input("How many bins do you have? "))
        for i in range(num_bins):
            dims = input(f"  Enter dimensions for bin #{i+1} (e.g., '20x30'): ")
            bin_descriptions.append(f"bin {i+1} is {dims}")
    except ValueError:
        print("Invalid number. Please enter a whole number.")
        return None
    
    full_description = f"I have {num_items} item types and {num_bins} bins. "
    full_description += "The items are: " + ", ".join(item_descriptions) + ". "
    full_description += "The bins are: " + ", ".join(bin_descriptions) + "."
    
    return full_description


if __name__ == "__main__":
    print("--- Starting Input Processing Workflow with OpenAI Agents SDK ---")
    
    # Initialize single agent
    warehouse_agent = WarehouseAgent()
    
    # Collect user input
    user_input_text = collect_input_with_loops()
    
    if user_input_text:
        # Parse input using the agent
        parsed_data = warehouse_agent.parse_user_input_to_json(user_input_text)
        
        print("\n--- Parsed JSON Data ---")
        print(json.dumps(parsed_data, indent=2))
        print("----------------------\n")
        
        print("Data has been sent to the optimization function...")
        
        # Simulate optimization output
        dummy_optimization_output = "{bin1=[1, 4, 6], bin2=[2, 5, 8], bin3=[3], bin4=[7]}"
        print(f"Status: Optimization complete. Result: {dummy_optimization_output}\n")
        
        # Generate visualization using the same agent
        markdown_table = warehouse_agent.create_markdown_table(dummy_optimization_output)
        
        print("--- Generated Visualization ---")
        print(markdown_table)
        print("-----------------------------")
    
    print("\n--- Workflow Finished ---")