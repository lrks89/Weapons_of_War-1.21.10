package net.wowmod.util;

import net.minecraft.client.model.ModelPart;
import java.lang.reflect.Field;

public class ModelPartUtils {
    private static Field xField;
    private static Field yField;
    private static Field zField;
    private static boolean initialized = false;

    public static void initialize(ModelPart rightArm) {
        if (initialized) return;

        try {
            Class<?> cls = rightArm.getClass();
            Field[] fields = cls.getDeclaredFields();

            // Smart Probe: Find fields matching Right Arm constants (-5, 2, 0)
            for (Field f : fields) {
                if (f.getType() == float.class) {
                    f.setAccessible(true);
                    float val = f.getFloat(rightArm);

                    if (val == -5.0F && xField == null) xField = f;
                    else if (val == 2.0F && yField == null) yField = f;
                }
            }

            // Infer Z based on X location
            if (xField != null && yField != null) {
                for (int i = 0; i < fields.length; i++) {
                    if (fields[i].equals(xField)) {
                        if (i + 2 < fields.length && fields[i+2].getType() == float.class) {
                            zField = fields[i+2];
                        }
                        break;
                    }
                }
            }
            if (xField != null) initialized = true;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setPivot(ModelPart part, float x, float y, float z) {
        if (!initialized) return;
        try {
            if (xField != null) xField.setFloat(part, x);
            if (yField != null) yField.setFloat(part, y);
            if (zField != null) zField.setFloat(part, z);
        } catch (Exception e) {}
    }

    // NEW: Getters to read current values
    public static float getX(ModelPart part) {
        try { return (xField != null) ? xField.getFloat(part) : 0.0F; } catch (Exception e) { return 0.0F; }
    }
    public static float getY(ModelPart part) {
        try { return (yField != null) ? yField.getFloat(part) : 0.0F; } catch (Exception e) { return 0.0F; }
    }
    public static float getZ(ModelPart part) {
        try { return (zField != null) ? zField.getFloat(part) : 0.0F; } catch (Exception e) { return 0.0F; }
    }
}