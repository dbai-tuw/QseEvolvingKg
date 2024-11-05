#!/bin/bash

config_file_path="/home/eva/datenlink/QseEvolvingKg/QseEvolvingKg/ConfigSparqlShapeChecker.properties"
dataSetNames1=("Bear-B1" "Bear-B2" "Bear-B3" "Bear-B4" "Bear-B5" "Bear-B6")
dataSetNames2=("Bear-B2" "Bear-B3" "Bear-B4" "Bear-B5" "Bear-B6" "Bear-B7")

for i in {0..5}; do
  sed -i "2s/.*/dataSetNameQSE=${dataSetNames1[$i]}/" "$config_file_path"
  sed -i "4s/.*/dataSetsToCheck=${dataSetNames2[$i]}/" "$config_file_path"
  
  cd /home/eva/datenlink/QseEvolvingKg/QseEvolvingKg|| { echo "Directory not found"; exit 1; }

  sudo java -cp "target/classes:target/dependency/*" sparqlshapechecker.SparqlShapeChecker
done

source_folder="/home/eva/datenlink/QseEvolvingKg/QseEvolvingKg/Output"
destination_base="/home/eva/datenlink/evaluationResultsBearBOneVersionEach"
timestamp=$(date +%Y%m%d%H%M%S)
destination_folder="${destination_base}_${timestamp}"

# Copy the folder
cp -r "$source_folder" "$destination_folder"

echo "Copied $source_folder to $destination_folder"