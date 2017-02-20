package gql.services.rest.Orchestrator;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by abdulrahman on 20/02/2017.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public final class GMQLSchemaCollection {

    @XmlAttribute(name = "name", required = true)
    private String collectionName;
    @XmlElement(name = "gmqlSchema", required = true)
    private List<GMQLSchema> schemaList;

    /**
     *
     */
    public GMQLSchemaCollection() {
        this(new ArrayList<GMQLSchema>());
    }

    /**
     *
     * @param schemaList
     */
    public GMQLSchemaCollection(List<GMQLSchema> schemaList){
        this.schemaList = schemaList;
    }

    /**
     * @return the gqlSchema
     */
    public List<GMQLSchema> getSchemaList() {
        if (schemaList == null)
            return new ArrayList<>();
        return schemaList;
    }

    /**
     * @param schemaList the gqlSchema to set
     */
    public void setSchemaList(List<GMQLSchema> schemaList) {
        this.schemaList = schemaList;
    }

    /**
     * @return the collectionName
     */
    public String getCollectionName() {
        return collectionName;
    }

    /**
     * @param collectionName the collectionName to set
     */
    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    /**
     *
     * @param schemaFilePath
     * @return
     * @throws JAXBException
     * @throws FileNotFoundException
     */
    public static GMQLSchemaCollection parseGQLSchemaCollection(Path schemaFilePath) throws JAXBException, FileNotFoundException{
        JAXBContext jaxbContext = JAXBContext.newInstance(GMQLSchemaCollection.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        //TODO: Schema validation
        return (GMQLSchemaCollection) unmarshaller.unmarshal(new FileInputStream(schemaFilePath.toFile()));

    }
    /**
     * @throws javax.xml.bind.JAXBException
     * @throws java.io.FileNotFoundException
     * @auther Abdulrahman Kaitoua <abdulrahman.kaitoua at polimi.it>
     * @param schemaFilePath the file path to where to store the schema
     */
    public void bindGMQLSchemaCollection(Path schemaFilePath) throws JAXBException, FileNotFoundException{
        JAXBContext jaxbContext = JAXBContext.newInstance(GMQLSchemaCollection.class);
        Marshaller marshaller = jaxbContext.createMarshaller();

        marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, true );
        marshaller.marshal( this, new File(schemaFilePath.toString()) );
        //TODO: Schema validation
        //return (GMQLDataSetCollection) unmarshaller.unmarshal(new FileInputStream(DataSetFilePath.toFile()));

    }
}

