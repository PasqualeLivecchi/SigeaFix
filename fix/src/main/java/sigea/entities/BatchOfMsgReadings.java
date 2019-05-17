package sigea.entities;


import sigea.entities.MsgReading;
import java.util.List;
import lombok.Getter;

/**
 * * * @author Pasquale Livecchi
 */
@Getter
public class BatchOfMsgReadings {

//    private final MsgType msgType;
    private final List<MsgReading> msgReadings;

    public BatchOfMsgReadings(List<MsgReading> msgList) {
//        this.msgType = msgType;
        this.msgReadings = msgList;
    }

    public boolean hasData() {
        return !msgReadings.isEmpty();
    }
}
