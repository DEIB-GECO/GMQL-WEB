package gql.services.rest.Orchestrator;

/**
 * Created by abdulrahman on 14/02/2017.
 */

import java.util.Date;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Massimo Quadrana <massimo.quadrana at polimi.it>
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class GMQLJobStatusXML {

    @XmlElement(name = "start_date")
    private Date startDate;
    @XmlElement(name = "status")
    private String jobStatus;
    @XmlElement(name = "message")
    private String stateMessage;
    @XmlElement(name = "datasetNames")
    private String resultDatsetsNames;
    @XmlElement(name = "execTime")
    private String exeutiontime;
    @XmlElementWrapper(name = "experiments")
    @XmlElement(name = "filekey")
    private List<String> experiments;


    public GMQLJobStatusXML() {

    }

    /**
     *
     * @param startDate
     * @param jobStatus
     * @param stateMessage
     * @param experiments
     * @param datasetsnames
     */
    public GMQLJobStatusXML(Date startDate, String jobStatus, String stateMessage, List<String> experiments, String datasetsnames, String exeutiontime) {
        this.startDate = startDate;
        this.jobStatus = jobStatus;
        this.stateMessage = stateMessage;
        this.experiments = experiments;
        this.resultDatsetsNames = datasetsnames;
        this.exeutiontime = exeutiontime;
    }

    /**
     * @return the jobState
     */
    public String getJobState() {
        return jobStatus;
    }

    /**
     * @param jobState the jobState to set
     */
    public void setJobState(String jobState) {
        this.jobStatus = jobState;
    }

    /**
     * @return the stateMessage
     */
    public String getStateMessage() {
        return stateMessage;
    }

    public String getexeutiontime() {
        return exeutiontime;
    }

    /**
     * @return the experiments
     */
    public List<String> getExperiments() {
        return experiments;
    }

    public String getResultDataSetsNames() {
        return resultDatsetsNames;
    }

    /**
     * @return the startDate
     */
    public Date getStartDate() {
        return startDate;
    }

    /**
     * @param startDate the startDate to set
     */
    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }
}
