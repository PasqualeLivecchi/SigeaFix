package sigea.entities;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * * * @author Pasquale Livecchi
 */
@Getter
@Setter
public class MsgConfig {

    private String id;
    private List<Message> msgs = new ArrayList<>();
    private List<MsgConnection> msgConnections = new ArrayList<>();
}
