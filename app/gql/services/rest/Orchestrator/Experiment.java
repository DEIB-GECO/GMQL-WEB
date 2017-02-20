package gql.services.rest.Orchestrator;

import javax.xml.bind.annotation.*;
import java.util.*;

/**
 * Created by abdulrahman on 20/02/2017.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public final class Experiment {

    @XmlElement
    private final List<Metadata> metadata;
    @XmlAttribute
    private final int count;

    /**
     *
     */
    public Experiment() {
        this.metadata = new ArrayList<>();
        this.count = 0;
    }

    /**
     *
     * @param metadata
     */
    public Experiment(List<Metadata> metadata) {
        this.metadata = metadata;
        this.count = this.metadata.size();
        //internally sort attribute list
        Collections.sort(this.metadata, Metadata.getComparator());
    }

    public Experiment(List<Metadata> metadata, List<String> topAttributes) {

        HashMap<String, Integer> mapAttPos = new HashMap<>();
        HashSet<String> topAttSet = new HashSet();
        List<Metadata> otherList = new ArrayList<>();
        List<List<Metadata>> topLists = new ArrayList<>();

        //System.out.println("INIT MAPS");
        for(int i = 0; i < topAttributes.size(); i++){
            mapAttPos.put(topAttributes.get(i),i);
            System.out.println("Put in map " +topAttributes.get(i) + " " + i);
            topAttSet.add(topAttributes.get(i));
            topLists.add(new ArrayList<Metadata>());
        }
        //System.out.println("Build OUTPUT");
        for (Metadata m : metadata) {
            String attribute = m.getAttribute();
            //System.out.print("Testing attribute " + attribute);
            if(topAttSet.contains(attribute)){
                //System.out.println("-> is found in map ");
                int index = mapAttPos.get(attribute);
                topLists.get(index).add(m);
            }
            else{
                //System.out.println("-> is NOT found in map ");
                otherList.add(m);
            }
        }

        Collections.sort(otherList, Metadata.getComparator());


        this.metadata = new ArrayList<>();
        for(List<Metadata> lm : topLists){
            this.metadata.addAll(lm);
        }
        this.metadata.addAll(otherList);

        this.count = this.metadata.size();
    }

    /**
     *
     * @return
     */
    public List<Metadata> getMetadata() {
        if (this.metadata == null) {
            return new ArrayList<>();
        }
        return this.metadata;
    }

    /**
     * @return the count
     */
    public int getCount() {
        return count;
    }
}

