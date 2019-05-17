/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sigea.entities;

import com.opencsv.bean.CsvBindByName;
import lombok.Data;

/**
 *
 * @author Pasquale Livecchi
 */
@Data
public class MsgConnection {
    @CsvBindByName(column="CONNECTION_NAME", required=true)
    private String nameId;
    @CsvBindByName(column="PORT_IP", required=false)
    private int port;
    public MsgConnection() {
    }
    public MsgConnection(String nameId){
        this.nameId = nameId;
    }
}
