
package cz.habarta.typescript.generator;


public class Logger {

    protected enum Level {
        Verbose, Info, Warning, Error;
    }

    protected void write(Level level, String message) {
        System.out.println(message);
    }

    public final void verbose(String message) {
        write(Level.Verbose, message);
    }

    public final void info(String message) {
        write(Level.Info, message);
    }

    public final void warning(String message) {
        write(Level.Warning, "Warning: " + message);
    }

    public final void error(String message) {
        write(Level.Error, "Error: " + message);
    }

}
