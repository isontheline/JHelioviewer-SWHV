package org.helioviewer.jhv.gui.components.base;

import javax.swing.JFormattedTextField;
import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.JFormattedTextField.AbstractFormatterFactory;
import javax.swing.text.DefaultFormatter;
import java.text.ParseException;

import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.log.Log;

@SuppressWarnings("serial")
public class TerminatedFormatterFactory extends AbstractFormatterFactory {

    private final TerminatedFormatter formatter;

    public TerminatedFormatterFactory(String _format, String _terminator, double _min, double _max) {
        formatter = new TerminatedFormatter(_format, _terminator, _min, _max);
    }

    @Override
    public AbstractFormatter getFormatter(JFormattedTextField tf) {
        return formatter;
    }

    private static class TerminatedFormatter extends DefaultFormatter {

        private final String format;
        private final String terminator;
        private final double min;
        private final double max;

        TerminatedFormatter(String _format, String _terminator, double _min, double _max) {
            format = _format;
            terminator = _terminator;

            if (_min > _max)
                _max = _min;
            min = _min;
            max = _max;
            setOverwriteMode(false);
        }

        @Override
        public Object stringToValue(String string) throws ParseException {
            double value = 0;
            if (string != null) {
                int t = string.indexOf(terminator);
                if (t > 0)
                    string = string.substring(0, t);

                try {
                    value = Double.valueOf(string);
                } catch (NumberFormatException e) {
                    throw new ParseException("Could not parse number: " + string, 0);
                }
            }
            return MathUtils.clip(value, min, max);
        }

        @Override
        public String valueToString(Object value) {
            return String.format(format, value) + terminator;
        }

        @Override
        public Class<?> getValueClass() {
            return Double.class;
        }

    }

}
