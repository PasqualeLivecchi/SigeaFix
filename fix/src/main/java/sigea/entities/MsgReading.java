package sigea.entities;

import java.time.Instant;
import java.util.Date;

import lombok.Getter;
import lombok.ToString;

/**
 * 
 *  @author Pasquale Livecchi
 */
@Getter
public class MsgReading {

    private final String msgKey;
    private final byte quality;
    private final long time;
    private final double value;

    public MsgReading(Message msg, Double value, byte quality) {
        this(msg.uniqueKeyName(), Instant.now().getEpochSecond(), value, quality);
    }
    
    public MsgReading(Message msg, long time, Double value, byte quality) {
        this(msg.uniqueKeyName(), time, value, quality);
    }

    public MsgReading(String msgKey, long time, Double value, byte quality) {
        this.msgKey = msgKey;
        this.time = time;
        this.value = value;
        this.quality = quality;
    }

    @Override
    public String toString() {
        return (msgKey + "," + MsgQuality.fromQualityByte(quality).toString() + "," + Long.toString(new Date(time).getTime()) + "," + Double.toString(value));
    }
}
