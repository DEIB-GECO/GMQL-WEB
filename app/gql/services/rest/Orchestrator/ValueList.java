package gql.services.rest.Orchestrator;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by abdulrahman on 20/02/2017.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public final class ValueList {

    @XmlElement(name = "value")
    private final List<Value> values;

    /**
     *
     */
    public ValueList() {
        this.values = new ArrayList<>();
    }

    /**
     * Creates a new instance of {@code ValueList} from a list of metadata's
     * attributes
     *
     * @param values
     */
    public ValueList(List<Value> values) {
        this.values = values;
        Collections.sort(this.values, Value.getComparator());
    }

    /**
     *
     * @return the values
     */
    public List<Value> getValues() {
        return values;
    }
}

