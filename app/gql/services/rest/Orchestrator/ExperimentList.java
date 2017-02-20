package gql.services.rest.Orchestrator;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by abdulrahman on 20/02/2017.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public final class ExperimentList {

    @XmlElement(name = "experiment")
    private final List<Experiment> experiments;
    @XmlAttribute
    private final int count;

    /**
     *
     */
    public ExperimentList(){
        this.experiments = new ArrayList<>();
        this.count = 0;
    }

    /**
     *
     * @param experiments
     */
    public ExperimentList(List<Experiment> experiments){
        this.experiments = experiments;
        this.count = this.experiments.size();
    }

    /**
     * @return the experiments
     */
    public List<Experiment> getExperiments() {
        return experiments;
    }

    /**
     * @return the count
     */
    public int getCount() {
        return count;
    }


}

