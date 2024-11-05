#!/bin/bash

config_file_path="/home/eva/datenlink/QseEvolvingKg/QseEvolvingKg/ConfigShaclDiffExtractor.properties"
input_properties_file_path="/home/eva/datenlink/evaluationRQ2BearB1.properties"

cat $input_properties_file_path > $config_file_path
echo "Copied $input_properties_file_path to $config_file_path"
  
cd /home/eva/datenlink/QseEvolvingKg/QseEvolvingKg|| { echo "Directory not found"; exit 1; }

sudo java -cp "target/classes:target/dependency/*" shacldiffextractor.ShaclDiffExtractor

source_folder="/home/eva/datenlink/QseEvolvingKg/QseEvolvingKg/Output"
destination_base="/home/eva/datenlink/evaluationResultsRQ2BearB1"
timestamp=$(date +%Y%m%d%H%M%S)
destination_folder="${destination_base}_${timestamp}"

# Copy the folder
cp -r "$source_folder" "$destination_folder"
sudo chmod -R 777 $destination_folder
sudo chmod -R 777 /home/eva/datenlink/QseEvolvingKg/

echo "Copied $source_folder to $destination_folder"