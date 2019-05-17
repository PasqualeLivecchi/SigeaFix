package sigea.entities;

import lombok.Getter;

/**
 * * * @author Pasquale Livecchi
 */
@Getter
public enum MsgType {
    MT("MT MESSAGE", "MT"), 
    IFN("IFN MESSAGE", "IFN"), 
    NSERIES("NSERIES MESSAGE", "NSERIES"), 
    ARC("ARC MESSAGE", "ARC");

    private final String longName;
    private final String shortName;

    private MsgType(String longName, String shortName) {
        this.longName = longName;
        this.shortName = shortName;
    }
}
