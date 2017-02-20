package gql.services.rest.Orchestrator;

/**
 * Created by abdulrahman on 14/02/2017.
 */
import com.google.common.collect.ComparisonChain;
import java.util.Comparator;
import java.util.Objects;
import javax.xml.bind.annotation.*;

/**
 * Represents a metadata as an (attribute,value) pair and wraps it into a JAXB object.
 * @author Massimo Quadrana <massimo.quadrana at polimi.it>
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType
public class Metadata {

    @XmlElement(required = true)
    private String attribute;
    @XmlElement(required = true)
    private String value;
    private static Comparator<Metadata> comparator = null;

    /**
     *
     */
    public Metadata() {
    }

    /**
     *
     * @param attribute
     * @param value
     */
    public Metadata(String attribute, String value) {
        this.attribute = attribute.trim();
        this.value = value.trim();
    }

    /**
     *
     * @return
     */
    public String getAttribute() {
        return attribute;
    }

    /**
     *
     * @param attribute
     */
    public void setAttribute(String attribute) {
        this.attribute = attribute.trim();
    }

    /**
     *
     * @return
     */
    public String getValue() {
        return value;
    }

    /**
     *
     * @param value
     */
    public void setValue(String value) {
        this.value = value.trim();
    }

    /**
     *
     * @return
     */
    @Override
    public int hashCode() {
        return Objects.hash(getAttribute(), getValue());
    }

    /**
     *
     * @param obj
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Metadata)) {
            return false;
        }
        Metadata objMetadata = (Metadata) obj;
        return Objects.equals(getAttribute(), objMetadata.getAttribute())
                && Objects.equals(getValue(), objMetadata.getValue());
    }

    /**
     *
     * @return
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(").append(getAttribute()).append(",").append(getValue()).append(")");
        return sb.toString();
    }

    /**
     *
     * @return
     */
    public static Comparator<Metadata> getComparator() {
        if (comparator == null) {
            comparator = new Comparator<Metadata>() {

                @Override
                public int compare(Metadata o1, Metadata o2) {
                    return ComparisonChain.start().compare(o1.getAttribute().toLowerCase(), o2.getAttribute().toLowerCase()).compare(o1.getValue(), o2.getValue()).result();
                }
            };
        }
        return comparator;
    }
}

