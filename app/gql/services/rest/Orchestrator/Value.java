package gql.services.rest.Orchestrator;

import com.google.common.collect.ComparisonChain;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import java.util.Comparator;

/**
 * Created by abdulrahman on 20/02/2017.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Value {

    @XmlElement
    private final String name;
    private static Comparator<Value> comparator = null;

    /**
     *
     */
    public Value() {
        name = "";
    }

    /**
     *
     * @param name
     */
    public Value(String name) {
        this.name = name.trim();
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     *
     * @return
     */
    public static Comparator<Value> getComparator() {
        if (comparator == null) {
            comparator = new Comparator<Value>() {
                @Override
                public int compare(Value o1, Value o2) {
                    try {
                        Double val1 = Double.parseDouble(o1.getName());
                        Double val2 = Double.parseDouble(o2.getName());
                        return Double.compare(val1, val2);
                    } catch (Exception e) {
                    }
                    return ComparisonChain.start().compare(o1.getName().toLowerCase(), o2.getName().toLowerCase()).result();
                }
            };
        }
        return comparator;
    }
}

