#!/bin/bash

config_file_path="/home/eva/datenlink/QseEvolvingKg/QseEvolvingKg/ConfigSparqlShapeChecker.properties"
dataSetNames1=("Bear-B1" "Bear-B10" "Bear-B20" "Bear-B30" "Bear-B40" "Bear-B50" "Bear-B60" "Bear-B70")
dataSetNames2=("Bear-B10" "Bear-B20" "Bear-B30" "Bear-B40" "Bear-B50" "Bear-B60" "Bear-B70" "Bear-B80")

for i in {0..7}; do
  sed -i "2s/.*/dataSetNameQSE=${dataSetNames1[$i]}/" "$config_file_path"
  sed -i "4s/.*/dataSetsToCheck=${dataSetNames2[$i]}/" "$config_file_path"
  
  cd /home/eva/datenlink/QseEvolvingKg/QseEvolvingKg|| { echo "Directory not found"; exit 1; }

  sudo java -cp "target/classes:target/dependency/*" sparqlshapechecker.SparqlShapeChecker
done

source_folder="/home/eva/datenlink/QseEvolvingKg/QseEvolvingKg/Output"
destination_base="/home/eva/datenlink/evaluationResultsBearBOneVersionEach10"
timestamp=$(date +%Y%m%d%H%M%S)
destination_folder="${destination_base}_${timestamp}"

# Copy the folder
cp -r "$source_folder" "$destination_folder"

echo "Copied $source_folder to $destination_folder"