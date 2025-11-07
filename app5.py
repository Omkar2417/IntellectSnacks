import os
os.environ["KMP_DUPLICATE_LIB_OK"] = "TRUE"
import re
import pandas as pd
import Levenshtein
import json
import pprint
import requests
from bs4 import BeautifulSoup
import numpy as np
from flask import Flask, request, jsonify
import cv2
import easyocr
import pandas as pd
from fuzzywuzzy import process, fuzz
import re
import re
from collections import OrderedDict
import mysql.connector
from difflib import SequenceMatcher
from pprint import pprint
from transformers import pipeline
from transformers import AutoModelForTokenClassification, AutoTokenizer, pipeline

# Use the model name from Hugging Face Hub
model_name = "dslim/bert-base-NER"  # popular pretrained NER model on HF hub

# Load tokenizer and model directly from Hugging Face online (no local_files_only=True)
tokenizer = AutoTokenizer.from_pretrained(model_name)
model = AutoModelForTokenClassification.from_pretrained(model_name)

db_config = {
    'host': '135.125.65.213',            # e.g., 'sg2nlmysql3plsk.secureserver.net'
    'user': 'wastefood',        # e.g., 'snehaldbuser'
    'password': 'waste@12345',
    'database': 'llqzqipe_wastefood'     # e.g., 'snehaldb'
}

app = Flask(__name__)

UPLOAD_FOLDER = 'uploads'
os.makedirs(UPLOAD_FOLDER, exist_ok=True)

# Initialize EasyOCR reader once (use gpu=True if you have GPU)
reader = easyocr.Reader(['en'], gpu=True)

EXCEL_PATH = r"D:/PythonCodes/PythonCodes/additves_complete.xlsx"
additives_df = pd.read_excel(EXCEL_PATH)
additives_df['Normalized Names'] = additives_df['Names'].apply(lambda x: [name.strip() for name in re.sub(r'\(.*?\)', '', x.lower()).split(',') if name.strip()])

# Load extracted additives JSON file once
with open(r"D:/PythonCodes/PythonCodes/extracted_additives.json") as f:
    additives_data = json.load(f)
def normalize_text(text):
    return re.sub(r'[^a-z0-9,() ]', '', text.lower())

# Split string into ingredients
def split_names(text):
    return [item.strip() for item in text.split(',') if item.strip()]

# 🔍 Fuzzy match function (case-insensitive)

# Scrape additive info from the food-detektiv site
def scrape_additive_info(e_number):
    url = f"https://food-detektiv.de/en/additives/?enummer={e_number}"
    headers = {
        "User-Agent": "Mozilla/5.0"
    }
    response = requests.get(url, headers=headers)

    if response.status_code != 200:
        print(f"Failed to retrieve data for E-number: {e_number}")
        return None

    soup = BeautifulSoup(response.content, 'html.parser')

    sections = {
        "The risks": "",
        "Does it affect me?": "",
        "What is it anyway?": ""
    }

    for header in soup.find_all(['h4', 'h5']):
        title = header.get_text(strip=True)
        if title in sections:
            next_elem = header.find_next(['p', 'div'])
            if next_elem:
                sections[title] = next_elem.get_text(strip=True)

    return sections
# Route to check ingredients
@app.route('/check_ingredients', methods=['POST'])
def check_ingredients():
    data = request.get_json()
    ingredient_list = data.get("ingredients", "")
    ingredients = [i.strip() for i in ingredient_list.split(',')]

    results = []

    for ingredient in ingredients:
        matched = False
        for additive, details in additives_data.items():
            if additive == "synonyms":
                continue

            synonyms = details.get("content", [])
            e_number = details.get("e_number", "") or additive.split(",")[0].strip()

            if ingredient.lower() in [s.lower() for s in synonyms]:
                matched = True
                info = scrape_additive_info(e_number)
                affect_info = info.get("Does it affect me?", "No info found.") if info else "No information available."
                results.append({
                    "ingredient": ingredient,
                    "matched": True,
                    "e_number": e_number,
                    "does_it_affect_me": affect_info
                })
                break
                print(affect_info)
        if not matched:
            results.append({
                "ingredient": ingredient,
                "matched": False,
                "does_it_affect_me": "No match found in database"
            })
    print(results)
    return jsonify(results)
# 🔍 Fuzzy match function (case-insensitive, lower threshold)
def find_matches(ingredient, df):
    matches = []
    ingredient_lower = ingredient.lower()
    for _, row in df.iterrows():
        normalized_names = row.get('Normalized Names', [])
        if not isinstance(normalized_names, list):
            continue

        for synonym in normalized_names:
            score = SequenceMatcher(None, ingredient_lower, synonym.lower()).ratio()
            if score >= 0.85:  # LOWERED THRESHOLD
                matches.append((row, score))
                break  # Only consider first match per row
    return matches

@app.route("/match_ingredients", methods=["POST"])
def match_ingredients():
    try:
        data = request.get_json()

        # ✅ Expect list of {"ingredient": "..."} inside the "ingredients" key
        ingredient_list = data.get("ingredients")

        if not ingredient_list or not isinstance(ingredient_list, list):
            return jsonify({"error": "Missing or invalid ingredient list"}), 400

        # Extract and normalize ingredient text
        ingredients_raw_text = ", ".join(item['ingredient'] for item in ingredient_list if 'ingredient' in item)

        print("📦 Raw Ingredients:", ingredients_raw_text)
        normalized = normalize_text(ingredients_raw_text)
        print("🔧 Normalized:", normalized)

        ingredients = split_names(normalized)
        print("🧪 Ingredients:", ingredients)

        results = []
        columns_to_display = ['E Code', 'Names', 'Origin', 'Function & characteristics', 'Daily intake', 'Side effects']

        for ingredient in ingredients:
            print(f"🔍 Checking: {ingredient}")
            matches = find_matches(ingredient, additives_df)

            harmful_matches = []
            for match, score in matches:
                side_effects = str(match.get('Side effects', '')).lower()
                if any(word in side_effects for word in ['harmful', 'cause', 'risk', 'danger', 'unsafe', 'toxic']):
                    harmful_matches.append({
                        "score": round(score, 2),
                        "details": match[columns_to_display].to_dict()
                    })

            if harmful_matches:
                results.append({
                    "ingredient": ingredient,
                    "matches": harmful_matches
                })

        print("✅ Final Output:")
        pprint(results)
        return jsonify({"results": results})  # ✅ wrap in "results" for Android parsing

    except Exception as e:
        return jsonify({"error": str(e)}), 500



def preprocess_text(text):
    text = text.replace(';', ',')
    text = re.sub(r'\d+%', '', text)
    text = re.sub(r'\[([^\]]+)\]', r'\1', text)
    text = re.sub(r'\(([^\)]+)\)', r'\1', text)
    text = re.sub(r',\s*\((.?)\)\s,', r', \1 ,', text)
    text = re.sub(r'[^a-zA-Z0-9\s,&-]', '', text)
    text = re.sub(r'\s{2,}', ' ', text)
    text = re.sub(r',\s*,', ',', text)
    text = re.sub(r',\s*$', '', text)
    category_words = ['preservative', 'acidity regulator', 'colour', 'color', 'flavour', 'emulsifier', 'stabilizer', 'antioxidant']
    category_pattern = r'\b(?:' + '|'.join(category_words) + r')\b'
    text = re.sub(category_pattern, '', text, flags=re.IGNORECASE)
    text = re.sub(r'\s{2,}', ' ', text)
    text = re.sub(r',\s*,', ',', text)
    text = re.sub(r',\s*$', '', text)
    return text.strip()

def remove_duplicates(text):
    sections = text.split(',')
    processed_sections = []
    for section in sections:
        words = section.split()
        filtered_words = ' '.join(OrderedDict.fromkeys(words))
        processed_sections.append(filtered_words)
    return ', '.join(processed_sections)
# --- Load CSV dictionary ---
def load_dictionary_csv(file_path):
    df = pd.read_csv(file_path)
    return {re.sub(r'[;,.()]+', '', row['Ingredient'].lower()): row['Ingredient'] for _, row in df.iterrows()}

@app.route('/correct_ingredients_db', methods=['GET'])
def correct_ingredients_from_db():
    try:
        # Connect to DB and fetch ingredient text
        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor()
        cursor.execute("SELECT result FROM ocr_result ORDER BY id DESC LIMIT 1")
        row = cursor.fetchone()
        cursor.close()
        conn.close()

        if not row:
            return jsonify({'error': 'Record not found'}), 404

        ingredient_text = row[0]

        # Load dictionary and correct text
        correction_dict = load_dictionary_csv(r'D:/PythonCodes/PythonCodes/final_merged_ingredients.csv')
        corrected_text, corrections = correct_text_with_approximate_matching(ingredient_text, correction_dict)
        pprint(ingredient_text);
        pprint("✅ Final Corrected:")
        pprint(corrected_text);
        
        return jsonify({
            
            'ingredients': corrected_text,
            
        })
    
       
    except Exception as e:
        return jsonify({'error': str(e)}), 500
# --- Correction logic ---
def correct_text_with_approximate_matching(text, correction_dict):
    tokens = re.split('(\W+)', text)
    
    corrected_tokens = []
    corrections = []

    for token in tokens:
        original_token = token
        token_clean = re.sub(r'[;,.()]+', '', token).lower()
        if token_clean:
            best_match = token_clean
            min_distance = float('inf')
            for candidate in correction_dict.keys():
                if abs(len(candidate) - len(token_clean)) <= len(token_clean):
                    distance = Levenshtein.distance(token_clean, candidate)
                    if distance < min_distance and distance / len(candidate) < 0.5:
                        min_distance = distance
                        best_match = correction_dict[candidate].upper()
            if token_clean != best_match.lower():
                corrections.append((original_token, best_match))
                token = re.sub(re.escape(token_clean), best_match, token, flags=re.IGNORECASE)
        corrected_tokens.append(token)

    return ''.join(corrected_tokens), corrections

def otsu_preprocessing(filepath):
    img = cv2.imread(filepath)
    if img is None:
        print(f"Failed to read image from {filepath}")
        return None
    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    
    # Try inverted thresholding if text is dark on light background
    _, otsu_thresh = cv2.threshold(gray, 0, 255, cv2.THRESH_BINARY_INV | cv2.THRESH_OTSU)

    # Convert to 3-channel
    processed_img = cv2.cvtColor(otsu_thresh, cv2.COLOR_GRAY2RGB)

    # Save for debugging
    cv2.imwrite("debug_preprocessed.jpg", processed_img)

    return processed_img


ner_pipeline = pipeline("ner", model="dslim/bert-base-NER", aggregation_strategy="simple")

@app.route('/ner', methods=['GET'])
def ner_from_latest_ocr():
    try:
        # Connect to MySQL and fetch the latest OCR result
        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor()
        cursor.execute("SELECT result FROM ocr_result ORDER BY id DESC LIMIT 1")
        row = cursor.fetchone()
        cursor.close()
        conn.close()

        if not row:
            return jsonify({'error': 'No OCR result found in the database'}), 404

        extracted_texts = row[0]  # Latest OCR text

        # Run NER pipeline on the extracted text
        entities = ner_pipeline(extracted_texts)

        # Post-process: merge nearby entities (simple concatenation for overlapping/adjacent)
        concatenated_entities = []
        current_entity = None
        for entity in entities:
            if current_entity and not extracted_texts[current_entity["end"]:entity["start"]].strip():
                current_entity["end"] = entity["end"]
            else:
                if current_entity:
                    concatenated_entities.append(
                        extracted_texts[current_entity["start"]:current_entity["end"]]
                    )
                current_entity = entity

        if current_entity:
            concatenated_entities.append(
                extracted_texts[current_entity["start"]:current_entity["end"]]
            )

        return jsonify({
            'latest_ocr_result': extracted_texts,
            'entities': concatenated_entities
        })

    except mysql.connector.Error as db_err:
        return jsonify({'error': f'MySQL error: {db_err}'}), 500
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/upload', methods=['POST'])
def upload_file():
    print("Upload endpoint called")

    if 'file' not in request.files:
        return jsonify({'error': 'No file part'}), 400

    file = request.files['file']
    if file.filename == '':
        return jsonify({'error': 'No selected file'}), 400

    filepath = os.path.join(UPLOAD_FOLDER, file.filename)
    file.save(filepath)
    print(f"Saved file to {filepath}")

    preprocessed_image = otsu_preprocessing(filepath)
    if preprocessed_image is None:
        return jsonify({'error': 'Image preprocessing failed'}), 500

    # Ensure correct format
    preprocessed_image = preprocessed_image.astype(np.uint8)
    preprocessed_image = cv2.cvtColor(preprocessed_image, cv2.COLOR_BGR2RGB)

    # Try OCR
    try:
        result = reader.readtext(preprocessed_image, detail=0, paragraph=False)
        if not result:
            # Retry with filepath
            print("Empty result from image array, retrying with filepath")
            result = reader.readtext(filepath, detail=0, paragraph=False)
    except Exception as e:
        print(f"OCR error: {e}")
        return jsonify({'error': f'OCR processing failed: {str(e)}'}), 500

    ocr_output = ' '.join(result)
    print(f"OCR output: {ocr_output}")

    processed_output = preprocess_text(ocr_output)
    processed_output = remove_duplicates(processed_output).upper()
    print(f"Processed OCR output: {processed_output}")

    # Save to MySQL
    try:
        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor()
        cursor.execute("DELETE FROM ocr_result")
        print("Deleted Success")
        cursor.execute("INSERT INTO ocr_result (filename, result) VALUES (%s, %s)", (file.filename, processed_output))
        conn.commit()
        cursor.close()
        conn.close()
    except mysql.connector.Error as err:
        return jsonify({'error': f'MySQL error: {err}'}), 500

    return jsonify({
        'message': 'File uploaded and processed successfully',
        'filename': file.filename,
        'ocr_result': processed_output
    })
if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
