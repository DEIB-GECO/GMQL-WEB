package gql.services.rest.Orchestrator;

/**
 * Created by abdulrahman on 14/02/2017.
 */

import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class JobList {
    private List<String> jobs;

    /**
     *
     */
    public JobList(){

    }

    /**
     *
     * @param jobs
     */
    public JobList(List<String> jobs){
        this.jobs = jobs;
    }

    /**
     * @return the jobs
     */
    public List<String> getJobs() {
        return jobs;
    }


}

