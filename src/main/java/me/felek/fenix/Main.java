package me.felek.fenix;

import me.felek.bright.parser.FenixLexer;
import me.felek.bright.parser.FenixParser;
import me.felek.fenix.analyzer.SemanticAnalyzer;
import me.felek.fenix.ast.ASTResult;
import me.felek.fenix.ast.ASTVisitor;
import me.felek.fenix.compiler.Compiler;
import me.felek.fenix.listener.ErrorStrategy;
import me.felek.fenix.utils.OtherUtils;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * @className: ${NAME}
 * @author: Felek
 * @date: 27/10/2026 13:39
 */

public class Main {
    public static void main(String[] args) throws IOException {
        String code = """
                using "java.lang.IO";
                func test(input: Int) -> Int {
                    return if (input >= 5) 5 else 10;
                }
                """;
        List<String> errors = new ArrayList<>();

        var lexer = new FenixLexer(CharStreams.fromString(code));
        lexer.removeErrorListeners();
        lexer.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
                errors.add(OtherUtils.formatError(0x000, line, charPositionInLine, msg));
            }
        });
        var tokens = new CommonTokenStream(lexer);
        var parser = new FenixParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
                errors.add(msg);
            }
        });
        parser.setErrorHandler(new ErrorStrategy());
        FenixParser.ProgramContext parsedCode = parser.program();
        if (!errors.isEmpty()) {
            errors.forEach(System.err::println);
            return;
        }

        ASTVisitor ast = new ASTVisitor();
        ast.visit(parsedCode);
        SemanticAnalyzer sema = new SemanticAnalyzer();
        sema.visit(ast.getResult());

        if (!sema.getErrors().isEmpty()) {
            sema.getErrors().forEach((e) -> System.err.println("[ " + e.getCode() + " ] " + e.getMessage()));
            return;
        }

        Compiler compiler = new Compiler();
        compiler.visit(sema.getResult());
        Files.write(Paths.get(ast.getResult().getModuleName() + ".class"), compiler.bytecode());
    }
}