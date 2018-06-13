
package cz.habarta.typescript.generator;


public class Logger {

    private final Level level;

    public enum Level {
        Debug, Verbose, Info, Warning, Error;
    }

    public Logger() {
        this(null);
    }

    public Logger(Level level) {
        this.level = level != null ? level : Level.Verbose;
    }

    protected void write(Level level, String message) {
        if (level.compareTo(this.level) >= 0) {
            System.out.println(message);
        }
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
