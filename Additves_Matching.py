import pandas as pd
from fuzzywuzzy import process, fuzz
import re

def normalize_text(text):
    """ Normalize the text by lowering case and removing non-alphanumeric characters except commas and parentheses """
    text = text.lower()
    text = re.sub(r'[^a-z0-9,() ]', '', text)
    return text

def split_names(names):
    """ Split multiple names separated by commas and remove any content in parentheses """
    names = re.sub(r'\(.*?\)', '', names)  # Remove any parentheses and content within
    return [name.strip() for name in names.split(',') if name.strip()]

def find_matches(ingredient, additives_df):
    """ Find all matches for the ingredient in the additives dataframe using fuzzy matching """
    matches = []
    ingredient = normalize_text(ingredient)
    for idx, row in additives_df.iterrows():
        # Compare against all possible names
        for name in row['Normalized Names']:
            score = fuzz.WRatio(ingredient, name)
            if score >= 90:  # Collect all matches that meet the threshold
                matches.append((row, score))
    # Sort matches by score in descending order
    matches = sorted(matches, key=lambda x: x[1], reverse=True)
    return matches

def main(ingredient_text, path_to_csv):
    # Load Excel file
    print(path_to_csv)
    additives_df = pd.read_excel(path_to_csv)
    
    # Normalize and split names within the dataframe
    additives_df['Normalized Names'] = additives_df['Names'].apply(lambda x: split_names(normalize_text(x)))

    # Print column names to check them
    print("Additives DataFrame columns:", additives_df.columns)

    # Assume ingredient_text is a single string of ingredients, separate it if it's one string
    ingredients = split_names(normalize_text(ingredient_text))

    # Define the columns to display
    columns_to_display = ['E Code', 'Names', 'Origin', 'Function & characteristics', 'Daily intake', 'Side effects']

    # Find matches for each ingredient
    for ingredient in ingredients:
        matches = find_matches(ingredient, additives_df)
        if matches:
            print(f"Matches found for {ingredient}:")
            for match, score in matches:
                print(f"Match with score {score}:")
                print(match[columns_to_display].to_dict())
        else:
            print(f"No matches found for {ingredient}.")

if __name__ == "__main__":
    ingredient_text = "SULTANAS I, DEMERARA SUGAR, FORTIFIED WHEAT FLOUR, CALCIUM CARBONATE, IRON, NIACIN B3, THIAMIN BI, RAISINS, CIDER, POTASSIUM METABISULPHITE, RUM, PALM OIL, COGNAC BRANDY, GLAZE CHERRIES, GLUCOSE-FRUCTOSE SYRUP, ANTHOCYANINS, CITRIC ACID, SULPHUR DIOXIDE, CURRANTS, SINGLE CREAM MILK, WALNUTS 39, ALMONDS NUT, SHERRY, BRANDY, TREACLE, CANDIED CITRUS PEEL ORANGE, GLUCOSE SYRUP, LEMON PEEL, SUGAR, CITRIC ACID, BARLEY MALT EXTRACT, MIXED SPICES CORIANDER, CINNAMON, CLOVES, FENNEL, GINGER, NUTMEG, CARDAMOM, SALT, YEAST, LEMON OIL, ORANGE OIL"
    path_to_csv = r"G:/PythonCodes/PythonCodes/additves_complete.xlsx"

    main(ingredient_text, path_to_csv)
