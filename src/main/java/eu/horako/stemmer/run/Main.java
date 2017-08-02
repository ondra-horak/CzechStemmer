package eu.horako.stemmer.run;

import gnu.getopt.Getopt;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Command line interface for word expansion/stemming.
 * 
 * Use parameter -h from the command line to see the usage.
 * 
 * 
 * @author Ondrej Horak &lt;ondrej.horak@centrum.cz&gt;
 */
public class Main {
    private String mode = null;

    /**
     * @param args the command line arguments
     * @throws java.io.FileNotFoundException
     */
    public static void main(String[] args) throws FileNotFoundException {
        new Main().run(args);
    }
    
    
    
    public void run(String[] args) {
        parseOptions(Arrays.copyOf(args, args.length));
        long startTimeNS = System.nanoTime();
        try {        
            IRunner runner = null;
            switch(mode) {
                case "expand":
                case "expandall":
                case "expanddict":
                case "wordlist":
                case "stem":
                    runner = new DictionaryRunner();
                    break;
                case "fstbuild":
                case "fstcheck":
                case "fstsearch":
                    runner = new FSTBuilder();
                    break;
                default:
                    System.err.println("Unknown mode: " + mode);
                    return;
            }
            if(runner != null) {
                runner.init(args);
                runner.run();
            } else {
                
            }
        } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(1);
        }
        
        
        long endTimeNS = System.nanoTime();
        System.err.println("Processing time: " + ((endTimeNS-startTimeNS)/1000.0/1000/1000) + " seconds" );
    }
   
    
    private void parseOptions(String[] inputArgs) {
        String[] args  = Arrays.copyOf(inputArgs, inputArgs.length);
        Getopt g = new Getopt("processor", args, "m:h");
        g.setOpterr(false);
        
        int opt;
        boolean printHelp = false;
        while ((opt = g.getopt()) != -1) {
            switch(opt) {
              case 'h':
                  printHelp = true;
                  break;
              case 'm':
                  mode = g.getOptarg().toLowerCase();
                  break;
              case '?':
                  break; // getopt() already printed an error
              default:
                  break;//System.out.print("getopt() returned " + opt + "\n");
            }
        }
        
        if(mode == null) {
            printResourceToStderr("help.txt");
            System.exit(0);
        }
    }
    
    public static void printResourceToStderr(String resourceName) {
        try {
            InputStreamReader reader = new InputStreamReader(Main.class.getClassLoader().getResourceAsStream(resourceName),"UTF-8");
            OutputStreamWriter writer = new OutputStreamWriter(System.err, "UTF-8");
            char[] buffer = new char[8192];
            while(true) {
                int r = reader.read(buffer);
                if(r == -1) {
                    break;
                }
                writer.write(buffer,0,r);
            }
            writer.flush();
        } catch(Exception ex) {
        }
    }
    
}

