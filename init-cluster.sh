#!/bin/bash

echo "Initialisation du cluster MongoDB shardé..."

# --- Helper function ---
wait_for_mongo() {
  echo "Attente de MongoDB sur $1..."
  until mongosh --host $1 --eval "db.adminCommand('ping')" >/dev/null 2>&1; do
    sleep 2
  done
  echo "$1 est prêt."
}

# --- Wait for config servers ---
wait_for_mongo config1:27017
wait_for_mongo config2:27017
wait_for_mongo config3:27017

# --- Init Config Replica Set ---
echo "Configuration du replica set des config servers..."
mongosh --host config1:27017 <<EOF
rs.initiate({
  _id: "configReplSet",
  configsvr: true,
  members: [
    { _id: 0, host: "config1:27017" },
    { _id: 1, host: "config2:27017" },
    { _id: 2, host: "config3:27017" }
  ]
});
EOF

# --- Wait for shard members ---
for shard in 1 2 3; do
  wait_for_mongo shard${shard}a:2701$((7 + $shard))
  wait_for_mongo shard${shard}b:2701$((7 + $shard))
  wait_for_mongo shard${shard}c:2701$((7 + $shard))
done

# --- Init Shard Replica Sets ---
for shard in 1 2 3; do
  echo "Initialisation de shard${shard}ReplSet..."
  mongosh --host shard${shard}a:2701$((7 + $shard)) <<EOF
rs.initiate({
  _id: "shard${shard}ReplSet",
  members: [
    { _id: 0, host: "shard${shard}a:2701$((7 + $shard))" },
    { _id: 1, host: "shard${shard}b:2701$((7 + $shard))" },
    { _id: 2, host: "shard${shard}c:2701$((7 + $shard))" }
  ]
});
EOF
done

# --- Wait for mongos ---
wait_for_mongo mongos:27017

# --- Shard cluster setup ---
echo "Ajout des shards et sharding des collections..."
mongosh --host mongos:27017 <<EOF
sh.addShard("shard1ReplSet/shard1a:27018,shard1b:27018,shard1c:27018");
sh.addShard("shard2ReplSet/shard2a:27019,shard2b:27019,shard2c:27019");
sh.addShard("shard3ReplSet/shard3a:27020,shard3b:27020,shard3c:27020");

sh.enableSharding("gestionBourse");

db = db.getSiblingDB("gestionBourse");
db.createCollection("investors");
db.createCollection("holdings");
db.createCollection("stockPriceHistory");
db.createCollection("stocks");
db.createCollection("transactions");

sh.shardCollection("gestionBourse.investors", { _id: "hashed" });
sh.shardCollection("gestionBourse.holdings", { _id: "hashed" });
sh.shardCollection("gestionBourse.stockPriceHistory", { _id: "hashed" });
sh.shardCollection("gestionBourse.stocks", { stockTicker: "hashed" });
sh.shardCollection("gestionBourse.transactions", { _id: "hashed" });
EOF

echo "Cluster MongoDB shardé prêt à l'emploi."
