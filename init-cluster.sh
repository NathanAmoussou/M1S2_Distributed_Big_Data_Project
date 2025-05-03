#!/bin/bash

echo "Initialisation du cluster MongoDB shardé..."

# --- Helper function ---
wait_for_mongo() {
  local host=$1
  local service_name=$(echo $host | cut -d: -f1) # Extract service name for better logging
  echo "Attente de MongoDB sur $service_name ($host)..."
  # Use a timeout to prevent infinite loops
  local max_wait=120 # Maximum wait time in seconds (adjust as needed)
  local current_wait=0
  until mongosh --host $host --quiet --eval "db.adminCommand('ping')" >/dev/null 2>&1; do
    if [ $current_wait -ge $max_wait ]; then
      echo "ERREUR: Timeout en attendant $service_name ($host)."
      exit 1
    fi
    sleep 2
    current_wait=$((current_wait + 2))
  done
  echo "$service_name ($host) est prêt."
}

# --- Helper function to wait for RS primary ---
wait_for_rs_primary() {
  local host=$1
  local rs_name=$2
  local service_name=$(echo $host | cut -d: -f1) # Extract service name
  echo "Attente de élection du primaire pour $rs_name via $service_name ($host)..."
  local max_wait=180 # Maximum wait time in seconds
  local current_wait=0
  # Loop until rs.status() shows a primary member (state 1)
  until mongosh --host $host --quiet --eval "rs.status().members.some(member => member.state === 1)" | grep -q "true"; do
     if [ $current_wait -ge $max_wait ]; then
      echo "ERREUR: Timeout en attendant le primaire pour $rs_name via $service_name ($host)."
      # Optional: Dump rs.status() for debugging
      echo "--- rs.status() on $host ---"
      mongosh --host $host --quiet --eval "rs.status()"
      echo "--------------------------"
      exit 1
    fi
    echo "  (Attente du primaire pour $rs_name... $current_wait/$max_wait s)"
    sleep 5
    current_wait=$((current_wait + 5))
  done
  echo "Primaire élu pour $rs_name (vérifié via $service_name)."
}


# --- Wait for individual config server nodes ---
wait_for_mongo config1:27017
wait_for_mongo config2:27017
wait_for_mongo config3:27017

# --- Init Config Replica Set ---
echo "Configuration du replica set des config servers (configReplSet)..."
# Use --eval instead of heredoc for potentially better error handling/output capture
mongosh --host config1:27017 --eval '
  try {
    rs.initiate({
      _id: "configReplSet",
      configsvr: true,
      members: [
        { _id: 0, host: "config1:27017" },
        { _id: 1, host: "config2:27017" },
        { _id: 2, host: "config3:27017" }
      ]
    });
    print("SUCCESS: configReplSet initiated.");
  } catch (e) {
    if (e.codeName == "AlreadyInitialized") {
      print("INFO: configReplSet already initialized.");
    } else {
      print("ERROR initiating configReplSet:");
      printjson(e);
      quit(1); // Exit script with error
    }
  }
' || exit 1 # Exit bash script if mongosh fails

# --- Wait for Config RS Primary ---
wait_for_rs_primary config1:27017 configReplSet


# --- Wait for individual shard server nodes ---
for shard in 1 2 3; do
  port=$((27017 + $shard)) # Calculate port (27018, 27019, 27020)
  wait_for_mongo shard${shard}a:${port}
  wait_for_mongo shard${shard}b:${port}
  wait_for_mongo shard${shard}c:${port}
done

# --- Init Shard Replica Sets ---
for shard in 1 2 3; do
  port=$((27017 + $shard)) # Calculate port
  rs_name="shard${shard}ReplSet"
  primary_host="shard${shard}a:${port}"

  echo "Initialisation de $rs_name..."
  mongosh --host $primary_host --eval '
    var rs_name = "'$rs_name'"; // Pass variables into the mongo shell script
    var port = '$port';
    var shard = '$shard';
    try {
      rs.initiate({
        _id: rs_name,
        members: [
          { _id: 0, host: "shard" + shard + "a:" + port },
          { _id: 1, host: "shard" + shard + "b:" + port },
          { _id: 2, host: "shard" + shard + "c:" + port }
        ]
      });
      print("SUCCESS: " + rs_name + " initiated.");
    } catch (e) {
      if (e.codeName == "AlreadyInitialized") {
        print("INFO: " + rs_name + " already initialized.");
      } else {
        print("ERROR initiating " + rs_name + ":");
        printjson(e);
        quit(1); // Exit script with error
      }
    }
  ' || exit 1 # Exit bash script if mongosh fails

  # --- Wait for Shard RS Primary ---
  wait_for_rs_primary $primary_host $rs_name

done

# --- Wait for mongos (Important: mongos depends on configReplSet being ready) ---
# At this point, configReplSet should have a primary, so mongos *should* be able to start correctly.
wait_for_mongo mongos:27017

# Add a small delay to allow mongos to fully connect internally after becoming pingable
echo "pause pour stabilisation de mongos..."
sleep 10

# --- Shard cluster setup ---
echo "Ajout des shards au cluster via mongos..."
# Use --eval for potentially better error checking
mongosh --host mongos:27017 --eval '
  print("Tentative ajout des shards...");

  // Function to check if a shard is already added
  function isShardAdded(shardName) {
    var status = sh.status();
    if (status && status.shards) {
      return status.shards.some(shard => shard._id === shardName);
    }
    return false;
  }

  // Add Shard 1
  if (!isShardAdded("shard1ReplSet")) {
    print("Ajout de shard1ReplSet...");
    var result1 = sh.addShard("shard1ReplSet/shard1a:27018,shard1b:27018,shard1c:27018");
    printjson(result1);
    if (!result1.ok) { print("ERREUR lors de ajout de shard1ReplSet"); quit(1); }
  } else {
    print("INFO: shard1ReplSet déjà ajouté.");
  }

  // Add Shard 2
  if (!isShardAdded("shard2ReplSet")) {
    print("Ajout de shard2ReplSet...");
    var result2 = sh.addShard("shard2ReplSet/shard2a:27019,shard2b:27019,shard2c:27019");
    printjson(result2);
     if (!result2.ok) { print("ERREUR lors de ajout de shard2ReplSet"); quit(1); }
  } else {
    print("INFO: shard2ReplSet déjà ajouté.");
  }

  // Add Shard 3
  if (!isShardAdded("shard3ReplSet")) {
    print("Ajout de shard3ReplSet...");
    var result3 = sh.addShard("shard3ReplSet/shard3a:27020,shard3b:27020,shard3c:27020");
    printjson(result3);
     if (!result3.ok) { print("ERREUR lors de ajout de shard3ReplSet"); quit(1); }
  } else {
    print("INFO: shard3ReplSet déjà ajouté.");
  }

  print("Vérification de état du sharding...");
  printjson(sh.status());

  // Enable Sharding on DB (idempotent check)
  var dbName = "gestionBourse";
  var databases = db.getSiblingDB("config").databases.find({_id: dbName}).toArray();
  if (databases.length > 0 && databases[0].partitioned) {
      print("INFO: Sharding déjà activé pour la base de données " + dbName);
  } else {
      print("Activation du sharding pour la base de données " + dbName + "...");
      var enableResult = sh.enableSharding(dbName);
      printjson(enableResult);
      if (!enableResult.ok && enableResult.codeName !== "DatabaseAlreadyEnabled") { print("ERREUR lors de activation du sharding pour " + dbName); quit(1); }
  }

  // Switch to the target database
  db = db.getSiblingDB(dbName);

  // Create collections if they dont exist (createCollection is idempotent)
  print("Création/Vérification des collections...");
  db.createCollection("investors");
  db.createCollection("holdings");
  db.createCollection("stockPriceHistory");
  db.createCollection("stocks");
  db.createCollection("transactions");
  print("Collections créées/vérifiées.");

  // Shard collections (check if already sharded)
  function shardCollectionIfNotSharded(collName, key) {
    var ns = dbName + "." + collName;
    var configDB = db.getSiblingDB("config");
    var collectionInfo = configDB.collections.findOne({ _id: ns });

    if (collectionInfo && collectionInfo.sharded) {
      print("INFO: La collection " + ns + " est déjà shardée.");
    } else {
      print("Sharding de la collection " + ns + " sur la clé " + JSON.stringify(key) + "...");
      var shardResult = sh.shardCollection(ns, key);
      printjson(shardResult);
      if (!shardResult.ok && shardResult.codeName !== "AlreadySharded") { print("ERREUR lors du sharding de " + ns); quit(1); }
    }
  }

  shardCollectionIfNotSharded("investors", { username: "hashed" });
  shardCollectionIfNotSharded("holdings", { walletId: 1, stockTicker: 1 });
  shardCollectionIfNotSharded("stockPriceHistory", { _id: "hashed" });
  shardCollectionIfNotSharded("stocks", { stockTicker: "hashed" });
  shardCollectionIfNotSharded("transactions", { _id: "hashed" });

  print("Configuration du sharding terminée.");
' || exit 1 # Exit bash script if mongosh fails

echo "Cluster MongoDB shardé prêt à emploi."
exit 0