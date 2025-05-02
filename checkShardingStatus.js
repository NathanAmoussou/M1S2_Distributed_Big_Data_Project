print("\n--- Vérification du sharding pour la base 'gestionBourse' ---\n");

const dbName = "gestionBourse";
const dbRef = db.getSiblingDB(dbName);
const maxRetries = 5; // Nombre d'essais avant d'abandonner
let attempts = 0;

function checkSharded() {
  try {
    const collections = dbRef.getCollectionInfos();

    if (!collections || collections.length === 0) {
      print(`X. Aucune collection trouvée dans la base '${dbName}'.`);
    } else {
      let shardedCount = 0;

      collections.forEach((collInfo) => {
        const collName = collInfo.name;
        const fullName = `${dbName}.${collName}`;
        const metadata = db
          .getSiblingDB("config")
          .collections.findOne({ _id: fullName });

        if (metadata && metadata.key) {
          print(`V. '${collName}' est SHARDÉE sur la clé : ${metadata.key}`);
          shardedCount++;
        } else {
          print(`X. '${collName}' n'est PAS shardée.`);
        }
      });

      print(
        `\nRésumé : ${shardedCount}/${collections.length} collections shardées.`
      );
    }
  } catch (e) {
    if (attempts < maxRetries) {
      attempts++;
      print(
        `X. Erreur lors de la vérification du sharding. Tentative ${attempts}/${maxRetries}...`
      );
      sleep(5000); // Attente avant de réessayer
      checkSharded(); // Réessayer
    } else {
      print("X. Erreur lors de la vérification du sharding : ", e);
    }
  }
}

checkSharded();
