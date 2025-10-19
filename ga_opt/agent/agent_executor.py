import os
import json
import google.generativeai as genai
from dotenv import load_dotenv

load_dotenv()
GOOGLE_API_KEY = os.getenv('GOOGLE_API_KEY')
if not GOOGLE_API_KEY:
    raise ValueError("Error: GOOGLE_API_KEY not found in .env file.")
genai.configure(api_key=GOOGLE_API_KEY)


class InputProcessor:
    def __init__(self):
        self.parser_model = genai.GenerativeModel('gemini-2.5-pro')

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
                        "required": ["number", "width", "height", "price", "quantity"]
                    }
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
                        "required": ["number", "width", "height"]
                    }
                }
            }
        }
        prompt = f"""
        Parse the following user input into a valid JSON object based on the provided schema.
        Assign a unique number (starting from 1) to each item type and bin.

        USER INPUT: "{user_text}"
        """
        response = self.parser_model.generate_content(
            prompt,
            generation_config=genai.types.GenerationConfig(
                response_mime_type="application/json",
                response_schema=json_schema
            )
        )
        return json.loads(response.text)


class VisualizationGenerator:
    def __init__(self):
        self.visualizer_model = genai.GenerativeModel('gemini-2.5-pro')

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
                 You are an AI visualization assistant.
                 Given the following bin-item allocation from a warehouse optimization GA:

                 {bins_data}

                 Generate a clean, simple Markdown-formatted table showing each bin and its assigned items.
                 Each row should represent one bin.
                 Label the columns as "Bin" and "Items".
                 Do not add any other text, just the Markdown table.
                 """
        
        response = self.visualizer_model.generate_content(prompt)
        return response.text


def collect_input_with_loops():
    item_descriptions = []
    bin_descriptions = []
    
    try:
        num_items = int(input("How many item types do you have? "))
        for i in range(num_items):
            dims = input(f"  Enter dimensions for item #{i+1} (e.g., '5x3'): ")
            price = input(f"  Enter price for item #{i+1}: ")
            quantity = input(f"  Enter quantity for item #{i+1}: ")
            item_descriptions.append(f"item {i+1} is {dims} with a price of {price} and quantity of {quantity}. ")
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
    print("--- Starting Input Processing Workflow ---")

    user_input_text = collect_input_with_loops()

    if user_input_text:
        processor = InputProcessor()
        parsed_data = processor.parse_user_input_to_json(user_input_text)
        
        print("\n--- Parsed JSON Data ---")
        print(json.dumps(parsed_data, indent=2))
        print("----------------------\n")

        print("Data has been sent to the optimization function...")

        dummy_optimization_output = "{bin1=[1, 4, 6], bin2=[2, 5, 8], bin3=[3], bin4=[7]}"
        print(f"Status: Optimization complete. Result: {dummy_optimization_output}\n")
        
        visualizer = VisualizationGenerator()
        markdown_table = visualizer.create_markdown_table(dummy_optimization_output)
        
        print("--- Generated Visualization ---")
        print(markdown_table)
        print("-----------------------------")

    print("\n--- Workflow Finished ---")