package std;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class temperature_file {

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public static void writeTemperature(String filename, double temperature, boolean append) {
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(filename, append), StandardCharsets.UTF_8))) {
            if (!append) {
                writer.write("date_time\ttemperature_celsius");
                writer.write(System.lineSeparator());
            }

            writer.write(LocalDateTime.now().format(DATE_TIME_FORMAT));
            writer.write('\t');
            writer.write(Double.toString(temperature));
            writer.write(System.lineSeparator());
        } catch (IOException exception) {
            System.err.println("Could not write temperature data to '" + filename + "': "
                    + exception.getMessage());
        }
    }
}
