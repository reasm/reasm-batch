package org.reasm.batch;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.ServiceLoader;

import org.reasm.*;
import org.reasm.messages.InternalAssemblerErrorMessage;
import org.reasm.source.SourceFile;

/**
 * The main class of the batch assembler.
 *
 * @author Francis Gagné
 */
public class Assembler {

    // TODO
    // - Read all the Configuration data from a file, not from command-line arguments
    // - Command-line arguments may influence the assembler's behavior (e.g. emit a listing or not)

    /**
     * The entry point of the batch assembler.
     *
     * @param args
     *            the command-line arguments
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java -jar reasm-batch.jar <file> <initialArchitecture>");
            System.exit(1);
        } else {
            try {
                // Initialize the environment.
                final ArrayList<Architecture> architectures = new ArrayList<>();
                final ServiceLoader<ArchitectureProvider> loader = ServiceLoader.load(ArchitectureProvider.class);
                for (ArchitectureProvider architectureProvider : loader) {
                    for (Architecture architecture : architectureProvider) {
                        architectures.add(architecture);
                    }
                }

                final Environment environment = Environment.DEFAULT.addArchitectures(architectures);

                // Set the initial architecture.
                final Architecture initialArchitecture = environment.findArchitectureByName(args[1]);
                if (initialArchitecture == null) {
                    System.err.println("Architecture \"" + args[1] + "\" was not found");
                    return;
                }

                // Create the main source file.
                final Path path = Paths.get(args[0]);
                final Path fileName = path.getFileName();
                final SourceFile mainSourceFile = new SourceFile(new String(Files.readAllBytes(path), "UTF-8"),
                        fileName == null ? "" : fileName.toString());

                // Create a configuration.
                final FileFetcher fileFetcher = new LocalFileFetcher(args[0]);
                final Configuration configuration = new Configuration(environment, mainSourceFile, initialArchitecture)
                        .setFileFetcher(fileFetcher);

                // Assemble the source based on this configuration.
                final Assembly assembly = new Assembly(configuration);

                // Assemble the file.
                while (performPass(assembly)) {
                }

                if (assembly.getGravity() != MessageGravity.NONE) {
                    displayMessages(assembly);
                } else {
                    System.out.println("No assembly messages.");
                }

                if (assembly.getGravity().compareTo(MessageGravity.ERROR) >= 0) {
                    System.exit(1);
                }

                outputFile(assembly, "a.out");
            } catch (FileNotFoundException e) {
                System.err.println(args[0] + ": " + e.toString());
                System.exit(1);
            } catch (UnsupportedEncodingException e) {
                System.err.println("The UTF-8 encoding is not supported by this Java runtime.");
                System.exit(1);
            } catch (IOException e) {
                System.err.println(args[0] + ": " + e.toString());
                System.exit(1);
            }
        }
    }

    /**
     * Displays the messages that occurred in the assembly.
     *
     * @param assembly
     *            the assembly
     */
    private static void displayMessages(Assembly assembly) {
        for (AssemblyMessage message : assembly.getMessages()) {
            final AssemblyStep step = message.getStep();

            if (step == null) {
                System.out.printf("(no location): %s: %s%n", getGravityName(message.getGravity()), message.getText());
            } else {
                AssemblyStepLocation location = step.getLocation();
                System.out.printf("%s: %s: %s%n> %s%n", location.getFullPath(), getGravityName(message.getGravity()),
                        message.getText(), location.getSourceLocation().getTextReader().readToString());
            }

            if (message instanceof InternalAssemblerErrorMessage) {
                ((InternalAssemblerErrorMessage) message).getThrowable().printStackTrace();
            }
        }
    }

    /**
     * Gets the description of a message gravity level.
     *
     * @param gravity
     *            the message gravity level
     * @return the description
     */
    private static String getGravityName(MessageGravity gravity) {
        switch (gravity) {
        case NONE:
            return "Message";

        case INFORMATION:
            return "Information";

        case WARNING:
            return "Warning";

        case ERROR:
            return "Error";

        case FATAL_ERROR:
            return "Fatal error";

        default:
            return "Unknown";
        }
    }

    /**
     * Outputs the assembly to a file.
     *
     * @param assembly
     *            the assembly
     * @param fileName
     *            the name of the output file
     * @throws IOException
     *             an I/O exception occurred while working with the output file
     * @throws FileNotFoundException
     *             the output file could not be created or opened for writing
     */
    private static void outputFile(Assembly assembly, String fileName) throws IOException {
        try (FileOutputStream out = new FileOutputStream(fileName)) {
            assembly.writeAssembledDataTo(out);
        }
    }

    /**
     * Performs a pass in the specified assembly.
     *
     * @param assembly
     *            the assembly in which to perform a pass
     * @return <code>true</code> if there are more passes to perform, or <code>false</code> if the assembly is complete.
     */
    private static boolean performPass(Assembly assembly) {
        System.out.printf("Pass %d%n", assembly.getCurrentPass());
        AssemblyCompletionStatus status;

        while ((status = assembly.step()) == AssemblyCompletionStatus.PENDING) {
        }

        return status == AssemblyCompletionStatus.STARTED_NEW_PASS;
    }

}
