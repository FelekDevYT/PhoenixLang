package me.felek.fenix.analyzer

import me.felek.ast.Expr
import me.felek.ast.exprs.ArrayCreateExpr
import me.felek.ast.exprs.ArrayGetExpr
import me.felek.ast.exprs.AssignmentExpression
import me.felek.ast.exprs.BinaryExpr
import me.felek.ast.exprs.BlockExpr
import me.felek.ast.exprs.BoolExpr
import me.felek.ast.exprs.BreakExpr
import me.felek.ast.exprs.CastExpr
import me.felek.ast.exprs.ContinueExpr
import me.felek.ast.exprs.EqCmpExpr
import me.felek.ast.exprs.FloatExpr
import me.felek.ast.exprs.ForExpr
import me.felek.ast.exprs.IdExpr
import me.felek.ast.exprs.IfElseExpr
import me.felek.ast.exprs.IncDecExpr
import me.felek.ast.exprs.IntExpr
import me.felek.ast.exprs.NewExpr
import me.felek.ast.exprs.ReturnExpr
import me.felek.ast.exprs.StrExpr
import me.felek.ast.exprs.UnaryMinusExpr
import me.felek.ast.exprs.UnaryNotExpr
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
import java.util.ArrayDeque

/**
 * @className: SemanticAnalyzer
 * @author: Felek
 * @date: 27/10/2026 16:11
 */

class SemanticAnalyzer : Visitor<ValueType> {
    private lateinit var result: ASTResult
    private var env: Environment = Environment(null)//root
    private var errors: MutableList<Error> = mutableListOf()
    private var functions: MutableMap<String, FunctionSymbol> = mutableMapOf()
    private var currentFunction: FunctionSymbol? = null
    private var continueQueue: ArrayDeque<Boolean> = ArrayDeque()
    private var breakQueue: ArrayDeque<Boolean> = ArrayDeque()

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
                it.name == expr.method && it.parameterCount == argTypes.size && it.parameterTypes.zip(argTypes).all { (p, a) -> p.descriptorString().equals("Ljava/lang/Object;") || TypeUtils.typeByDescriptor(p.descriptorString()) == a }
            }!!//TODO: 0x311 implementation
        } catch (_: ClassNotFoundException) {
            errors.add(Error(expr.line, expr.column, 0xB00, "Java class not found"))
            return ValueType.VOID
        }

        return TypeUtils.typeByDescriptor(expr.methodImpl.returnType.descriptorString())
    }

    override fun visit(varDecl: VariableDeclaration): ValueType {
        if (varDecl.value == null && varDecl.type == null) {
            return if (!varDecl.isConst) {
                errors.add(Error(varDecl.line, varDecl.column, 0x101, "Variable without initializer"))
                ValueType.VOID
            } else {
                errors.add(Error(varDecl.line, varDecl.column, 0x100, "Constant without initializer"))
                ValueType.VOID
            }
        }

        val type = visit(varDecl.value!!)

        if (type == ValueType.VOID) {
            errors.add(Error(varDecl.line, varDecl.column, 0x221, "Void can not be used as variable type"))
        }

        if (varDecl.type == null) {
            varDecl.type = type
        } else {
            varDecl.type = resolveType(varDecl.type!!)
            if (varDecl.type != type) {
                errors.add(Error(varDecl.line, varDecl.column, 0x200, "Incompatible types: ${varDecl.type} and $type"))
            }
        }

        if (env.variables.containsKey(varDecl.name)) {
            errors.add(Error(varDecl.line, varDecl.column, 0x102, "Redefinition of variable in one scope"))
            return varDecl.type!!
        }

        env.defineVariable(varDecl.name, varDecl.type, varDecl.isConst)

        return varDecl.type!!
    }

    override fun visit(id: IdExpr): ValueType {
        if (env.lookupVariable(id.id) == null) {
            errors.add(Error(id.line, id.column, 0x120, "Unknown name " + id.id))
            return ValueType.VOID
        }

        return env.lookupVariable(id.id).type
    }

    override fun visit(ret: ReturnExpr): ValueType {
        val real = visit(ret.expr)
        if (real!= currentFunction!!.returnType) {
            errors.add(Error(ret.line, ret.column, 0x322, "Wrong function return: expected $real but found ${currentFunction!!.returnType}"))
        }
        return ValueType.VOID
    }

    override fun visit(bin: BinaryExpr): ValueType {
        val typeA = visit(bin.a)
        val typeB = visit(bin.b)

        if (typeA == ValueType.INT && typeB == ValueType.FLOAT || typeA == ValueType.FLOAT && typeB == ValueType.INT) {
            errors.add(Error(bin.line, bin.column, 0x204, "Int and Float can't be used without casting"))
        }

        if (!TypeUtils.isBinaryComparable(typeA, typeB)) {
            errors.add(Error(bin.line, bin.column, 0x201, "Operator is not applicable to type"))
        }

        if (typeA != typeB) {
            errors.add(Error(bin.line, bin.column, 0x200, "Incompatible types"))
        }

        bin.type = Type.getType(visit(bin.b).descriptor)
        return visit(bin.a)
    }

    override fun visit(ifelse: IfElseExpr): ValueType {
        if (visit(ifelse.ifExpr) != ValueType.BOOL) {
            errors.add(Error(ifelse.line, ifelse.column, 0x203, "Non-bool condition value"))
        }
        val output = visit(ifelse.ifStmt)
        if(ifelse.elseStmt != null && visit(ifelse.elseStmt!!) != output) {
            errors.add(Error(ifelse.line, ifelse.column, 0x401, "if and else blocks must have same types!"))
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
                errors.add(Error(expr.line, expr.column, 0x312, "Call of variable as function"))
                return ValueType.VOID
            }
            errors.add(Error(expr.line, expr.column, 0x330, "Unknown function: " + expr.method))
            return ValueType.VOID
        }
        val func = functions[expr.method]

        for ((index, arg) in expr.args.withIndex()) {
            if (visit(arg) != func!!.args[index]) {
                errors.add(Error(expr.line, expr.column, 0x311, "Wrong function argument"))
            }
        }

        return func!!.returnType
    }

    override fun visit(whileExpr: WhileExpr): ValueType {
        if (visit(whileExpr.condition) != ValueType.BOOL) {
            errors.add(Error(whileExpr.line, whileExpr.column, 0x203, "Non-bool condition"))
        }

        breakQueue.push(true)
        continueQueue.push(true)
        val result = visit(whileExpr.statement)
        breakQueue.pop()
        continueQueue.pop()

        return result
    }

    override fun visit(assignment: AssignmentExpression): ValueType {
        val variable = env.lookupVariable(assignment.name)
        if (variable == null) {
            errors.add(Error(assignment.line, assignment.column, 0x120, "Unknown name"))
        }
        if (variable.isConst) {
            errors.add(Error(assignment.line, assignment.column, 0x110, "Constant can not be assigned"))
        }
        val out = visit(assignment.value)

        if (assignment.indexes.isNotEmpty()) {
            val varType = variable.type
            val result = ValueType(varType.descriptor.removePrefix("[".repeat(assignment.indexes.size)), varType.name.removeSuffix("[]".repeat(assignment.indexes.size)),varType.dimensions - assignment.indexes.size)

            if (result != out) {
                errors.add(Error(assignment.line, assignment.column, 0x200, "Incompatible types: $result and $out"))
            }

            assignment.firstValue = variable.type
        }

        return out
    }

    override fun visit(inc: IncDecExpr): ValueType {
        val variable = env.lookupVariable(inc.varName)
        if (variable == null) {
            errors.add(Error(inc.line, inc.column, 0x120, "Unknown name"))
        }

        if (variable.type != ValueType.INT && variable.type != ValueType.FLOAT) {
            errors.add(Error(inc.line, inc.column, 0x201, "Increment only can be applied to Int or Float"))
        }

        return variable.type
    }

    override fun visit(forExpr: ForExpr): ValueType {
        breakQueue.push(true)
        continueQueue.push(true)
        val temp: Environment = env
        env = Environment(env)

        visit(forExpr.initializer)
        if (visit(forExpr.condition) != ValueType.BOOL) {
            errors.add(Error(forExpr.line, forExpr.column, 0x203, "Non-bool condition"))
        }
        visit(forExpr.increment)

        val out = visit(forExpr.body)

        env = temp
        breakQueue.pop()
        continueQueue.pop()

        return out
    }

    override fun visit(breakExpr: BreakExpr): ValueType {
        if (breakQueue.isEmpty()) {
            errors.add(Error(breakExpr.line, breakExpr.column, 0x410, "Break outside loop"))
        }

        return ValueType.VOID
    }

    override fun visit(continueExpr: ContinueExpr): ValueType {
        if (continueQueue.isEmpty()) {
            errors.add(Error(continueExpr.line, continueExpr.column, 0x411, "Continue outside loop"))
        }

        return ValueType.VOID
    }

    override fun visit(castExpr: CastExpr): ValueType {
        val out = visit(castExpr.value)

        if (!TypeUtils.canCast(out, castExpr.toType)) {
            errors.add(Error(castExpr.line, castExpr.column, 0x20A, "Unallowed cast"))
        }

        castExpr.fromAsmType = Type.getType(out.descriptor)

        return TypeUtils.getCastReturn(out, castExpr.toType)
    }

    override fun visit(arrayGet: ArrayGetExpr): ValueType {
        val value = visit(arrayGet.left)
        arrayGet.firstValue = value

        if (value.dimensions == 0) {
            errors.add(Error(arrayGet.line, arrayGet.column, 0x511, "Trying to index non-array"))
            return ValueType.VOID
        }
        if (value.dimensions < arrayGet.indexes.size) {
            errors.add(Error(arrayGet.line, arrayGet.column, 511, "Trying to get a lot of dimensions"))
            return ValueType.VOID
        }

        var desc = value.descriptor
        for (index in arrayGet.indexes) {
            if (desc.startsWith("[")) desc = desc.substring(1)
        }

        return ValueType(desc, value.name.removeSuffix("[]".repeat(arrayGet.indexes.size)), value.dimensions - arrayGet.indexes.size)
    }

    override fun visit(arrayCreate: ArrayCreateExpr): ValueType {
        for (el in arrayCreate.indexes) {
            if (visit(el) != ValueType.INT) {
                errors.add(Error(arrayCreate.line, arrayCreate.column, 0x516, "Non-int array index"))
            }
        }

        return ValueType("[".repeat(arrayCreate.indexes.size) + resolveType(arrayCreate.type).descriptor, resolveType(arrayCreate.type).name + "[]".repeat(arrayCreate.indexes.size), arrayCreate.indexes.size)
    }

    override fun visit(unary: UnaryMinusExpr): ValueType {
        val type = visit(unary.expr)

        if (!(type == ValueType.INT || type == ValueType.FLOAT)) {
            errors.add(Error(unary.line, unary.column, 0x201, "Unary minus only can be applied to Int and Float"))
        }
        unary.type = type

        return type
    }

    override fun visit(unary: UnaryNotExpr): ValueType {
        val type = visit(unary.expr)

        if (type != ValueType.BOOL) {
            errors.add(Error(unary.line, unary.column, 0x201, "Not only can be applied to boolean"))
        }

        return type
    }

    override fun visit(new: NewExpr): ValueType {
        try {
            val clazz: Class<*> = Class.forName(OtherUtils.resolveImport(new.owner, result.importMap))
            new.clazz = clazz

            val descriptor: StringBuilder = StringBuilder("(")
            for (arg in new.args) {
                descriptor.append(visit(arg).descriptor)
            }
            descriptor.append(")")
            descriptor.append("V")

            new.constructorDescriptor = descriptor.toString()

            return TypeUtils.typeByDescriptor(clazz.descriptorString())
        } catch (_: ClassNotFoundException) {
            errors.add(Error(new.line, new.column, 0xB00, "Java class not found"))
        }

        return ValueType.VOID
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
            is BreakExpr -> visit(expr)
            is ContinueExpr -> visit(expr)
            is CastExpr -> visit(expr)
            is ArrayGetExpr -> visit(expr)
            is ArrayCreateExpr -> visit(expr)
            is UnaryNotExpr -> visit(expr)
            is UnaryMinusExpr -> visit(expr)
            is IncDecExpr -> visit(expr)
            is NewExpr -> visit(expr)
            else -> {
                errors.add(Error(expr.line, expr.column, 0xF00, "Internal Compiler Error: Unknown expression ${expr.javaClass.name}"))
                ValueType.VOID
            }
        }
    }

    override fun visit(decl: ASTResult): ValueType {
        result = decl

        for (f in decl.functions) {
            f.returnType = resolveType(f.returnType)
            for (arg in f.args) {
                arg.type = resolveType(arg.type)
            }

            if (functions.containsKey(f.name)) {
                errors.add(Error(f.line, f.column, 0x300, "Double function declaration"))
                continue
            }

            val usedNames: MutableList<String> = mutableListOf()
            for ((name) in f.args) {
                if (usedNames.contains(name)) {
                    errors.add(Error(f.line, f.column, 0x301, "Double function argument name"))
                    continue
                }
                usedNames.add(name)
            }

            functions[f.name] = FunctionSymbol(f.name, f.args, f.returnType)
        }

        for (f in decl.functions) {
            val temp: Environment = env
            env = Environment(env)

            for ((name, type) in f.args) {
                env.defineVariable(name, type, true)
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

    private fun resolveType(type: ValueType): ValueType {
        val base = type.descriptor.removePrefix("[".repeat(type.dimensions))
        if (!base.startsWith("L") || base == "Ljava/lang/String;") {
            return type
        }

        val desc = base.substring(1, base.length - 1).replace("/", ".")
        val value = if (desc.contains(".")) desc else OtherUtils.resolveImport(desc, result.importMap)
        if (desc == value) {
            return type
        }

        return ValueType("[".repeat(type.dimensions) + "L${value.replace(".", "/")};", value + "[]".repeat(type.dimensions), type.dimensions)
    }
}