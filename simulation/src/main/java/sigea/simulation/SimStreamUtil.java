package sigea.simulation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;

import javax.swing.JOptionPane;

import sigea.entities.MsgQuality;
import sigea.entities.MsgReading;
import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import io.vavr.control.Try;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

public class SimStreamUtil {

    public static void generateBlank(Iterable<?> msgs) {
        Path csv;
        try {
            csv = Files.createTempFile("sigea_simulation_data", ".csv");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Unable to write to CSV file");
            return;
        }

        try (Writer w = Files.newBufferedWriter(csv)) {
            Stream.ofAll(msgs)
                    .map(Object::toString)
//                    .map(s -> s.replaceAll("Message(", ""))
//                    .map(s -> s.replaceAll(")", ""))
                    .intersperse(",")
                    .forEach(str -> Try.run(() -> w.write(str)));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        // boolean opened = false; 
        try {
            Files.copy(csv, Paths.get(csv.getFileName().toString()), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e1) {
            JOptionPane.showMessageDialog(null, "Unable to copy CSV to current directory");
        }
    }
    
    public static Option<Path> chooseFile(String description, String extension) {
        JFileChooser file = new JFileChooser();
        for (FileFilter filter : file.getChoosableFileFilters()) {
            file.removeChoosableFileFilter(filter);
        }
        file.addChoosableFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().endsWith(extension);
            }

            @Override
            public String getDescription() {
                return description;
            }
        });
        file.setAcceptAllFileFilterUsed(false);
        file.setMultiSelectionEnabled(false);
        if (JFileChooser.APPROVE_OPTION == file.showOpenDialog(null)) {
            return Option.of(file.getSelectedFile().toPath());
        }
        return Option.none();
    }

    private static Iterable<String> nonHeaderRows(Path csv) {
        return () -> Try.of(() -> Files.lines(csv)).get().skip(1).iterator();
    }

    private static double valueOrNan(String value) {
        return Try.of(() -> Double.valueOf(value))
                .getOrElse(Double.NaN);
    }

    private static Stream<Double> parse(String line) {
        return Stream.of(line.split(","))
                .map(SimStreamUtil::valueOrNan);
    }

    private static Stream<MsgReading> combineHeaderRow(Stream<String> headers, String row) {
        return headers.zip(parse(row))
                .filter(head_val -> Double.isFinite(head_val._2))
                .map(head_valFinite -> new MsgReading(head_valFinite._1, 0, head_valFinite._2, MsgQuality.NONE.byteValue()));
    }

    private static <T> ObservableTransformer<T, T> onInterval(int delay, TimeUnit unit) {
        return obs -> Observable.interval(delay, unit).zipWith(obs, (a, b) -> b);
    }

    public static Try<Observable<Stream<MsgReading>>> simulateStream(Path csv, int hz) {
        return Try.of(() -> {
            return Files.lines(csv)
                    .findFirst()
                    .map(line -> Stream.of(line.split(",")))
                    .map(headers -> {
                        return Observable.fromIterable(nonHeaderRows(csv))
                                .compose(onInterval(1000 / hz, TimeUnit.MILLISECONDS))
                                .map(row -> combineHeaderRow(headers, row))
                                .repeat();
                    })
                    .orElseThrow(() -> new IOException("Could not read headers"));
        });
    }
}
