import re
import pandas as pd
import Levenshtein

# Function to load dictionary from a CSV file
def load_dictionary_csv(file_path):
    df = pd.read_csv(file_path)
    dictionary = {re.sub(r'[;,.()]+', '', row['Ingredient'].lower()): row['Ingredient'] for index, row in df.iterrows()}
    return dictionary

# Function to correct text using approximate matching strategy, reporting corrections
def correct_text_with_approximate_matching(text, correction_dict):
    # Split text by spaces, keeping delimiters (e.g., commas, spaces)
    tokens = re.split('(\W+)', text)  # Split but keep delimiters
    corrected_tokens = []
    corrections = []
    
    for token in tokens:
        original_token = token
        token_clean = re.sub(r'[;,.()]+', '', token).lower()  # Clean token for processing
        if token_clean:  # Only process non-empty tokens
            best_match = token_clean
            min_distance = float('inf')
            for candidate in correction_dict.keys():
                if abs(len(candidate) - len(token_clean)) <= len(token_clean):  # Pruning condition
                    distance = Levenshtein.distance(token_clean, candidate)
                    if distance < min_distance and distance / len(candidate) < 0.5:  # Adjust the threshold as needed
                        min_distance = distance
                        best_match = correction_dict[candidate].upper()  # Convert best match to uppercase
            if token_clean != best_match.lower():
                corrections.append((original_token, best_match))
                token = re.sub(re.escape(token_clean), best_match, token, flags=re.IGNORECASE)
        corrected_tokens.append(token)

    # Print corrections for debug
    for original, corrected in corrections:
        print(f"Correcting '{original}' to '{corrected}'")
        
    return ''.join(corrected_tokens)

# Main execution setup
def main():
    # Hardcoded text input with punctuation
    hardcoded_text = "SULLALLAS I, DEMERARA SUGAR, FORTIFIED WHEAT FLOUR, CALCIUM CARBONATE, IRON, NIACIN B3, THIAMIN BI, RAISINS, CIDER, POTASSIUM METABISULPHITE, RUM, PALM OIL, COGNAC BRANDY, GLACE CHERRIES, GLUCOSE-FRUCTOSE SYRUP, ANTHOCYANINS, CITRIC ACID, SULPHUR DIOXIDE, CURRANTS, SINGLE CREAM MILK, WALNUTS 39, ALMONDS NUT, SHERRY, BRANDY, TREACLE, CANDIED CITRUS PEEL ORANGE, GLUCOSE SVRUP, LEMON PEEL, SUGAR, CITRIC ACID, BARLEY MALT EXTRACT, MIXED SPICES CORIANDER, CINNAMON, CLOVES, FENNEL, GINGER, NUTMEG, CARDAMOM, SALT, YEAST, LEMON OIL, ORANGE OIL"

    dict_path_csv = r'./final_merged_ingredients.csv'

    # Load dictionary and apply corrections
    correction_dict = load_dictionary_csv(dict_path_csv)
    corrected_text = correct_text_with_approximate_matching(hardcoded_text, correction_dict)
    print("Text after Correction:", corrected_text)

if __name__ == "__main__":
    main()
