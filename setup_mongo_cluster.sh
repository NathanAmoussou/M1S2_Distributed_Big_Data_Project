#!/bin/bash

# setup_mongo_cluster.sh (Final Automation Version)

echo "**********************************************"
echo "Waiting for MongoDB servers to be reachable..."
echo "**********************************************"

# Function to wait for TCP port open using nc (preferred) or mongosh (fallback)
wait_for_mongo_tcp() {
  local host=$1
  local port=$2
  local service_name=$3
  local max_retries=45 # Increased retries
  local retry_count=0
  echo "Waiting for $service_name ($host:$port)..."
  if ! command -v nc &> /dev/null; then
      echo "INFO: 'nc' not found, using 'mongosh' for reachability check..."
      while ! mongosh --host $host --port $port --eval "quit()" --quiet > /dev/null 2>&1; do
            retry_count=$((retry_count + 1))
            if [[ $retry_count -gt $max_retries ]]; then
              echo "ERROR: $service_name ($host:$port) did not become reachable via mongosh after $max_retries attempts."
              exit 1
            fi
            echo "Attempt $retry_count/$max_retries (mongosh): $service_name ($host:$port) not reachable yet. Retrying in 5 seconds..."
            sleep 5
      done
  else
      while ! nc -z $host $port > /dev/null 2>&1; do
        retry_count=$((retry_count + 1))
        if [[ $retry_count -gt $max_retries ]]; then
          echo "ERROR: $service_name ($host:$port) did not become reachable via TCP check after $max_retries attempts."
          exit 1
        fi
        echo "Attempt $retry_count/$max_retries (TCP): $service_name ($host:$port) not reachable yet. Retrying in 5 seconds..."
        sleep 5
      done
  fi
  echo "$service_name ($host:$port) is reachable."
}

# Function to wait for a replica set to have a PRIMARY
wait_for_primary() {
    local host=$1
    local port=$2
    local service_name=$3
    local max_retries=30 # Increased retries
    local retry_count=0
    echo "Waiting for PRIMARY on $service_name ($host:$port)..."
    while true; do
        # Check rs.status() for primary member existence
        if mongosh --host $host --port $port --quiet --eval "rs.status().members.some(m => m.stateStr === 'PRIMARY')" | grep -q "true"; then
            echo "PRIMARY found for $service_name ($host:$port)."
            break # Exit loop on success
        fi

        retry_count=$((retry_count + 1))
        if [[ $retry_count -gt $max_retries ]]; then
            echo "ERROR: PRIMARY node did not appear for $service_name ($host:$port) after $max_retries attempts."
            mongosh --host $host --port $port --quiet --eval "printjson(rs.status())"
            exit 1
        fi
        echo "Attempt $retry_count/$max_retries: PRIMARY not found for $service_name ($host:$port). Retrying in 6 seconds..."
        sleep 6
    done
}


# 1. Wait for individual server processes to start listening
wait_for_mongo_tcp mongo-configsvr 27017 "Config Server"
wait_for_mongo_tcp mongo-shard1 27017 "Shard 1"
wait_for_mongo_tcp mongo-shard2 27017 "Shard 2"
wait_for_mongo_tcp mongo-shard3 27017 "Shard 3"


# 2. Initiate Replica Sets
echo "**********************************************"
echo "Initiating Replica Sets..."
echo "**********************************************"
mongosh --host mongo-configsvr:27017 --eval ' try { rs.initiate({_id: "rsConfig", configsvr: true, members: [{_id: 0, host: "mongo-configsvr:27017"}]}); print("rsConfig initiate command sent."); } catch (e) { if (e.codeName === "AlreadyInitialized" || e.codeName === "NamespaceExists") { print("rsConfig already initialized."); } else { print("Error initiating rsConfig:", e); quit(1); } } ' || { echo "FATAL: Failed to execute rs.initiate on configsvr"; exit 1; }
sleep 2
mongosh --host mongo-shard1:27017 --eval ' try { rs.initiate({_id: "rsShard1", members: [{_id: 0, host: "mongo-shard1:27017"}]}); print("rsShard1 initiate command sent."); } catch (e) { if (e.codeName === "AlreadyInitialized" || e.codeName === "NamespaceExists") { print("rsShard1 already initialized."); } else { print("Error initiating rsShard1:", e); quit(1); } } ' || { echo "FATAL: Failed to execute rs.initiate on shard1"; exit 1; }
sleep 2
mongosh --host mongo-shard2:27017 --eval ' try { rs.initiate({_id: "rsShard2", members: [{_id: 0, host: "mongo-shard2:27017"}]}); print("rsShard2 initiate command sent."); } catch (e) { if (e.codeName === "AlreadyInitialized" || e.codeName === "NamespaceExists") { print("rsShard2 already initialized."); } else { print("Error initiating rsShard2:", e); quit(1); } } ' || { echo "FATAL: Failed to execute rs.initiate on shard2"; exit 1; }
sleep 2
mongosh --host mongo-shard3:27017 --eval ' try { rs.initiate({_id: "rsShard3", members: [{_id: 0, host: "mongo-shard3:27017"}]}); print("rsShard3 initiate command sent."); } catch (e) { if (e.codeName === "AlreadyInitialized" || e.codeName === "NamespaceExists") { print("rsShard3 already initialized."); } else { print("Error initiating rsShard3:", e); quit(1); } } ' || { echo "FATAL: Failed to execute rs.initiate on shard3"; exit 1; }


# 3. Wait for Primaries to be Elected
echo "**********************************************"
echo "Waiting for PRIMARY nodes to be elected..."
echo "**********************************************"
wait_for_primary mongo-configsvr 27017 "Config Server"
wait_for_primary mongo-shard1 27017 "Shard 1"
wait_for_primary mongo-shard2 27017 "Shard 2"
wait_for_primary mongo-shard3 27017 "Shard 3"


# 4. Wait for mongos router to start and become reachable
echo "**********************************************"
echo "Waiting for Mongos Router..."
echo "**********************************************"
wait_for_mongo_tcp mongo-mongos 27017 "Mongos Router"
sleep 10 # Extra sleep for mongos to stabilize after config primary is ready


# 5. Add Shards via Mongos
echo "**********************************************"
echo "Adding Shards to the Cluster via mongos..."
echo "**********************************************"
mongosh --host mongo-mongos:27017 --eval '
  print("Waiting a few seconds before adding shards...");
  sleep(5000);
  shards_to_add = [
    { shard: "rsShard1/mongo-shard1:27017", id: "rsShard1" },
    { shard: "rsShard2/mongo-shard2:27017", id: "rsShard2" },
    { shard: "rsShard3/mongo-shard3:27017", id: "rsShard3" }
  ];
  existing_shards = [];
  try { existing_shards = sh.status().shards.map(s => s._id); } catch(e) {print("Could not get initial shard status, proceeding...");}
  print("Existing shards:", existing_shards);
  shards_to_add.forEach(s => {
    if (!existing_shards.includes(s.id)) {
      print("Adding shard:", s.shard);
      try { sh.addShard(s.shard); print("Added shard:", s.shard); sleep(2000); } catch(e) { print("Error adding shard", s.shard, ":", e); }
    } else { print("Shard", s.id, "already exists."); }
  });
' || { echo "WARNING: Failed to execute addShard commands via mongos"; }


# 6. Enable Sharding for Database via Mongos
echo "**********************************************"
echo "Enabling Sharding for Database 'gestionBourse'..."
echo "**********************************************"
mongosh --host mongo-mongos:27017 --eval '
  db_name = "gestionBourse";
  db_info = null;
  try { db_info = db.getSiblingDB("config").databases.findOne({_id: db_name}); } catch(e) {print("Could not check db sharding status");}
  if (db_info && db_info.partitioned) {
    print("Sharding already enabled for database", db_name);
  } else {
    print("Attempting to enable sharding for database", db_name);
    try { sh.enableSharding(db_name); print("Successfully enabled sharding for", db_name); } catch (e) { if (e.codeName === "DatabaseAlreadySharded") { print("Sharding already enabled"); } else { print("Error enabling sharding:", e); } }
  }
' || { echo "WARNING: Failed to execute enableSharding command"; }


# 7. Shard Collections via Mongos
echo "******************************************************"
echo "Attempting to Shard Collections via mongos..."
echo "******************************************************"
mongosh --host mongo-mongos:27017 --eval '
  db_name = "gestionBourse";
  use(db_name);

  collections_to_shard = [
    { ns: db_name + ".transactions", key: { "_id": "hashed" } },
    { ns: db_name + ".holdings", key: { "walletId": 1 } },
    { ns: db_name + ".stockPriceHistory", key: { "_id": "hashed" } }
    // Add others if needed
  ];

  collections_to_shard.forEach(col => {
    print("Checking/Sharding collection:", col.ns);
    try {
      let stats = db.getCollection(col.ns.split(".")[1]).stats();
      if (stats && !stats.sharded) {
         print("Attempting sh.shardCollection for", col.ns);
         sh.shardCollection(col.ns, col.key);
         print("Successfully sharded", col.ns);
      } else if (stats && stats.sharded) {
         print(col.ns, "is already sharded.");
      } else {
         print("Collection", col.ns, "does not exist yet or stats unavailable, skipping.");
      }
    } catch (e) {
      if (e.codeName === "NamespaceNotFound") {
         print("Collection", col.ns, "does not exist yet, skipping.");
      } else if (e.codeName === "AlreadySharded") {
         print(col.ns, "is already sharded (caught exception).");
      } else {
         print("Error attempting to shard collection", col.ns, ":", e);
      }
    }
    sleep(1000);
  });

  print("\nVerifying status...");
  sh.status();

' || { echo "WARNING: Failed during collection sharding attempts"; }


echo "**********************************************"
echo "Cluster Setup Script Completed."
echo "**********************************************"