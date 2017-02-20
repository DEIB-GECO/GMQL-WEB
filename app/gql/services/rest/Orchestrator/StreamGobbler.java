package gql.services.rest.Orchestrator;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by abdulrahman on 14/02/2017.
 */
public class StreamGobbler extends Thread {

    InputStream is;
    OutputStream os;
    String type;

    /**
     *
     * @param is
     */
    public StreamGobbler(InputStream is) {
        this(is,"GMQL>", null);
    }

    /**
     *
     * @param is
     * @param os
     */
    public StreamGobbler(InputStream is, String type,OutputStream os) {
        this.is = is;
        this.os = os;
        this.type = type;
    }

    /**
     *
     */
    @Override
    public void run() {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        PrintWriter pw = null;
        String line;

        if (os != null) {
            pw = new PrintWriter(os);
        }

        try {
            while ((line = br.readLine()) != null) {
                if (pw != null) {
                    pw.println(type+line);
                }
            }
            if (pw != null) {
                pw.flush();
            }
        } catch (IOException ex) {
            Logger.getLogger(StreamGobbler.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }

    }
}

