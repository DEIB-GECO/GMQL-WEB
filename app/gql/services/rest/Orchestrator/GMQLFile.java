package gql.services.rest.Orchestrator;

/**
 * Created by abdulrahman on 14/02/2017.
 */



        import java.util.ArrayList;
        import java.util.Collections;
        import java.util.Comparator;
        import java.util.List;
        import javax.xml.bind.annotation.*;

/**
 *
 * @author Massimo Quadrana <massimo.quadrana at polimi.it>
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType
public class GMQLFile {

    @XmlElement
    private final String filename;

    @XmlElement
    private final String filekey;

    @XmlTransient
    private GMQLFile parent = null;



    @XmlElement(name = "gmqlFile")
    private List<GMQLFile> children = null;

    @XmlAttribute
    private final boolean isDirectory;

    /**
     *
     */
    public GMQLFile() {
        this("", "", false);
    }

    /**
     *
     * @param filename
     * @param key
     * @param isDirectory
     */
    public GMQLFile(String filename, String key, boolean isDirectory) {
        this.filename = filename;
        this.filekey = key;
        this.isDirectory = isDirectory;
    }

    public List<GMQLFile> getChildren() {
        if(children == null)
            return new ArrayList<>();
        return children;
    }

    /**
     *
     * @return
     */
    public String getFilename() {
        return filename;
    }

    /**
     *
     * @return
     */
    public String getKey() {
        return filekey;
    }

    /**
     *
     * @return
     */
    public boolean isDirectory() {
        return isDirectory;
    }

    /**
     *
     * @return
     */
    public GMQLFile getParent() {
        return parent;
    }

    /**
     *
     * @param parent
     */
    public void setParent(GMQLFile parent) {
        this.parent = parent;
    }

    /**
     *
     * @param child
     */
    public void addChild(GMQLFile child) {
        if (this.children == null) {
            children = new ArrayList<>();
        }
        children.add(child);
//        System.out.println("Child :   "+child.getFilename());
        Collections.sort(children, new NameComparator());
    }

    /**
     *
     * @param child
     */
    public void removeChild(GMQLFile child){
        if(children != null)
            children.remove(child);
    }

    /**
     *
     * @return
     */
    public boolean isEmpty(){
        return isDirectory && (children == null || children.isEmpty());
    }


    private static class NameComparator implements Comparator{

        @Override
        public int compare(Object o1, Object o2) {
            String name1 = ((GMQLFile) o1).filename;
            String name2 = ((GMQLFile) o2).filename;
            return name1.compareTo(name2);
        }

    }


}

