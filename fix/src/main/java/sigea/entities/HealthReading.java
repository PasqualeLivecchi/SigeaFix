package sigea.entities;

import java.io.Serializable;
import java.util.Scanner;
import lombok.Getter;
import lombok.Setter;

/**
 * * * @author Pasquale Livecchi
 */
@Getter
@Setter
public class HealthReading implements Serializable {

    private static final long serialVersionUID = 1L;
    public static final String RUNTIME = "RUNTIME";
    public static final String LIF = "LIF";
    public static final String DIF = "DIF";
    public static final String ERM = "ERM";
    public static final String EMF = "EMF";
    public static final String ERS = "ERS";
    public static final String SPF = "SPF";
    public static final String SPY = "SPY";
    public static final String IFF = "IFF";
    public static final String BDM = "DIF";
    public static final String DAF = "DAF";

    private String component;
    private HealthStatus code = HealthStatus.NOT_OPERATIONAL;
    private String message = "Component has not reported its status";
    private long time = System.currentTimeMillis();

    public HealthReading() {
    }

    public HealthReading(String name) {
        component = name;
    }

    /**
     * Parses a health reading from a line of text (comma delimited)
     *
     * @param line
     * @return
     */
    public static HealthReading parse(String line) {
        Scanner sc = new Scanner(line.trim()).useDelimiter(",");
        HealthReading rec = new HealthReading();
        if (sc.hasNext()) {
            rec.component = sc.next();
        }
        if (sc.hasNextLong()) {
            rec.time = sc.nextLong();
        }
        if (sc.hasNext()) {
            rec.code = HealthStatus.valueOf(sc.next());
        }
        if (sc.hasNext()) {
            rec.message = sc.next();
        }
        return rec;
    }

    @Override
    public String toString() {
        return component + "," + time + "," + code.name() + "," + message;
    }
}
