from datetime import datetime

import yfinance as yf
from flask import Flask, jsonify, request

app = Flask(__name__)


# obtenir les données de l'action
def get_stock_data(stock_ticker, market, start_date, end_date):
    # combinaison du ticker et du marché
    if market:
        symbol = f"{stock_ticker}.{market}"
    else:
        symbol = stock_ticker

    print(f"Récupération des données pour {symbol} du {start_date} au {end_date}")

    # Vérification si le symbole existe en essayant de récupérer les données de l'action
    try:
        stock = yf.Ticker(symbol)
        if not stock:
            raise ValueError(
                f"\nAction {symbol}.{market} introuvable ou données non disponibles..."
                + "\nVérifier que symbole existe sur le marché donné."
            )
        # Vérification si nous pouvons obtenir les informations de l'action (par exemple, vérifier si 'info' n'est pas vide)
        stock_info = stock.info
        print("stock_info dans get_stock_data", stock_info)
        if not stock_info or "symbol" not in stock_info:
            raise ValueError(
                f"\nAction {symbol}.{market} introuvable ou données non disponibles..."
                + "\nVérifier que symbole existe sur le marché donné."
            )

        # récup les données historiques depuis yahoo finance
        history = stock.history(start=start_date, end=end_date)

        if history.empty:
            print(
                f"Aucune donnée historique trouvée pour {symbol} dans la période spécifiée."
            )
            raise ValueError(
                f"Aucune donnée historique trouvée pour {symbol} dans la période spécifiée."
            )

        # convertir les données JSON pour retourner
        data = history.reset_index().to_dict(orient="records")
        return data

    except Exception as e:
        raise ValueError(
            f"Erreur lors de la récupération des données pour {symbol}.{market}: {str(e)}"
        )


# Fonction pour obtenir les informations de l'action (nom, secteur, ticker, etc.)
def get_stock_info(stock_ticker, market):
    print("\nIN get_stock_info")
    if market:
        symbol = f"{stock_ticker}.{market}"
    else:
        symbol = stock_ticker

    try:
        try:
            stock = yf.Ticker(symbol)
            if not stock:
                raise ValueError(
                    f"\nAction {symbol}.{market} introuvable ou données non disponibles..."
                    + "\nVérifier que symbole existe sur le marché donné."
                )
            stock_info = stock.info
            print("stock_info dans get_stock_info", stock_info)
        except Exception:
            raise ValueError(
                f"\nAction {symbol}.{market} introuvable ou données non disponibles..."
                + "\nVérifier que symbole existe sur le marché donné."
            )

        # Vérif  si stock_info est un dictionnaire valide
        if (
            not stock_info
            or not isinstance(stock_info, dict)
            or "symbol" not in stock_info
        ):
            raise ValueError(
                f"\nAction {symbol}.{market} introuvable ou données non disponibles..."
                + "\nVérifier que symbole existe sur le marché donné."
            )

        # Extraction des informations pertinentes sur l'action à retourner
        info = {
            "stock_name": stock_info.get("longName", "N/A"),
            "stock_ticker": stock_info.get("symbol", "N/A"),
            "market": stock_info.get("exchange", market),
            "sector": stock_info.get("sector", "N/A"),
            "industry": stock_info.get("industry", "N/A"),
            "country": stock_info.get("country", "N/A"),
            "currency": stock_info.get("currency", "N/A"),
            "regular_market_price": stock_info.get("regularMarketPrice", "N/A"),
            "regular_market_day_high": stock_info.get("regularMarketDayHigh", "N/A"),
            "regular_market_day_low": stock_info.get("regularMarketDayLow", "N/A"),
        }

        return info

    except Exception as e:
        raise ValueError(
            f"Erreur lors de la récupération des informations de l'action pour {symbol}.{market}: {str(e)}"
        )


# Route pour récupérer les données de l'action
# exemple : /get_stock_data?stock_ticker=AIR&market=PA&start_date=2023-01-01&end_date=2023-12-31
#           /get_stock_data?stock_ticker=AAPL&start_date=2023-01-01&end_date=2023-12-31
@app.route("/get_stock_data", methods=["GET"])
def get_stock_data_api():
    # Récupérer les paramètres de l requête
    stock_ticker = request.args.get("stock_ticker")
    market = request.args.get("market")
    start_date = request.args.get("start_date")  # Format YYYY-MM-DD
    end_date = request.args.get("end_date")  # Format YYYY-MM-DD

    try:
        start_date = datetime.strptime(start_date, "%Y-%m-%d")
        end_date = datetime.strptime(end_date, "%Y-%m-%d")
    except ValueError:
        return jsonify({"error": "Format de date invalide. Utilisez YYYY-MM-DD."}), 400

    # get action data
    try:
        if start_date >= end_date:
            raise ValueError("La date de début doit être avant la date de fin.")
        if start_date > datetime.now() or end_date > datetime.now():
            raise ValueError("Les dates doivent être dans le passé.")
        if (end_date - start_date).days > 365:
            raise ValueError("La plage de dates ne doit pas dépasser 365 jours.")
        if (end_date - start_date).days < 1:
            raise ValueError("La plage de dates doit être d'au moins 1 jour.")
        if not stock_ticker:
            raise ValueError("Le ticker de l'action est requis.")
        if not market:
            # Si pas marché on utilise le ticker uniquement
            data = get_stock_data(stock_ticker, None, start_date, end_date)
        else:
            data = get_stock_data(stock_ticker, market, start_date, end_date)
        return jsonify(data)
    except ValueError as e:
        return jsonify({"error": str(e)}), 400


# Route pour récupérer les informations de l'action
# exemple : /get_stock_info?stock_ticker=AAPL
#           /get_stock_info?stock_ticker=IBM
#           /get_stock_info?stock_ticker=BP&market=LSE
@app.route("/get_stock_info", methods=["GET"])
def get_stock_info_api():
    # Récupérer les paramètres depuis la requête
    stock_ticker = request.args.get("stock_ticker")
    market = request.args.get("market")

    # Récupérer les informations de l'action
    try:
        stock_info = get_stock_info(stock_ticker, market)
        return jsonify(stock_info)
    except ValueError as e:
        return jsonify({"error": str(e)}), 400


if __name__ == "__main__":
    app.run(debug=True, host="0.0.0.0", port=5000)
