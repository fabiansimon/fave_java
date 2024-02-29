import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Parser {
    private static class ParseError extends RuntimeException {};
    private final List<Token> tokens;
    private int curr = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isEnd()) {
            statements.add(declaration());
        }

        return statements;
    }

    private Expr expression() {
        return assignment();
    }

    private Stmt declaration() {
        try {
            if (isMatch(TokenType.CLASS)) return classDeclaration();
            if (isMatch(TokenType.FUN)) return function("function");
            if (isMatch(TokenType.VAR)) return varDeclaration();

            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt classDeclaration() {
        Token name = consume(TokenType.IDENTIFIER, "Exepct class name.");
        consume(TokenType.LEFT_BRACE, "Expect '{' before class body.");

        List<Stmt.Function> methods = new ArrayList<>();
        while (!check(TokenType.RIGHT_BRACE) && !isEnd()) {
            methods.add((Stmt.Function) function("method")); // possible bug?
        }

        consume(TokenType.RIGHT_BRACE, "Expect '}' after class body.");

        return new Stmt.Class(name, methods);
    }

    private Stmt statement() {
        if (isMatch(TokenType.FOR)) return forStatement();
        if (isMatch(TokenType.IF)) return ifStatement();
        if (isMatch(TokenType.RETURN)) return returnStatement();
        if (isMatch(TokenType.PRINT)) return printStatement();
        if (isMatch(TokenType.WHILE)) return whileStatement();
        if (isMatch((TokenType.LEFT_BRACE))) return new Stmt.Block(block());
        return expressionStatement();
    }

    private Stmt returnStatement() {
        Token keyword = previous();
        Expr value = null;
        if (!check(TokenType.SEMICOLON)) {
            value = expression();
        }

        consume(TokenType.SEMICOLON, "Expect ';' after return value");
        return new Stmt.Return(keyword, value);
    }
    private Stmt forStatement() {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'for'.");
        Stmt initalizer;
        if (isMatch(TokenType.SEMICOLON)) {
            initalizer = null;
        } else if (isMatch(TokenType.VAR)) {
            initalizer = varDeclaration();
        } else {
            initalizer = expressionStatement();
        }

        Expr condition = null;
        if (!check(TokenType.SEMICOLON)) {
            condition = expression();
        }
        consume(TokenType.SEMICOLON, "Expect ';' after loop condition.");

        Expr increment = null;
        if (!check(TokenType.SEMICOLON)) {
            increment = expression();
        }
        consume(TokenType.RIGHT_PAREN, "Expect ')' after for clauses");

        Stmt body = statement();

        if (increment != null) {
            body = new Stmt.Block(
                    Arrays.asList(
                            body,
                            new Stmt.Expression(increment)
                    )
            );
        }

        if (condition == null) condition = new Expr.Literal(true);
        body = new Stmt.While(condition, body);

        if (initalizer != null)
            body = new Stmt.Block(Arrays.asList(initalizer, body));

        return body;
    }

    private Stmt ifStatement() {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'if'.");
        Expr condition = expression();
        consume(TokenType.RIGHT_PAREN, "Expect ')' after if condition.");

        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (isMatch(TokenType.ELSE)) {
            elseBranch = statement();
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt printStatement() {
        Expr val = expression();
        consume(TokenType.SEMICOLON, "Expect ';' after value");
        return new Stmt.Print(val);
    }

    private Stmt varDeclaration() {
        Token name = consume(TokenType.IDENTIFIER, "Expect variable name.");

        Expr initializer = null;
        if (isMatch(TokenType.EQUAL))
            initializer = expression();

        consume(TokenType.SEMICOLON, "Expect ';' after variable declaration");
        return new Stmt.Var(name, initializer);
    }

    private Stmt whileStatement() {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'while'.");
        Expr condition = expression();
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'while'.");
        Stmt body = statement();

        return new Stmt.While(condition, body);
    }

    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(TokenType.SEMICOLON, "Expect ';' after expression.");

        return new Stmt.Expression(expr);
    }

    private Stmt function(String kind) {
        Token name = consume(TokenType.IDENTIFIER, "Expect " + kind + " name.");
        consume(TokenType.LEFT_PAREN ,"Expect '(', after " + kind + " name.");
        List<Token> parameters = new ArrayList<>();
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (parameters.size() > 255) {
                    error(peek(), "Can't have more than 255 parameters");
                }

                parameters.add(consume(TokenType.IDENTIFIER, "Expect parameter name."));
            } while (isMatch(TokenType.COMMA));
        }

        consume(TokenType.RIGHT_PAREN, "Expect ')' after parameters");
        consume(TokenType.LEFT_BRACE, "Expect '{' before " + kind + " body.");
        List<Stmt> body = block();
        return new Stmt.Function(name, parameters, body);
    }

    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();

        while (!check(TokenType.RIGHT_BRACE) && !isEnd()) {
            statements.add(declaration());
        }

        consume(TokenType.RIGHT_BRACE, "Expect '}' after block");
        return statements;
    }

    private Expr assignment() {
        Expr expr = or();

        if (isMatch(TokenType.EQUAL)) {
            Token equals = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable) expr).name;
                return new Expr.Assign(name, value);
            }

            if (expr instanceof Expr.Get) {
                Expr.Get get = (Expr.Get) expr;
                return new Expr.Set(get.object, get.name, value);
            }

            error(equals, "Invalid assignment target.");
        }

        return expr;
    }

    private Expr or() {
        Expr expr = and();

        while (isMatch(TokenType.OR)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    private Expr and() {
        Expr expr = equality();

        while (isMatch(TokenType.AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
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

        return call();
    }

    private Expr finishCall(Expr callee) {
        List<Expr> args = new ArrayList<>();
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (args.size() >= 255) {
                    error(peek(), "Can't have more than 255 arguments.");
                }
                args.add(expression());
            } while (isMatch(TokenType.COMMA));
        }

        Token paren = consume(TokenType.RIGHT_PAREN,
                "Expect ')' after arguments.");

        return new Expr.Call(callee, paren, args);
    }

    private Expr call() {
        Expr expr = primary();

        while (true) {
            if (isMatch(TokenType.LEFT_PAREN)) {
                expr = finishCall(expr);
            } else if (isMatch(TokenType.DOT)) {
                Token name = consume(TokenType.IDENTIFIER, "Expect property name after '.'.");
                expr = new Expr.Get(expr, name);
            } else {
                break;
            }
        }

        return expr;
    }

    private Expr primary() {
        if (isMatch(TokenType.FALSE)) return new Expr.Literal(false);
        if (isMatch(TokenType.TRUE)) return new Expr.Literal(true);
        if (isMatch(TokenType.NIL)) return new Expr.Literal(null);

        if (isMatch(TokenType.NUMBER, TokenType.STRING))
            return new Expr.Literal(previous().literal);

        if (isMatch(TokenType.THIS)) return new Expr.This(previous());

        if (isMatch(TokenType.IDENTIFIER))
            return new Expr.Variable(previous());

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
