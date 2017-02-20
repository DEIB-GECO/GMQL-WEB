package gql.services.rest.Orchestrator;

/**
 * Created by abdulrahman on 14/02/2017.
 */
public enum GMQLFileTypes {

    ANY (""),
    METADATA ("meta"),
    QUERY ("gmql"),
    SCHEMA ("schema"),
    TRANSLATION ("pig");

    private final String extension;

    private GMQLFileTypes(String extension) {
        this.extension = extension;
    }

    /**
     *
     * @return
     */
    public String getExtension(){
        return this.extension;
    }

}
