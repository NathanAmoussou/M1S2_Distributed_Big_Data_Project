import json
import random
from datetime import datetime, timedelta

SRC  = "src/main/resources/InvestorsDatas2.json"
DEST = "src/main/resources/InvestorsDatasCompleted.json"

with open(SRC, "r", encoding="utf-8") as infile:
    investisseurs_data = json.load(infile)

investisseurs_corriges = []

for investisseur in investisseurs_data:
    # --- lastUpdateDate -----------------------------------------------------
    # 1. parse the existing creationDate (always present, ISO-8601 with 'Z')
    creation_dt = datetime.fromisoformat(
        investisseur["creationDate"].replace("Z", "+00:00")   # make it offset-aware
    )

    # 2. add 1-100 random days
    delta = timedelta(days=random.randint(1, 100))
    last_update_dt = creation_dt + delta

    # 3. store back as an ISO-8601 string (no {$date: …} wrapper)
    investisseur["lastUpdateDate"] = last_update_dt.strftime("%Y-%m-%dT%H:%M:%SZ")

    # --- addresses ----------------------------------------------------------
    adresse = {
        "number": "",
        "street": f"{random.randint(1, 999)} Rue Exemple",
        "zipCode": f"{random.randint(10000, 99999)}",
        "city": random.choice(["Paris", "Lyon", "Marseille", "Lille", "Bordeaux"]),
        "country": "FR"
    }

    # --- wallets ------------------------------------------------------------
    wallet = {
        "currencyCode": "USD",
        "balance": round(random.uniform(100.0, 5000.0), 2),
        "walletType": "default"
    }

    investisseur["addresses"] = [adresse]
    investisseur["wallets"]    = [wallet]

    investisseurs_corriges.append(investisseur)

with open(DEST, "w", encoding="utf-8") as outfile:
    json.dump(investisseurs_corriges, outfile, indent=2, ensure_ascii=False)

print(f"Fichier '{DEST}' créé avec succès.")
