import cv2
import easyocr
import re
import os
from collections import OrderedDict

# Initialize EasyOCR reader with GPU
reader = easyocr.Reader(['en'], gpu=True)

# Define the path to your image
image_path = r"./I0025.jpeg"

# Enhanced preprocess text function
def preprocess_text(text):
    # Replace ; with ,
    text = text.replace(';', ',')

    # Remove percentages
    text = re.sub(r'\d+%', '', text)

    # Replace brackets with a special token to preserve the text inside and remove brackets
    text = re.sub(r'\[([^\]]+)\]', r'\1', text)
    text = re.sub(r'\(([^\)]+)\)', r'\1', text)

    # Maintain text inside parentheses in commas
    text = re.sub(r',\s*\((.?)\)\s,', r', \1 ,', text)

    # Remove unwanted characters but keep allowed ones (including text within commas)
    text = re.sub(r'[^a-zA-Z0-9\s,&-]', '', text)

    # Remove multiple spaces and redundant commas
    text = re.sub(r'\s{2,}', ' ', text)
    text = re.sub(r',\s*,', ',', text)
    text = re.sub(r',\s*$', '', text)

    # Remove category words
    category_words = ['preservative', 'acidity regulator', 'colour', 'color', 'flavour', 'emulsifier', 'stabilizer', 'antioxidant']
    category_pattern = r'\b(?:' + '|'.join(category_words) + r')\b'
    text = re.sub(category_pattern, '', text, flags=re.IGNORECASE)

    # Remove multiple spaces again after removing category words
    text = re.sub(r'\s{2,}', ' ', text)
    text = re.sub(r',\s*,', ',', text)
    text = re.sub(r',\s*$', '', text)

    return text.strip()

# Function to remove duplicates within each comma-separated section
def remove_duplicates(text):
    sections = text.split(',')
    processed_sections = []
    for section in sections:
        words = section.split()
        filtered_words = ' '.join(OrderedDict.fromkeys(words))
        processed_sections.append(filtered_words)
    return ', '.join(processed_sections)

# Function to preprocess the image using Otsu's thresholding
def otsu_preprocessing(image_path):
    img = cv2.imread(image_path)
    if img is None:
        print(f"Failed to load image: {image_path}")
        return None
    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    _, otsu_thresh = cv2.threshold(gray, 0, 255, cv2.THRESH_BINARY | cv2.THRESH_OTSU)

    # Save the Otsu's thresholded image to verify the preprocessing
    cv2.imwrite('otsu_thresholded_image.png', otsu_thresh)

    # Display the Otsu's thresholded image (optional, for local environments)
    # Comment out or remove the following lines for headless environments
    # cv2.imshow('Otsu Thresholded Image', otsu_thresh)
    # cv2.waitKey(0)
    # cv2.destroyAllWindows()

    return otsu_thresh

# Load and preprocess the image
preprocessed_image = otsu_preprocessing(image_path)
if preprocessed_image is not None:
    print("Otsu's thresholding applied successfully.")

    # Perform OCR on the preprocessed image
    result = reader.readtext(preprocessed_image, detail=0, paragraph=True)
    ocr_output = ' '.join(result)
    print("Original OCR Output:", ocr_output)

    # Process OCR output
    processed_output = preprocess_text(ocr_output)
    # Remove duplicates from processed output
    processed_output = remove_duplicates(processed_output)
    # Ensure the final output is uppercase
    processed_output = processed_output.upper()
    print("Processed OCR Output:", processed_output)
else:
    print("Image preprocessing failed.")