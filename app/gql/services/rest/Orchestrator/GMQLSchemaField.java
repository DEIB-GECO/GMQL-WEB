package gql.services.rest.Orchestrator;

import javax.xml.bind.annotation.*;

/**
 * Created by abdulrahman on 20/02/2017.
 */
@XmlType
@XmlAccessorType(XmlAccessType.FIELD)
public class GMQLSchemaField {

    @XmlValue
    private String fieldName;

    @XmlAttribute(name = "type", required = true)
    private String fieldType;

    /**
     *
     */
    public GMQLSchemaField() {
        fieldName = "";
        fieldType = "";
    }

    /**
     * Creates a new instance of {@code GQLSchemaField} from its name and type
     *
     * @param fieldName name of the field
     * @param fieldType type of the field
     */
    public GMQLSchemaField(String fieldName, String fieldType) {
        this.fieldName = fieldName;
        this.fieldType = fieldType;
    }

    /**
     * @return the fieldName
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * @return the fieldType
     */
    public String getFieldType() {
        return fieldType;
    }

    /**
     * @param fieldName the fieldName to set
     */
    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    /**
     * @param fieldType the fieldType to set
     */
    public void setFieldType(String fieldType) {
        this.fieldType = fieldType;
    }

}

