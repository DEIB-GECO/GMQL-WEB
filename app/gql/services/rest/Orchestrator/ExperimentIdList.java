package gql.services.rest.Orchestrator;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by abdulrahman on 20/02/2017.
 */
@XmlRootElement
public final class ExperimentIdList {

    @XmlElement(name = "experiementId")
    private final List<String> experimentIds;
    @XmlAttribute(name = "count")
    private final int count;

    /**
     *
     */
    public ExperimentIdList() {
        this.experimentIds = new ArrayList<>();
        this.count = 0;
    }

    /**
     * Creates a new instance of {@code ExperimentsList} from a list of experiments' ids
     * @param experiments
     */
    public ExperimentIdList(List<String> experiments) {
        this.experimentIds = experiments;
        this.count = this.experimentIds.size();
        //internally sort attribute list
        Collections.sort(this.experimentIds);
    }

    /**
     *
     * @return return the experimentIds
     */
    public List<String> getExperimentIds() {
        if (this.experimentIds == null) {
            return new ArrayList<>();
        }
        return this.experimentIds;
    }

    /**
     * @return the count
     */
    public int getCount() {
        return count;
    }
}

