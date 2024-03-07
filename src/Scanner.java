import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Scanner {
    private final String src;
    private final List<Token> tokens;
    private final HashMap<String, TokenType> keywords = new HashMap<>() {{
        put("and",    TokenType.AND);
        put("class",  TokenType.CLASS);
        put("else",   TokenType.ELSE);
        put("false",  TokenType.FALSE);
        put("for",    TokenType.FOR);
        put("fun",    TokenType.FUN);
        put("if",     TokenType.IF);
        put("nil",    TokenType.NIL);
        put("or",     TokenType.OR);
        put("print",  TokenType.PRINT);
        put("return", TokenType.RETURN);
        put("super",  TokenType.SUPER);
        put("this",   TokenType.THIS);
        put("true",   TokenType.TRUE);
        put("var",    TokenType.VAR);
        put("while",  TokenType.WHILE);
        put("min",  TokenType.MIN);
        put("max",  TokenType.MAX);
    }};
    private int start = 0;
    private int curr = 0;
    private int line = 1;
    public Scanner(String src) {
        this.src = src;
        this.tokens = new ArrayList<>();
    }

   List<Token> scanTokens() {
        while (!isEnd()) {
            start = curr;
            scanToken();
        }

        tokens.add(new Token(TokenType.EOF, "", null, line));
        return tokens;
    }

    private void scanToken() {
        char c = advance();

        if (isDigit(c)) {
            addDigit();
            return;
        }

        switch (c) {
            case '(': addToken(TokenType.LEFT_PAREN); break;
            case ')': addToken(TokenType.RIGHT_PAREN); break;
            case '{': addToken(TokenType.LEFT_BRACE); break;
            case '}': addToken(TokenType.RIGHT_BRACE); break;
            case ',': addToken(TokenType.COMMA); break;
            case '.': addToken(TokenType.DOT); break;
            case '-': addToken(TokenType.MINUS); break;
            case '+': addToken(TokenType.PLUS); break;
            case ';': addToken(TokenType.SEMICOLON); break;
            case '*': addToken(TokenType.STAR); break;
            case '!':
                addToken(isMatch('=') ? TokenType.BANG_EQUAL : TokenType.BANG);
                break;
            case '=':
                addToken(isMatch('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL);
                break;
            case '<':
                addToken(isMatch('=') ? TokenType.LESS_EQUAL : TokenType.LESS);
                break;
            case '>':
                addToken(isMatch('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER);
                break;
            case '/': {
                if (isMatch('/'))
                    skipLineComment();
                else if (isMatch('*'))
                    skipBlockComment();
                else
                    addToken(TokenType.SLASH);
                break;
            }
            case '"': addString(); break;

            case ' ':
            case '\r':
            case '\t':
                break;

            case '\n':
                line++;
                break;

            default: {
                if (isAlpha(c)) {
                    addIdentifier();
                    return;
                }

                Fave.error(line, "Unexpected Character");
                break;
            }
        }
    }

    private void addString() {
        while (peek() != '"' && !isEnd()) {
            if (peek() == '\n') line++;
            advance();
        }
        if (isEnd()) {
            Fave.error(line, "Unterminated string.");
            return;
        }

        advance(); // The closing ""
        String str = src.substring(start+1, curr-1);
        addToken(TokenType.STRING, str);
    }

    private void addDigit() {
        while (isDigit(peek())) advance();

        if (peek() == '.' && isDigit(peek(1))) {
            advance();

            while (isDigit(peek())) advance();
        }

        Double number = Double.parseDouble(src.substring(start, curr));
        addToken(TokenType.NUMBER, number);
    }

    private void addIdentifier() {
        while (isAlphaNumeric(peek())) advance();

        String str = src.substring(start, curr);
        TokenType type = keywords.get(str);
        if (type == null) type = TokenType.IDENTIFIER;

        addToken(type);
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = src.substring(start, curr);
        tokens.add(new Token(type, text, literal, line));
    }

    private char advance() {
        return src.charAt(curr++);
    }

    private char peek() {
        if (isEnd()) return '\0';
        return src.charAt(curr);
    }

    private char peek(int skip) {
        if (curr + skip >= src.length()) return '\0';
        return src.charAt(curr + skip);
    }

    private void skipLineComment() {
        while (peek() != '\n' && !isEnd()) advance();
    }

    private void skipBlockComment() {
        while (!isEnd() && !(peek() == '*' && peek(1) == '/')) {
            if (advance() == '\n') line++;
        }
        if (!isEnd()) curr += 2;
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAlpha(char c) {
        return  (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                 c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private boolean isMatch(char expected) {
        if (isEnd()) return false;
        if (src.charAt(curr) != expected) return false;

        curr++;
        return true;
    }

    private boolean isEnd() {
        return curr >= src.length();
    }

}
