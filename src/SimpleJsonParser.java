import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Simple custom JSON parser used for save/load functionality
class SimpleJsonParser {
    private final String text;
    private int pos = 0;

    public SimpleJsonParser(String text) {
        this.text = text == null ? "" : text;
    }

    public Object parse() {
        skipWhitespace();
        Object value = parseValue();
        skipWhitespace();
        return value;
    }

    private Object parseValue() {
        skipWhitespace();

        if (pos >= text.length()) return null;

        char c = text.charAt(pos);

        if (c == '"') return parseString();
        if (c == '{') return parseObject();
        if (c == '[') return parseArray();
        if (c == 't' || c == 'f') return parseBoolean();
        if (c == 'n') return parseNull();

        return parseNumber();
    }

    private Map<String, Object> parseObject() {
        Map<String, Object> map = new LinkedHashMap<>();
        expect('{');
        skipWhitespace();

        if (peek() == '}') {
            pos++;
            return map;
        }

        while (true) {
            skipWhitespace();
            String key = parseString();

            skipWhitespace();
            expect(':');

            Object value = parseValue();
            map.put(key, value);

            skipWhitespace();

            char c = peek();

            if (c == ',') {
                pos++;
                continue;
            }

            if (c == '}') {
                pos++;
                break;
            }

            break;
        }

        return map;
    }

    private List<Object> parseArray() {
        List<Object> list = new ArrayList<>();
        expect('[');
        skipWhitespace();

        if (peek() == ']') {
            pos++;
            return list;
        }

        while (true) {
            Object value = parseValue();
            list.add(value);

            skipWhitespace();

            char c = peek();

            if (c == ',') {
                pos++;
                continue;
            }

            if (c == ']') {
                pos++;
                break;
            }

            break;
        }

        return list;
    }

    private String parseString() {
        StringBuilder sb = new StringBuilder();
        expect('"');

        while (pos < text.length()) {
            char c = text.charAt(pos++);

            if (c == '"') break;

            if (c == '\\' && pos < text.length()) {
                char next = text.charAt(pos++);

                switch (next) {
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/' -> sb.append('/');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        if (pos + 4 <= text.length()) {
                            String hex = text.substring(pos, pos + 4);
                            sb.append((char) Integer.parseInt(hex, 16));
                            pos += 4;
                        }
                    }
                    default -> sb.append(next);
                }
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    private Boolean parseBoolean() {
        if (text.startsWith("true", pos)) {
            pos += 4;
            return true;
        }

        if (text.startsWith("false", pos)) {
            pos += 5;
            return false;
        }

        return false;
    }

    private Object parseNull() {
        if (text.startsWith("null", pos)) {
            pos += 4;
        }

        return null;
    }

    private Number parseNumber() {
        int start = pos;

        while (pos < text.length()) {
            char c = text.charAt(pos);

            if ((c >= '0' && c <= '9') || c == '-' || c == '+' || c == '.') {
                pos++;
            } else {
                break;
            }
        }

        String number = text.substring(start, pos);

        try {
            if (number.contains(".")) {
                return Double.parseDouble(number);
            }

            return Integer.parseInt(number);

        } catch (Exception e) {
            return 0;
        }
    }

    private void skipWhitespace() {
        while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) {
            pos++;
        }
    }

    private char peek() {
        if (pos >= text.length()) return '\0';
        return text.charAt(pos);
    }

    private void expect(char expected) {
        skipWhitespace();

        if (pos < text.length() && text.charAt(pos) == expected) {
            pos++;
        }
    }
}
