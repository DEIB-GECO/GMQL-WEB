package gql.services.rest.Orchestrator;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by abdulrahman on 20/02/2017.
 */
@XmlRootElement
@XmlType(name = "gmqlSchema")
@XmlAccessorType(XmlAccessType.FIELD)
public class GMQLSchema {

    @XmlAttribute(name = "name", required = true)
    private String schemaName;
    @XmlAttribute(name = "type", required = true)
    private String schemaType;
    @XmlElement(name = "field", required = true)
    private List<GMQLSchemaField> fields;

    /**
     *
     */
    public GMQLSchema() {
        this.schemaName = "";
        this.schemaType = "";
        this.fields = new ArrayList<>();
    }

    /**
     *
     * @param schemaName
     * @param schemaType
     * @param fields
     */
    public GMQLSchema(String schemaName, String schemaType,List<GMQLSchemaField> fields) {
        this.schemaName = schemaName;
        this.schemaType = schemaType;
        this.fields = fields;
    }

    /**
     * @return the fields
     */
    public List<GMQLSchemaField> getFields() {
        if (fields == null) {
            return new ArrayList<>();
        }
        return fields;
    }

    /**
     * @return the schemaName
     */
    public String getSchemaName() {
        return schemaName;
    }

    /**
     * @return the schemaType
     */
    public String getSchemaType() {
        return schemaType;
    }


    /**
     * @param schemaName the schemaName to set
     */
    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    /**
     * @param schemaType the schemaType to set
     */
    public void setSchemaType(String schemaType) {
        this.schemaType = schemaType;
    }

    /**
     * @param fields the fields to set
     */
    public void setFields(List<GMQLSchemaField> fields) {
        this.fields = fields;
    }

    /**
     *
     * @param schemaFilePath
     * @return
     * @throws JAXBException
     * @throws FileNotFoundException
     */
    public static GMQLSchema parseGQLSchema(Path schemaFilePath) throws JAXBException, FileNotFoundException{
        JAXBContext jaxbContext = JAXBContext.newInstance("entities");
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        //TODO: Schema validation
        return (GMQLSchema) unmarshaller.unmarshal(new FileInputStream(schemaFilePath.toFile()));
    }
}

