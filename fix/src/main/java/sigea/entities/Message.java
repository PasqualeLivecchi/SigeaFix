package sigea.entities;

import com.opencsv.bean.CsvBindByName;
import java.util.Objects;

import lombok.Getter;
import lombok.Setter;

/**
 * * * Base class for all messages that interact with Sigea
 *
 * @author Pasquale Livecchi
 */
@Getter
@Setter
public class Message {
    @CsvBindByName(required = true, column = "CONNECTION")
    private String msgConn;
    @CsvBindByName(required = true, column = "MSG_NAME")
    private String msgName;
    @CsvBindByName(required = true, column = "DATA_TYPE")
    private String dataType;
    @CsvBindByName(required = true, column = "FIELD_NAME")
    private String fieldName;
    @CsvBindByName(required = true, column = "DESCRIPTION")
    private String description;
    @CsvBindByName(required = true, column = "UNITS")
    private String units;
    @CsvBindByName(required = true, column = "DIM")
    private String dimension;
    @CsvBindByName(required = true, column = "INIT_VALUE")
    private String initValue;
    @CsvBindByName(required = true, column = "RANGE_LOW")
    private double rangeLow;
    @CsvBindByName(required = true, column = "RANGE_HIGH")
    private double rangeHigh;
    @CsvBindByName(required = true, column = "BYTE_OFFSET")
    private int byteOffset;
    @CsvBindByName(required = true, column = "BIT_OFFSET")
    private int bitOffset;
    @CsvBindByName(column = "REMARKS")
    private String remarks;
    private MsgType msgType;
    private int id;

    public Message() {
    }

    public Message(String msgName, String fieldName) {
        this.msgName = msgName;
        this.msgName = fieldName;
    }

    public String uniqueKeyName() {
        return msgName + ":" + fieldName;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(msgName, fieldName);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!Message.class.isInstance(obj)) {
            return false;
        }
        final Message other = (Message) obj;
        if (!Objects.equals(this.msgName, other.msgName)) {
            return false;
        }
        return Objects.equals(this.fieldName, other.fieldName);
    }
    
    @Override
    public String toString(){
        return uniqueKeyName();
    }
}
