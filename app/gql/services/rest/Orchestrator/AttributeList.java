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
public final class AttributeList {

    @XmlElement(name = "attribute")
    private final List<Attribute> attributes;

    /**
     *
     */
    public AttributeList() {
        this.attributes = new ArrayList<>();
    }

    /**
     * Creates a new instance of {@code AttributeList} from a list of attributes
     *
     * @param attributes
     */
    public AttributeList(List<Attribute> attributes) {
        this.attributes = attributes;
        //internally sort attribute list
        Collections.sort(this.attributes, Attribute.getComparator());
    }
    /**
     *
     * @return the attributes
     */
    public List<Attribute> getAttributes() {
        if (this.attributes == null) {
            return new ArrayList<>();
        }
        return this.attributes;
    }
}

