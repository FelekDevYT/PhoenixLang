package me.felek.fenix.analyzer

import io.github.classgraph.ClassGraph
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
import me.felek.ast.exprs.GetField
import me.felek.ast.exprs.IdExpr
import me.felek.ast.exprs.IfElseExpr
import me.felek.ast.exprs.IncDecExpr
import me.felek.ast.exprs.IntExpr
import me.felek.ast.exprs.IsExpr
import me.felek.ast.exprs.LogicExpr
import me.felek.ast.exprs.NewExpr
import me.felek.ast.exprs.ReturnExpr
import me.felek.ast.exprs.SetField
import me.felek.ast.exprs.StrExpr
import me.felek.ast.exprs.UnaryMinusExpr
import me.felek.ast.exprs.UnaryNotExpr
import me.felek.ast.exprs.VariableDeclaration
import me.felek.ast.invoke.InvokeLocalExpr
import me.felek.ast.invoke.InvokeStaticExpr
import me.felek.ast.invoke.InvokeVirtual
import me.felek.ast.loop.WhileExpr
import me.felek.ast.type.ValueType
import me.felek.ast.visitor.Visitor
import me.felek.fenix.ast.ASTResult
import me.felek.fenix.scope.Environment
import me.felek.fenix.scope.FunctionSymbol
import me.felek.fenix.utils.OtherUtils
import me.felek.fenix.utils.TypeUtils
import org.objectweb.asm.Type
import org.objectweb.asm.commons.Method
import java.lang.reflect.Modifier
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
        val className = OtherUtils.resolveImport(expr.owner, result.importMap)
        val clazz = try {
            Class.forName(className)
        } catch (_: ClassNotFoundException) {
            errors.add(Error(expr.line, expr.column, 0xB00, "Java class not found: $className"))
            return ValueType.VOID
        }

        val args: List<ValueType> = expr.args.map { visit(it) }
        var method: java.lang.reflect.Method? = null
        for (meth in clazz.methods) {
            if (!Modifier.isStatic(meth.modifiers)) {
                continue
            }
            if (meth.name == expr.method && meth.parameterCount == args.size) {
                for ((index ,param) in meth.parameterTypes.withIndex()) {
                    if (param.descriptorString() == "Ljava/lang/Object;" || TypeUtils.typeByDescriptor(param.descriptorString())  == expr.args[index]) {
                        method = meth
                    }
                }
            }
        }

        if (method == null) {
            errors.add(Error(expr.line, expr.column, 0x313, "Method not found"))
            return ValueType.VOID
        }

        expr.methodImpl = method
        return TypeUtils.typeByDescriptor(method.returnType.descriptorString())
    }

    override fun visit(varDecl: VariableDeclaration): ValueType {
        val type = if (varDecl.value != null) visit(varDecl.value!!) else null
        var explicitType: ValueType? = null
        if (varDecl.type != null) {
            explicitType = resolveType(varDecl.type!!)
        }

        val finality = explicitType ?: type ?: ValueType.VOID
        if (finality == ValueType.VOID) {
            errors.add(Error(varDecl.line, varDecl.column, 0x221, "Void can not be used as variable type"))
            return ValueType.VOID
        }

        if (explicitType != null && type != null && explicitType != type) {
            errors.add(Error(varDecl.line, varDecl.column, 0x200, "Incompatible types: $explicitType and $type"))
        }

        varDecl.type = finality
        if (env.variables.containsKey(varDecl.name)) {
            errors.add(Error(varDecl.line, varDecl.column, 0x102, "Redefinition of variable in one scope"))
        } else {
            env.defineVariable(varDecl.name, finality, varDecl.isConst)
        }

        return finality
    }

    override fun visit(logic: IsExpr): ValueType {
        visit(logic.left)
        logic.isType = resolveType(logic.isType)

        return ValueType.BOOL
    }

    override fun visit(id: IdExpr): ValueType {
        val variable = env.lookupVariable(id.id)
        if (variable == null) {
            errors.add(Error(id.line, id.column, 0x120, "Unknown name " + id.id))
            return ValueType.VOID
        }
        return resolveType(variable.type)
    }

    override fun visit(ret: ReturnExpr): ValueType {
        if (ret.expr != null) {
            val real = visit(ret.expr!!)
            if (real!= currentFunction!!.returnType) {
                errors.add(Error(ret.line, ret.column, 0x322, "Wrong function return: expected $real but found ${currentFunction!!.returnType}"))
            }
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
            val result = ValueType(
                varType.descriptor.removePrefix("[".repeat(assignment.indexes.size)),
                varType.name.removeSuffix("[]".repeat(assignment.indexes.size)),
                varType.dimensions - assignment.indexes.size
            )

            if (result != out) {
                errors.add(Error(assignment.line, assignment.column, 0x200, "Incompatible types: $result and $out"))
            }

            assignment.firstValue = variable.type
        }
        assignment.variableValue = out

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
            errors.add(Error(new.line, new.column, 0xB00, "Java class not found: ${OtherUtils.resolveImport(new.owner, result.importMap)}"))
        }

        return ValueType.VOID
    }

    override fun visit(logic: LogicExpr): ValueType {
        val left = visit(logic.left)
        val right = visit(logic.right)

        if (left != ValueType.BOOL || right != ValueType.BOOL) {
            errors.add(Error(logic.line, logic.column, 0x201, "Logic operators only can be applied to boolean values"))
        }

        return ValueType.BOOL
    }

    override fun visit(expr: InvokeVirtual): ValueType {
        if (expr.owner !is ArrayGetExpr && expr.owner !is IdExpr && expr.owner !is InvokeLocalExpr && expr.owner !is GetField) {
            errors.add(Error(expr.line, expr.column, 0x201, "Virtual invoke only applicable to arrayGet, local function call or variable"))
        }
        val desc = visit(expr.owner)
        expr.asmType = Type.getType(desc.descriptor)

        val clazz = try {
            Class.forName(OtherUtils.resolveImport(desc.name, result.importMap))
        } catch (_: ClassNotFoundException) {
            errors.add(Error(expr.line, expr.column, 0xB00, "Java class not found: ${OtherUtils.resolveImport(desc.name, result.importMap)}"))
            return ValueType.VOID
        }

        val args: List<ValueType> = expr.args.map { visit(it) }
        val method = clazz.methods.find {
            it.name == expr.method &&
                    it.parameterCount == args.size &&
                    it.parameterTypes.zip(args).all { (p, a) ->
                        p.descriptorString() == "Ljava/lang/Object;" || TypeUtils.typeByDescriptor(p.descriptorString()) == a
                    }
        }

        if (method == null) {
            errors.add(Error(expr.line, expr.column, 0x313, "Method not found"))
            return ValueType.VOID
        }

        expr.asmMethod = Method.getMethod(method)

        return TypeUtils.typeByDescriptor(method.returnType.descriptorString())
    }

    override fun visit(field: GetField): ValueType {
        if (field.left is IdExpr && env.lookupVariable((field.left as IdExpr).id) == null) {
            val name = OtherUtils.resolveImport((field.left as IdExpr).id, result.importMap)

            try {
                field.clazz = Class.forName(name)
            } catch (_: ClassNotFoundException) {
                errors.add(Error(field.line, field.column, 0xB00, "Java class not found: $name"))
                return ValueType.VOID
            }

            try {
                field.field = field.clazz!!.getField(field.name)
            } catch (_: NoSuchFieldException) {
                errors.add(Error(field.line, field.column, 0x314, "Field not found: ${field.name}"))
                return ValueType.VOID
            }

            if (!Modifier.isStatic(field.field!!.modifiers)) {
                errors.add(Error(field.line, field.column, 0x316, "Instance field ${field.name} can not be accessed from class"))
                return ValueType.VOID
            }

            field.isStatic = true
            return TypeUtils.typeByDescriptor(field.field!!.type.descriptorString())
        }

        val owner: ValueType = visit(field.left)

        val name = OtherUtils.resolveImport(owner.name, result.importMap)
        try {
            field.clazz = Class.forName(name)
        } catch (_: ClassNotFoundException) {
            errors.add(Error(field.line, field.column, 0xB00, "Java class not found: $name"))
            return ValueType.VOID
        }

        try {
            field.field = field.clazz!!.getField(field.name)
        } catch (_: ClassNotFoundException) {
            errors.add(Error(field.line, field.column, 0x314, "Field not found: ${field.name}"))
            return ValueType.VOID
        }

        return TypeUtils.typeByDescriptor(field.field!!.type.descriptorString())
    }

    override fun visit(field: SetField): ValueType {
        if (field.left is IdExpr && env.lookupVariable((field.left as IdExpr).id) == null) {
            val name = OtherUtils.resolveImport((field.left as IdExpr).id, result.importMap)
            try {
                field.clazz = Class.forName(name)
            } catch (_: ClassNotFoundException) {
                errors.add(Error(field.line, field.column, 0xB00, "Java class not found: $name"))
                return ValueType.VOID
            }

            try {
                field.field = field.clazz!!.getField(field.id)
            } catch (_: NoSuchFieldException) {
                errors.add(Error(field.line, field.column, 0x314, "Field not found: ${field.id}"))
                return ValueType.VOID
            }

            if (!Modifier.isStatic(field.field!!.modifiers)) {
                errors.add(Error(field.line, field.column, 0x316, "Instance field ${field.id} can not be assigned from class"))
                return ValueType.VOID
            }
            if (Modifier.isFinal(field.field!!.modifiers)) {
                errors.add(Error(field.line, field.column, 0x110, "Constant can not be assigned"))
                return ValueType.VOID
            }

            val value: ValueType = visit(field.right)
            val type: ValueType = TypeUtils.typeByDescriptor(field.field!!.type.descriptorString())
            if (value != type) {
                errors.add(Error(field.line, field.column, 0x200, "Incompatible types: $value and $type"))
            }

            field.isStatic = true
            return value
        }

        val owner: ValueType = visit(field.left)
        val value: ValueType = visit(field.right)

        if (owner == ValueType.VOID && field.left !is IdExpr) {
            return ValueType.VOID
        }

        val name = OtherUtils.resolveImport(owner.name, result.importMap)
        try {
            field.clazz = Class.forName(name)
        } catch (_: ClassNotFoundException) {
            errors.add(Error(field.line, field.column, 0xB00, "Java class not found: $name"))
            return ValueType.VOID
        }

        try {
            field.field = field.clazz!!.getField(field.id)
        } catch (_: ClassNotFoundException) {
            errors.add(Error(field.line, field.column, 0x314, "Field not found: ${field.id}"))
            return ValueType.VOID
        }

        val type: ValueType = TypeUtils.typeByDescriptor(field.field!!.type.descriptorString())
        if (value != type) {
            errors.add(Error(field.line, field.column, 0x200, "Incompatible types: $value and $type"))
        }

        return value
    }

    override fun visit(expr: Expr): ValueType {
        return when (expr) {
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
            is LogicExpr -> visit(expr)
            is IsExpr -> visit(expr)
            is InvokeVirtual -> visit(expr)
            is InvokeStaticExpr -> visit(expr)
            is GetField -> visit(expr)
            is SetField -> visit(expr)
            else -> {
                errors.add(Error(expr.line, expr.column, 0xF00, "Internal Compiler Error: Unknown expression ${expr.javaClass.name}"))
                ValueType.VOID
            }
        }
    }

    override fun visit(decl: ASTResult): ValueType {
        result = decl

        //java.lang default imports
        val javaClasses: Set<Class<*>> = ClassGraph().enableClassInfo().enableSystemJars().enableSystemModules().acceptPackagesNonRecursive("java.lang").scan().use { it.allClasses.map { Class.forName(it.name) }.toSet() }
        for (clazz in javaClasses) {
            result.importMap[clazz.simpleName] = clazz.canonicalName
        }

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

            functions[f.name] = FunctionSymbol(f.name, f.args, f.returnType, f.modifier)
        }

        for (f in decl.functions) {
            val temp: Environment = env
            env = Environment(env)

            for ((name, type) in f.args) {
                env.defineVariable(name, type, true)
            }

            currentFunction = FunctionSymbol(f.name, f.args, f.returnType, f.modifier)

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

        val name = base.substring(1, base.length - 1).replace("/", ".")
        val resolvedName = result.importMap.getOrDefault(name, name)

        val newDescriptor = "[".repeat(type.dimensions) + "L${resolvedName.replace(".", "/")};"
        return ValueType(newDescriptor, resolvedName + "[]".repeat(type.dimensions), type.dimensions)
    }
}