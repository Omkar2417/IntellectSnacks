from transformers import AutoModelForTokenClassification, AutoTokenizer, pipeline

# Use the model name from Hugging Face Hub
model_name = "dslim/bert-base-NER"  # popular pretrained NER model on HF hub

# Load tokenizer and model directly from Hugging Face online (no local_files_only=True)
tokenizer = AutoTokenizer.from_pretrained(model_name)
model = AutoModelForTokenClassification.from_pretrained(model_name)

# Initialize the NER pipeline with simple aggregation strategy
ner_pipeline = pipeline("ner", model=model, tokenizer=tokenizer, aggregation_strategy="simple")

extracted_texts = (
    "CHRISTMAS PUDDING WITH VINE FRUITS, GLACE CHERRIES, ALMONDS, WALNUTS, COGNAC, SHERRY AND RUM "
    "INGREDIENTS SULLALLAS I, DEMERARA SUGAR, FORTIFIED WHEAT FLOUR, CALCIUM CARBONATE, IRON, NIACIN B3, "
    "THIAMIN BI, RAISINS, CIDER, POTASSIUM METABISULPHITE, RUM, PALM OIL, COGNAC BRANDY, GLACE CHERRIES, "
    "GLUCOSE-FRUCTOSE SYRUP, ANTHOCYANINS, CITRIC ACID, SULPHUR DIOXIDE, CURRANTS, SINGLE CREAM MILK, "
    "WALNUTS 39, ALMONDS NUT, SHERRY, BRANDY, TREACLE, CANDIED CITRUS PEEL ORANGE, GLUCOSE SVRUP, LEMON PEEL, "
    "SUGAR, CITRIC ACID, BARLEY MALT EXTRACT, MIXED SPICES CORIANDER, CINNAMON, CLOVES, FENNEL, GINGER, "
    "NUTMEG, CARDAMOM, SALT, YEAST, LEMON OIL, ORANGE OIL ALLERGV ADVICEL FOR ALLERGENS, INCLUDING CEREALS "
    "CONTAINING GLUTEN, SEE INGREDIENTS IN BOLD NOT SUITABLE FOR SOVA ALLERGY SUFFERERS MAY ALSO CONTAIN "
    "TRACES OF OTHER NUTS NO ARTIFICIAL COLOURS, FLAVOURS OR HYDROGENATED FAT"
)

# Run NER
entities = ner_pipeline(extracted_texts)

# Concatenate adjacent or overlapping entities (fixed slicing)
concatenated_entities = []
if entities:
    current_entity = entities[0]
    for next_entity in entities[1:]:
        if next_entity['start'] <= current_entity['end'] + 1:
            current_entity['end'] = max(current_entity['end'], next_entity['end'])
        else:
            concatenated_entities.append(extracted_texts[current_entity['start']:current_entity['end'] + 1])
            current_entity = next_entity
    concatenated_entities.append(extracted_texts[current_entity['start']:current_entity['end'] + 1])

print("Extracted Entities:")
for entity in concatenated_entities:
    print("-", entity)
