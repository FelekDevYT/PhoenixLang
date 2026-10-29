package me.felek.fenix.analyzer

import me.felek.ast.Expr
import me.felek.ast.exprs.AssignmentExpression
import me.felek.ast.exprs.BinaryExpr
import me.felek.ast.exprs.BlockExpr
import me.felek.ast.exprs.BoolExpr
import me.felek.ast.exprs.EqCmpExpr
import me.felek.ast.exprs.FloatExpr
import me.felek.ast.exprs.ForExpr
import me.felek.ast.exprs.IdExpr
import me.felek.ast.exprs.IfElseExpr
import me.felek.ast.exprs.IntExpr
import me.felek.ast.exprs.ReturnExpr
import me.felek.ast.exprs.StrExpr
import me.felek.ast.exprs.VariableDeclaration
import me.felek.ast.invoke.InvokeLocalExpr
import me.felek.ast.invoke.InvokeStaticExpr
import me.felek.ast.loop.WhileExpr
import me.felek.ast.type.ValueType
import me.felek.ast.visitor.Visitor
import me.felek.fenix.ast.ASTResult
import me.felek.fenix.scope.Environment
import me.felek.fenix.scope.FunctionSymbol
import me.felek.fenix.utils.OtherUtils
import me.felek.fenix.utils.TypeUtils
import org.objectweb.asm.Type

/**
 * @className: SemanticAnalyzer
 * @author: Felek
 * @date: 27/10/2026 16:11
 */

class SemanticAnalyzer : Visitor<ValueType> {
    private lateinit var result: ASTResult
    private var env: Environment = Environment(null);//root
    private var errors: MutableList<Error> = mutableListOf()
    private var functions: MutableMap<String, FunctionSymbol> = mutableMapOf()
    private var currentFunction: FunctionSymbol? = null

    override fun visit(expr: IntExpr): ValueType {
        return ValueType.INT
    }

    override fun visit(expr: FloatExpr): ValueType {
        return ValueType.FLOAT
    }

    override fun visit(expr: StrExpr): ValueType {
        return ValueType.STRING
    }

    override fun visit(expr: BoolExpr): ValueType {
        return ValueType.BOOL
    }

    override fun visit(expr: BlockExpr): ValueType {
        val temp: Environment = env
        env = Environment(env)

        var last: ValueType = ValueType.VOID
        for (el in expr.body.indices) {
            last = visit(expr.body[el])
        }

        env = temp
        return last
    }

    override fun visit(expr: InvokeStaticExpr): ValueType {
        try {
            val clazz = Class.forName(OtherUtils.resolveImport(expr.owner, result.importMap))
            val argTypes = expr.args.map { visit(it) }
            expr.methodImpl = clazz.methods.find {
                it.name.equals(expr.method) && it.parameterCount == argTypes.size && it.parameterTypes.zip(argTypes).all { (p, a) -> p.descriptorString().equals("Ljava/lang/Object;") || TypeUtils.typeByDescriptor(p.descriptorString()) == a }
            }!!//TODO: 0x311 implementation
        } catch (exc: ClassNotFoundException) {
            errors.add(Error(0xB00, "Java class not found"))
            return ValueType.VOID
        }

        return TypeUtils.typeByDescriptor(expr.methodImpl.returnType.descriptorString())
    }

    override fun visit(varDecl: VariableDeclaration): ValueType {
        if (varDecl.value == null) {
            if (!varDecl.isConst) {
                errors.add(Error(0x101, "Variable without initializer"))
            } else {
                errors.add(Error(0x100, "Constant without initializer"))
            }
        }

        val type = visit(varDecl.value!!)

        if (type == ValueType.VOID) {
            errors.add(Error(0x221, "Void can not be used as variable type"))
        }

        if (varDecl.type == null) {
            varDecl.type = type
        } else {
            if (varDecl.type != type) {
                errors.add(Error(0x200, "Incompatible types"))
            }
        }

        if (env.variables.containsKey(varDecl.name)) {
            errors.add(Error(0x102, "Redefinition of variable in one scope"))
            return varDecl.type!!
        }

        env.defineVariable(varDecl.name, varDecl.type, varDecl.isConst)

        return varDecl.type!!
    }

    override fun visit(id: IdExpr): ValueType {
        if (env.lookupVariable(id.id) == null) {
            errors.add(Error(0x120, "Unknown name " + id.id))
            return ValueType.VOID
        }

        return env.lookupVariable(id.id).type
    }

    override fun visit(ret: ReturnExpr): ValueType {
        if (visit(ret.expr) != currentFunction!!.returnType) {
            errors.add(Error(0x322, "Wrong function return"))
        }
        return ValueType.VOID
    }

    override fun visit(bin: BinaryExpr): ValueType {
        val typeA = visit(bin.a)
        val typeB = visit(bin.b)

        if (typeA == ValueType.INT && typeB == ValueType.FLOAT || typeA == ValueType.FLOAT && typeB == ValueType.INT) {
            errors.add(Error(0x204, "Int and Float can't be used without casting"))
        }

        if (!TypeUtils.isBinaryComparable(typeA, typeB)) {
            errors.add(Error(0x201, "Operator is not applicable to type"))
        }

        if (typeA != typeB) {
            errors.add(Error(0x200, "Incompatible types"))
        }

        bin.type = Type.getType(visit(bin.b).descriptor)
        return visit(bin.a)
    }

    override fun visit(ifelse: IfElseExpr): ValueType {
        if (visit(ifelse.ifExpr) != ValueType.BOOL) {
            errors.add(Error(0x203, "Non-bool condition value"))
        }
        var output = visit(ifelse.ifStmt)
        if(ifelse.elseStmt != null) {
            if (visit(ifelse.elseStmt!!) != output) {
                errors.add(Error(0x401, "if and else blocks must have same types!"))
            }
        }

        return output
    }

    override fun visit(eqCmp: EqCmpExpr): ValueType {
        visit(eqCmp.left)
        val type = visit(eqCmp.right)

        eqCmp.type = Type.getType(type.descriptor)

        return ValueType.BOOL
    }

    override fun visit(expr: InvokeLocalExpr): ValueType {
        if (!functions.containsKey(expr.method)) {
            if (env.lookupVariable(expr.method) != null) {
                errors.add(Error(0x312, "Call of variable as function"))
                return ValueType.VOID
            }
            errors.add(Error(0x330, "Unknown function: " + expr.method))
            return ValueType.VOID
        }
        val func = functions.get(expr.method)!!

        for ((index, arg) in expr.args.withIndex()) {
            if (visit(arg) != func.args[index]) {
                errors.add(Error(0x311, "Wrong function argument"))
            }
        }

        return func.returnType
    }

    override fun visit(eqCmp: WhileExpr): ValueType {
        if (visit(eqCmp.condition) != ValueType.BOOL) {
            errors.add(Error(0x203, "Non-bool condition"))
        }

        return visit(eqCmp.statement)
    }

    override fun visit(assignment: AssignmentExpression): ValueType {
        var variable = env.lookupVariable(assignment.name)
        if (variable == null) {
            errors.add(Error(0x120, "Unknown name"))
        }
        if (variable.isConst) {
            errors.add(Error(0x110, "Constant can not be assigned"))
        }

        return visit(assignment.value)
    }

    override fun visit(forExpr: ForExpr): ValueType {
        val temp: Environment = env
        env = Environment(env)

        visit(forExpr.initializer)
        if (visit(forExpr.condition) != ValueType.BOOL) {
            errors.add(Error(0x203, "Non-bool condition"))
        }
        visit(forExpr.increment)

        val out = visit(forExpr.body)

        env = temp

        return out
    }

    override fun visit(expr: Expr): ValueType {
        return when (expr) {
            is InvokeStaticExpr -> visit(expr)
            is IntExpr -> visit(expr)
            is FloatExpr -> visit(expr)
            is BoolExpr -> visit(expr)
            is StrExpr -> visit(expr)
            is BlockExpr -> visit(expr)
            is VariableDeclaration -> visit(expr)
            is IdExpr -> visit(expr)
            is ReturnExpr -> visit(expr)
            is BinaryExpr -> visit(expr)
            is IfElseExpr -> visit(expr)
            is EqCmpExpr -> visit(expr)
            is InvokeLocalExpr -> visit(expr)
            is WhileExpr -> visit(expr)
            is AssignmentExpression -> visit(expr)
            is ForExpr -> visit(expr)
            else -> {
                errors.add(Error(0xF00, "Internal Compiler Error: Unknown expression ${expr.javaClass.name}"))
                ValueType.VOID
            }
        }
    }

    override fun visit(decl: ASTResult): ValueType {
        result = decl

        for (f in decl.functions) {
            if (functions.containsKey(f.name)) {
                errors.add(Error(0x300, "Double function declaration"))
                continue
            }

            val usedNames: MutableList<String> = mutableListOf()
            for (arg in f.args) {
                if (usedNames.contains(arg.name)) {
                    errors.add(Error(0x301, "Double function argument name"))
                    continue
                }
                usedNames.add(arg.name)
            }

            functions[f.name] = FunctionSymbol(f.name, f.args, f.returnType)
        }

        for (f in decl.functions) {
            val temp: Environment = env
            env = Environment(env)

            for (arg in f.args) {
                env.defineVariable(arg.name, arg.type, true)
            }

            currentFunction = FunctionSymbol(f.name, f.args, f.returnType)

            visit(f.body)
            env = temp
        }

        return ValueType.VOID
    }

    fun getResult(): ASTResult {
        return result
    }

    fun getErrors(): List<Error> {
        return errors
    }
}