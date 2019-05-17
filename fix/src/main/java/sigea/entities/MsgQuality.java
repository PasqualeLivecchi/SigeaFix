package sigea.entities;

/**
 * * * @author Pasquale Livecchi
 */
public enum MsgQuality {
    NONE, 
    UNKNOWN, 
    BAD, 
    GOOD;

    public byte byteValue() {
        return (byte) ordinal();
    }

    public static MsgQuality fromQualityByte(byte b) {
        if (b < 0 || b >= values().length) {
            return NONE;
        }
        return values()[b];
    }
}
