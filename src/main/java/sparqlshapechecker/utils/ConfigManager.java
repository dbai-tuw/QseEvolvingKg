package sparqlshapechecker.utils;

import sparqlshapechecker.SparqlShapeChecker;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

//Copied from QSE
public class ConfigManager {
    private static final Logger LOGGER = Logger.getLogger(SparqlShapeChecker.class.getName());

    public static String getProperty(String property, String fileName) {
        try {
            String configPath = System.getProperty("user.dir")+ File.separator + fileName;
            java.util.Properties prop = new java.util.Properties();
            FileInputStream configFile = new FileInputStream(configPath);
            prop.load(configFile);
            configFile.close();
            return prop.getProperty(property);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Exception occurred", ex);
            return null;
        }
    }

    public static List<String> getPropertiesGroupNames(String fileName) {
        var groupNames = new ArrayList<String>();
        try (BufferedReader reader = new BufferedReader(new FileReader(System.getProperty("user.dir")+ File.separator + fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                //skip comments
                if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                    continue;
                }

                //only take lines with key and value
                String[] keyValue = line.split("=", 2);
                if (keyValue.length == 2 && keyValue[0].contains(".")) {
                    var groupName = keyValue[0].split("\\.")[0];
                    if(!groupNames.contains(groupName))
                        groupNames.add(groupName);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Exception occurred", e);
            return new ArrayList<>();
        }
        return groupNames;
    }

    public static String getRelativeResourcesPathFromQse() {
        return new File(System.getProperty("user.dir"), "resources").getAbsolutePath();
    }
}
