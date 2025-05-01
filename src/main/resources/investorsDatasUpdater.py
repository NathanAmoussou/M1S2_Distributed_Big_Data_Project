import json
import random
# No need for datetime anymore as dates are removed before output

# --- Configuration ---
# Assumes your original JSON (with dates maybe) is InvestorsDatas2.json
# Assumes the final JSON (without dates, modified address/wallet) will be InvestorsDatasCompleted.json
# Both files expected in src/main/resources relative to project root
SRC_FILE_PATH  = "src/main/resources/InvestorsDatas2.json"
DEST_FILE_PATH = "src/main/resources/InvestorsDatasCompleted.json"

# --- Read Source JSON ---
try:
    with open(SRC_FILE_PATH, "r", encoding="utf-8") as infile:
        investisseurs_data = json.load(infile)
    print(f"Successfully loaded {len(investisseurs_data)} records from '{SRC_FILE_PATH}'.")
except FileNotFoundError:
    print(f"ERROR: Source file not found at '{SRC_FILE_PATH}'. Please create it first.")
    exit()
except json.JSONDecodeError as e:
    print(f"ERROR: Failed to decode JSON from '{SRC_FILE_PATH}'. Check format. Details: {e}")
    exit()
except Exception as e:
    print(f"ERROR: An unexpected error occurred while reading '{SRC_FILE_PATH}': {e}")
    exit()

# --- Process Data ---
investisseurs_corriges = []
for index, investisseur in enumerate(investisseurs_data):
    try:
        # --- Remove Dates (Let Java service handle creation/update times) ---
        investisseur.pop('creationDate', None) # Safely remove if exists
        investisseur.pop('lastUpdateDate', None) # Safely remove if exists

        # --- Addresses: Separate number and street ---
        street_number = str(random.randint(1, 999)) # Generate number
        adresse = {
            "number": street_number,                 # Put number here
            "street": "Rue Exemple",                 # Street name only
            "zipCode": f"{random.randint(10000, 99999)}",
            "city": random.choice(["Paris", "Lyon", "Marseille", "Lille", "Bordeaux"]),
            "country": "FR"                          # Keep country simple
        }
        # Replace or add the 'addresses' array with the new structure
        investisseur["addresses"] = [adresse]

        # --- Wallets: Generate default wallet (structure already correct) ---
        wallet = {
            "currencyCode": "USD",
            "balance": round(random.uniform(100.0, 5000.0), 2), # Numeric balance
            "walletType": "default"                              # Correct field name
        }
        # Replace or add the 'wallets' array
        investisseur["wallets"] = [wallet]

        # Add the fully modified investor to the results list
        investisseurs_corriges.append(investisseur)

    except KeyError as e:
        print(f"WARN: Missing expected key {e} in record {index+1}. Skipping this record.")
    except Exception as e:
        print(f"WARN: An unexpected error occurred processing record {index+1}. Skipping. Details: {e}")


# --- Write Destination JSON ---
try:
    with open(DEST_FILE_PATH, "w", encoding="utf-8") as outfile:
        # ensure_ascii=False helps if names/cities have accents
        json.dump(investisseurs_corriges, outfile, indent=2, ensure_ascii=False)
    print(f"Successfully created '{DEST_FILE_PATH}' with {len(investisseurs_corriges)} processed records.")
except IOError as e:
    print(f"ERROR: Could not write to destination file '{DEST_FILE_PATH}'. Details: {e}")
except Exception as e:
    print(f"ERROR: An unexpected error occurred while writing '{DEST_FILE_PATH}': {e}")
