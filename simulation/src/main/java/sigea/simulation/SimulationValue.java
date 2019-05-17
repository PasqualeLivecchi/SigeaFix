package sigea.simulation;


import lombok.Getter;
import sigea.entities.Message;

@Getter
class SimulationValue<T extends Message> {

    T msg;
    double val;

    public SimulationValue(T msg, double val) {
        this.msg = msg;
        this.val = val;
    }
}
