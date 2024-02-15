import javax.swing.text.DefaultEditorKit;
import java.util.List;

public class Parser {
    private static class ParseError extends RuntimeException {};
    private final List<Token> tokens;
    private int curr = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    Expr parse() {
        try {
            return expression();
        } catch (ParseError err) {
            return null;
        }
    }

    private Expr expression() {
        return equality();
    }

    private Expr equality() {
        Expr expr = comparison();

        while (isMatch(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr comparison() {
        Expr expr = term();

        while (isMatch(TokenType.GREATER,
                TokenType.GREATER_EQUAL,
                TokenType.LESS,
                TokenType.LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr term() {
        Expr expr = factor();

        while (isMatch(TokenType.MINUS, TokenType.PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr factor() {
        Expr expr = unary();

        while (isMatch(TokenType.SLASH, TokenType.STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr unary() {
        if (isMatch(TokenType.BANG, TokenType.MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        return primary();
    }

    private Expr primary() {
        if (isMatch(TokenType.FALSE)) return new Expr.Literal(false);
        if (isMatch(TokenType.TRUE)) return new Expr.Literal(true);
        if (isMatch(TokenType.NIL)) return new Expr.Literal(null);

        if (isMatch(TokenType.NUMBER, TokenType.STRING))
            return new Expr.Literal(previous().literal);

        if (isMatch(TokenType.LEFT_PAREN)) {
            Expr expr = expression();
            consume(TokenType.RIGHT_PAREN, "Expect ')' after expression");
            return new Expr.Grouping(expr);
        }


        throw error(peek(), "Expect expression");
    }

    private boolean isMatch(TokenType ...types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }

        return false;
    }

    private Token consume(TokenType type, String errorMessage) {
        if (check(type)) return advance();
        throw error(peek(), errorMessage);
    }

    private boolean check(TokenType type) {
        if (isEnd()) return false;
        return peek().type == type;
    }

    private Token advance() {
        if (!isEnd()) curr++;
        return previous();
    }

    private boolean isEnd() {
        return peek().type == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(curr);
    }

    private Token peek(int skip) {
        return tokens.get(curr + skip);
    }

    private Token previous() {
        if (curr > 0) return peek(-1);
        return null;
    }

    private ParseError error(Token token, String errorMessage) {
        Fave.error(token, errorMessage);
        return new ParseError();
    }

    private void synchronize() {
        advance();

        while (!isEnd()) {
            if (previous().type == TokenType.SEMICOLON) return;

            switch (peek().type) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }

            advance();
        }
    }
}
