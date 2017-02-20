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
public class Attribute {

    @XmlElement
    private final String name;

    @XmlElement
    private final String id;

    private static Comparator<Attribute> comparator = null;

    /**
     *
     */
    public Attribute() {
        this("");
    }

    /**
     *
     * @param name
     */
    public Attribute(String name) {
        this(name, null);
    }

    public Attribute(String name, String id) {
        this.name = name.trim();
        this.id = id != null ? id.trim() : null;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }


    /**
     *
     * @return
     */
    public static Comparator<Attribute> getComparator() {
        if (comparator == null) {
            comparator = new Comparator<Attribute>() {

                @Override
                public int compare(Attribute o1, Attribute o2) {
                    return ComparisonChain.start().compare(o1.getName().toLowerCase(), o2.getName().toLowerCase()).result();
                }
            };
        }
        return comparator;
    }
}

