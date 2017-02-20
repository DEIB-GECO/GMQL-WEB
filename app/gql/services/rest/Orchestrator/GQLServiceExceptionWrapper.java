package gql.services.rest.Orchestrator;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by abdulrahman on 20/02/2017.
 */
@XmlRootElement(name = "gql-service-exception")
public final class GQLServiceExceptionWrapper {

    String message;

    /**
     *
     */
    public GQLServiceExceptionWrapper() {
    }

    /**
     *
     * @param message
     */
    public GQLServiceExceptionWrapper(String message) {
        setMessage(message);
    }

    /**
     *
     * @return
     */
    public String getMessage() {
        return message;
    }

    /**
     *
     * @param message
     */
    public void setMessage(String message) {
        this.message = message;
    }
}
