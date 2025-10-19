import os
import json
from openai import OpenAI
from dotenv import load_dotenv

# Load API key from .env
load_dotenv()
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")
if not OPENAI_API_KEY:
    raise ValueError("Error: OPENAI_API_KEY not found in .env file.")

client = OpenAI(api_key=OPENAI_API_KEY)

# ----------------------------
# Input Processor
# ----------------------------
class InputProcessor:
    def __init__(self):
        self.model = "gpt-4o"

    def parse_user_input_to_json(self, user_text: str):
        json_schema = {
            "type": "object",
            "properties": {
                "items": {
                    "type": "array",
                    "items": {
                        "type": "object",
                        "properties": {
                            "number": {"type": "integer"},
                            "width": {"type": "number"},
                            "height": {"type": "number"},
                            "price": {"type": "number"},
                            "quantity": {"type": "integer"},
                        },
                        "required": ["number", "width", "height", "price", "quantity"],
                    },
                },
                "bins": {
                    "type": "array",
                    "items": {
                        "type": "object",
                        "properties": {
                            "number": {"type": "integer"},
                            "width": {"type": "number"},
                            "height": {"type": "number"},
                        },
                        "required": ["number", "width", "height"],
                    },
                },
            },
            "required": ["items", "bins"],
        }

        prompt = f"""
        You are a parser that converts human descriptions of items and bins into a structured JSON object.
        Use this schema: {json_schema}.
        Assign unique incremental numbers starting from 1 to each item and bin.

        USER INPUT:
        {user_text}
        """

        response = client.responses.create(
            model=self.model,
            reasoning={"effort": "medium"},
            response_format={"type": "json_schema", "json_schema": {"schema": json_schema}},
            input=prompt,
        )
        return json.loads(response.output[0].content[0].text)

# ----------------------------
# Visualization Generator
# ----------------------------
class VisualizationGenerator:
    def __init__(self):
        self.model = "gpt-4o-mini"

    def parse_java_hashmap(self, hashmap_str):
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

    def create_markdown_table(self, java_hashmap_str: str):
        bins_data = self.parse_java_hashmap(java_hashmap_str)
        prompt = f"""
        You are a visualization assistant.
        Convert the following bin-item mapping into a Markdown table.

        {json.dumps(bins_data, indent=2)}

        Each row represents one bin.
        Columns: "Bin" | "Items"
        Only return the Markdown table. No extra text.
        """
        response = client.chat.completions.create(
            model=self.model,
            messages=[
                {"role": "system", "content": "You are a data visualization expert."},
                {"role": "user", "content": prompt},
            ],
        )
        return response.choices[0].message.content


# ----------------------------
# Input Collection
# ----------------------------
def collect_input_with_loops():
    item_descriptions = []
    bin_descriptions = []

    try:
        num_items = int(input("How many item types do you have? "))
        for i in range(num_items):
            dims = input(f"  Enter dimensions for item #{i+1} (e.g., '5x3'): ")
            price = input(f"  Enter price for item #{i+1}: ")
            quantity = input(f"  Enter quantity for item #{i+1}: ")
            item_descriptions.append(
                f"item {i+1} is {dims} with a price of {price} and quantity of {quantity}."
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


# ----------------------------
# Main Workflow
# ----------------------------
if __name__ == "__main__":
    print("\n--- Starting Inventory Optimization Agent ---\n")

    user_input_text = collect_input_with_loops()

    if user_input_text:
        processor = InputProcessor()
        parsed_data = processor.parse_user_input_to_json(user_input_text)

        print("\n--- Parsed JSON Data ---")
        print(json.dumps(parsed_data, indent=2))
        print("------------------------\n")

        print("Data has been sent to the optimization engine...")

        # Dummy GA output for testing
        dummy_optimization_output = "{bin1=[1, 4, 6], bin2=[2, 5, 8], bin3=[3], bin4=[7]}"
        print(f"Optimization complete. Result: {dummy_optimization_output}\n")

        visualizer = VisualizationGenerator()
        markdown_table = visualizer.create_markdown_table(dummy_optimization_output)

        print("--- Generated Visualization ---")
        print(markdown_table)
        print("-------------------------------")

    print("\n--- Workflow Finished ---")
