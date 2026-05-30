package dev.lost.furnace.utils;

public class FastMath {

    // π and τ
    public static final float PI = (float) Math.PI;
    public static final float PI_2 = PI / 2;
    public static final float TAU = PI * 2F;

    // Sin/Cos
    private static final int SIN_SIZE = 65536;
    private static final int SIN_MASK = SIN_SIZE - 1;
    private static final float SIN_FACTOR = SIN_SIZE / TAU;
    private static final float[] SIN = new float[SIN_SIZE + 1];

    static {
        for (int i = 0; i < SIN_SIZE; i++) {
            SIN[i] = (float) Math.sin(i * TAU / SIN_SIZE);
        }
    }

    // Atan2
    private static final int ATAN2_SIZE = 1024;
    private static final float[] ATAN2 = new float[ATAN2_SIZE + 1];

    static {
        for (int i = 0; i <= ATAN2_SIZE; i++) {
            double d = (double) i / ATAN2_SIZE;
            double x = 1;
            double y = x * d;
            ATAN2[i] = (float) Math.atan2(y, x);
        }
    }

    /**
     * Returns an approximation of the sine of the given angle.
     *
     * @param value the angle in radians
     * @return an approximation of {@code sin(value)}
     */
    public static float sin(float value) {
        return SIN[FastMath.floor(value * SIN_FACTOR) & SIN_MASK];
    }

    /**
     * Returns an approximation of the cosine of the given angle.
     *
     * @param value the angle in radians
     * @return an approximation of {@code cos(value)}
     */
    public static float cos(float value) {
        return SIN[FastMath.floor(value * SIN_FACTOR + 16384) & SIN_MASK];
    }

    public static double atan2(float y, float x) {
        if (x == 0f) {
            if (y > 0f) return PI_2;
            if (y < 0f) return -PI_2;
            return 0f;
        }

        if (y == 0f) {
            return x < 0f ? PI : 0f;
        }

        float absX = x < 0f ? -x : x;
        float absY = y < 0f ? -y : y;

        if (absY > absX) {
            float ratio = absX / absY;
            int index = (int) (ratio * ATAN2_SIZE);
            if (index > ATAN2_SIZE) index = ATAN2_SIZE;

            if (x < 0f) {
                return y < 0f ? -PI_2 - ATAN2[index] : PI_2 + ATAN2[index];
            } else {
                return y < 0f ? -PI_2 + ATAN2[index] : PI_2 - ATAN2[index];
            }
        } else {
            float ratio = absY / absX;
            int index = (int) (ratio * ATAN2_SIZE);
            if (index > ATAN2_SIZE) index = ATAN2_SIZE;

            if (x < 0f) {
                return y < 0f ? -PI + ATAN2[index] : PI - ATAN2[index];
            } else {
                return y < 0f ? -ATAN2[index] : ATAN2[index];
            }
        }
    }

    /**
     * Returns the largest integer value less than or equal to the specified float value.
     *
     * @param value the floating-point number to be floored
     * @return the largest integer value less than or equal to {@code value}
     */
    public static int floor(float value) {
        int i = (int) value;
        return value < (float) i ? i - 1 : i;
    }

    /**
     * Returns the largest integer value less than or equal to the specified double value.
     *
     * @param value the floating-point number to be floored
     * @return the largest integer value less than or equal to {@code value}
     */
    public static int floor(double value) {
        int i = (int) value;
        return value < (double) i ? i - 1 : i;
    }

}
