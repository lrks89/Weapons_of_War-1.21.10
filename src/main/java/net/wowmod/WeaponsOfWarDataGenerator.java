// Make sure to rename the file to "WeaponsOfWarDataGenerator.java"
package net.wowmod;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

// Refinement 1: Class name changed to UpperCamelCase
public class WeaponsOfWarDataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        // This is where you will add your data providers
        // for things like models, blockstates, recipes,
        // and language files in the future.
    }
}