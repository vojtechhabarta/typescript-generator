
package cz.habarta.typescript.generator.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import java.io.IOException;


/**
 * Jackson2 PrettyPrinter implementation that produces JSON format similar to JSON.stringify() output.
 */
public class StandardJsonPrettyPrinter extends DefaultPrettyPrinter {
    private static final long serialVersionUID = 1;

    private final String indent;
    private final String eol;

    public StandardJsonPrettyPrinter() {
        this("    ", String.format("%n"));
    }

    public StandardJsonPrettyPrinter(String indent, String eol) {
        this.indent = indent;
        this.eol = eol;
        final DefaultIndenter indenter = new DefaultIndenter(indent, eol);
        this._arrayIndenter = indenter;
        this._objectIndenter = indenter;
    }

    @Override
    public DefaultPrettyPrinter createInstance() {
        return new StandardJsonPrettyPrinter(indent, eol);
    }

    @Override
    public void writeObjectFieldValueSeparator(JsonGenerator jg) throws IOException {
        jg.writeRaw(": ");
    }

    @Override
    public void writeEndObject(JsonGenerator g, int nrOfEntries) throws IOException {
        if (!_objectIndenter.isInline()) {
            --_nesting;
        }
        if (nrOfEntries > 0) {
            _objectIndenter.writeIndentation(g, _nesting);
        }
        g.writeRaw('}');
    }

    @Override
    public void writeEndArray(JsonGenerator g, int nrOfValues) throws IOException {
        if (!_arrayIndenter.isInline()) {
            --_nesting;
        }
        if (nrOfValues > 0) {
            _arrayIndenter.writeIndentation(g, _nesting);
        }
        g.writeRaw(']');
    }

}
