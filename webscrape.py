import json
import requests
from bs4 import BeautifulSoup

# Load extracted additives JSON file
with open(r"./extracted_additives.json") as f:
    additives_data = json.load(f)

# Function to scrape data from the website

def scrape_additive_info(e_number):
    url = f"https://food-detektiv.de/en/additives/?enummer={e_number}"
    response = requests.get(url)
    
    if response.status_code != 200:
        print(f"Failed to retrieve data for E-number: {e_number}")
        return None

    soup = BeautifulSoup(response.content, 'html.parser')
    sections = {
        "The risks": "",
        "Does it affect me?": "",
        "What is it anyway?": ""
    }
    
    for section in sections.keys():
        section_tag = soup.find('h5', string=section)
        if section_tag:
            sections[section] = section_tag.find_next('p').text
    
    return sections

# Example ingredient list
ingredient_list = "SULLALLAS I, DEMERARA SUGAR, FORTIFIED WHEAT FLOUR, CALCIUM CARBONATE, IRON, NIACIN B3, THIAMIN BI, RAISINS, CIDER, POTASSIUM METABISULPHITE, RUM, PALM OIL, COGNAC BRANDY, GLACE CHERRIES, GLUCOSE-FRUCTOSE SYRUP, ANTHOCYANINS, CITRIC ACID, SULPHUR DIOXIDE, CURRANTS, SINGLE CREAM MILK, WALNUTS 39, ALMONDS NUT, SHERRY, BRANDY, TREACLE, CANDIED CITRUS PEEL ORANGE, GLUCOSE SVRUP, LEMON PEEL, SUGAR, CITRIC ACID, BARLEY MALT EXTRACT, MIXED SPICES CORIANDER, CINNAMON, CLOVES, FENNEL, GINGER, NUTMEG, CARDAMOM, SALT, YEAST, LEMON OIL, ORANGE OIL"

# Split the ingredient list by comma
ingredients = [ingredient.strip() for ingredient in ingredient_list.split(',')]

# Process each ingredient
for ingredient in ingredients:
    matched = False
    for additive, details in additives_data.items():
        if additive == "synonyms":
            continue  # Skip the "synonyms" key at the root level

        synonyms = details.get("content", [])
        e_number = details["e_number"] if details["e_number"] else additive.split(",")[0].strip()

        if ingredient.lower() in [syn.lower() for syn in synonyms]:
            print(f"Exact match found for ingredient: {ingredient}")
            matched = True
            info = scrape_additive_info(e_number)
            if info:
                print(f"Information for {ingredient} (E-number: {e_number}):\n")
                for section, content in info.items():
                    print(f"{section}:\n{content}\n")
            break
    
    if not matched:
        print(f"No match found for ingredient: {ingredient}")

print("Processing complete.")
