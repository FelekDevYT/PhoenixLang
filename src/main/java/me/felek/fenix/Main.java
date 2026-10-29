package me.felek.fenix;

import me.felek.bright.parser.FenixLexer;
import me.felek.bright.parser.FenixParser;
import me.felek.fenix.analyzer.SemanticAnalyzer;
import me.felek.fenix.ast.ASTResult;
import me.felek.fenix.ast.ASTVisitor;
import me.felek.fenix.compiler.Compiler;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @className: ${NAME}
 * @author: Felek
 * @date: 27/10/2026 13:39
 */

public class Main {
    public static void main(String[] args) throws IOException {
        String code = """
                using "java.lang.IO";
                
                func factorial(a: Int) -> Int {
                    if (a == 1) {
                        return 1;
                    };
                    
                    return a * factorial(a - 1);
                }
                
                func main(args: Str[]) {
                    factorial(15);
                }
                """;
        var lexer = new FenixLexer(CharStreams.fromString(code));
        var tokens = new CommonTokenStream(lexer);
        var parser = new FenixParser(tokens);

        FenixParser.ProgramContext parsedCode = parser.program();
        ASTVisitor ast = new ASTVisitor();
        ast.visit(parsedCode);
        SemanticAnalyzer sema = new SemanticAnalyzer();
        sema.visit(ast.getResult());
        Compiler compiler = new Compiler();
        compiler.visit(sema.getResult());
        Files.write(Paths.get(ast.getResult().getModuleName() + ".class"), compiler.bytecode());
    }
}