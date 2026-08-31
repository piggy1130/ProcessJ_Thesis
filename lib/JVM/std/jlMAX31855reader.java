package std;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.DigitalState;
import com.pi4j.io.spi.Spi;
import com.pi4j.io.spi.SpiBus;
import com.pi4j.io.spi.SpiChipSelect;
import com.pi4j.io.spi.SpiMode;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class jlMAX31855reader {

    // =========================
    // Recommendation (IMPORTANT)
    // =========================
    // Use ONE SPI instance for SPI0 and control ALL MAX31855 chip-select lines via GPIO.
    // This avoids Pi4J "SPI-x already exists" registry collisions and is ideal for multi-sensor setups.
    //
    // Usage:
    //   initMAX31855();   // once before starting ProcessJ par{...}
    //   readTemp_HwSPI_ManualCS(gpioCS);  // in loop
    //   shutdown();       // if you ever exit

    private static final int BAUD = 5_000_000;

    private static volatile Context ctx = null;
    private static volatile boolean initialized = false;

    // Single SPI instance (recommended)
    private static Spi spi0 = null;

    // Cache CS GPIO objects
    private static final Map<Integer, DigitalOutput> manualCsMap = new HashMap<>();

    /** Call once (safe to call multiple times). */
    //public static synchronized void init() {
    public static void init() {
        if (initialized) return;

        ctx = Pi4J.newAutoContext();

        // Create exactly ONE SPI instance for SPI0.
        // chipSelect here is mostly "dummy" because we drive CS via GPIO manually.
        spi0 = ctx.create(Spi.newConfigBuilder(ctx)
                .bus(SpiBus.BUS_0)
                .chipSelect(SpiChipSelect.CS_1)
                .baud(BAUD)
                .mode(SpiMode.MODE_0)
                .build());

        initialized = true;
    }

    /** ProcessJ-friendly wrapper */
    public static void initMAX31855() {
        init();
    }

    /** Optional: call when your program terminates (your current program never terminates). */
    //public static synchronized void shutdown() {
    public static void shutdown() {
        if (!initialized) return;

        // Deassert all CS lines
        for (DigitalOutput cs : manualCsMap.values()) cs.high();
        manualCsMap.clear();

        if (spi0 != null) spi0.close();
        if (ctx != null) ctx.shutdown();

        spi0 = null;
        ctx = null;
        initialized = false;
    }

    /** Public API used by ProcessJ codegen. */
    public static double readTemp_HwSPI_ManualCS(int csGpio) {
        if (!initialized) init();

        DigitalOutput cs = manualCsMap.get(csGpio);
        if (cs == null) {
            // IMPORTANT: use the current ctx, guaranteed initialized
            Context local = ctx;

            cs = local.create(DigitalOutput.newConfigBuilder(local)
                    .address(csGpio)
                    .initial(DigitalState.HIGH)
                    .shutdown(DigitalState.HIGH)
                    .id("cs-" + csGpio)
                    .name("CS GPIO" + csGpio)
                    .build());

            manualCsMap.put(csGpio, cs);
        }

        byte[] rx = new byte[4];

        cs.low();
        spi0.read(rx);
        cs.high();

        return decodeMax31855(rx);
    }

    private static double decodeMax31855(byte[] rx) {
        int raw = ByteBuffer.wrap(rx).getInt();

        if ((raw & 0x00010000) != 0) {
            boolean scv = (raw & 0x00000004) != 0;
            boolean scg = (raw & 0x00000002) != 0;
            boolean oc  = (raw & 0x00000001) != 0;
            throw new RuntimeException(String.format(
                    "MAX31855 fault: SCV=%b SCG=%b OC=%b (raw=0x%08X)", scv, scg, oc, raw));
        }

        int ext14 = (raw >> 18) & 0x3FFF;
        if ((ext14 & 0x2000) != 0) ext14 |= ~0x3FFF;

        return ext14 * 0.25;
    }
}
