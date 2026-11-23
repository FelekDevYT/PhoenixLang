package me.felek.fenix.compiler

import me.felek.ast.Expr
import me.felek.ast.decl.ClassDeclaration
import me.felek.ast.decl.clazz.ClassField
import me.felek.ast.decl.clazz.ClassMethod
import me.felek.ast.exprs.ArrayCreateExpr
import me.felek.ast.exprs.ArrayGetExpr
import me.felek.ast.exprs.AssignmentExpression
import me.felek.ast.exprs.BinaryExpr
import me.felek.ast.exprs.BlockExpr
import me.felek.ast.exprs.literals.BoolLiteral
import me.felek.ast.exprs.BreakExpr
import me.felek.ast.exprs.CastExpr
import me.felek.ast.exprs.ContinueExpr
import me.felek.ast.exprs.EqCmpExpr
import me.felek.ast.exprs.literals.FloatLiteral
import me.felek.ast.loop.ForExpr
import me.felek.ast.exprs.GetField
import me.felek.ast.exprs.IdExpr
import me.felek.ast.exprs.IfElseExpr
import me.felek.ast.exprs.IncDecExpr
import me.felek.ast.exprs.literals.IntLiteral
import me.felek.ast.exprs.IsExpr
import me.felek.ast.exprs.LogicExpr
import me.felek.ast.exprs.NewExpr
import me.felek.ast.exprs.ReturnExpr
import me.felek.ast.exprs.SetField
import me.felek.ast.exprs.literals.StrLiteral
import me.felek.ast.exprs.UnaryMinusExpr
import me.felek.ast.exprs.UnaryNotExpr
import me.felek.ast.exprs.VariableDeclaration
import me.felek.ast.exprs.literals.NullLiteral
import me.felek.ast.exprs.util.AssignmentType
import me.felek.ast.exprs.util.BinaryType
import me.felek.ast.exprs.util.ECType
import me.felek.ast.exprs.util.Logic
import me.felek.ast.invoke.InvokeLocalExpr
import me.felek.ast.invoke.InvokeStaticExpr
import me.felek.ast.invoke.InvokeVirtual
import me.felek.ast.loop.DoWhileExpr
import me.felek.ast.loop.WhileExpr
import me.felek.ast.type.ValueType
import me.felek.ast.visitor.Visitor
import me.felek.fenix.ast.ASTResult
import me.felek.fenix.scope.Environment
import me.felek.fenix.scope.FunctionSymbol
import me.felek.fenix.scope.Modifier
import me.felek.fenix.scope.VariableSymbol
import me.felek.fenix.utils.TypeUtils
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.commons.GeneratorAdapter

import org.objectweb.asm.Opcodes.*;
import org.objectweb.asm.Type
import org.objectweb.asm.commons.Method
import java.util.ArrayDeque
import java.util.Deque

/**
 * @className: Compiler
 * @author: Felek
 * @date: 27/10/2026 15:23
 */

class Compiler : Visitor<Unit> {
    private var compiled: MutableMap<String, ByteArray> = mutableMapOf()
    private var cw: ClassWriter = ClassWriter(ClassWriter.COMPUTE_MAXS or ClassWriter.COMPUTE_FRAMES)
    private var ga: GeneratorAdapter? = null
    private var env: Environment = Environment(null)
    private lateinit var moduleName: String
    private var functions: MutableMap<String, FunctionSymbol> = mutableMapOf()
    private var breakQueue: Deque<Label> = ArrayDeque()
    private var continueQueue: Deque<Label> = ArrayDeque()

    override fun visit(expr: IntLiteral) {
        ga!!.push(expr.value)
    }

    override fun visit(expr: FloatLiteral) {
        ga!!.push(expr.value)
    }

    override fun visit(expr: StrLiteral) {
        ga!!.push(expr.value)
    }

    override fun visit(expr: BoolLiteral) {
        ga!!.push(expr.value)
    }

    override fun visit(expr: BlockExpr) {
        val temp: Environment = env
        env = Environment(env)
        for (e in expr.body) {
            visit(e)
        }
        env = temp
    }

    override fun visit(expr: InvokeStaticExpr) {
        for (arg in expr.args) {
            visit(arg)
        }

        if (expr.phoenixMethod != null) {
            val f = expr.phoenixMethod!!
            val sb = StringBuilder(f.returnType.descriptor)
            sb.append(" ").append(expr.method).append("(")
            for (arg in f.args) {
                sb.append(arg.type.descriptor)
            }
            sb.append(")")
            ga!!.invokeStatic(TypeUtils.getAsmType(expr.owner), Method.getMethod(sb.toString()))
        } else {
            ga!!.invokeStatic(TypeUtils.getAsmType(expr.owner), Method.getMethod(expr.methodImpl))
        }
    }

    override fun visit(expr: NullLiteral) {
        ga!!.visitInsn(ACONST_NULL)
    }

    @Suppress("kotlin:S6611")
    override fun visit(expr: InvokeLocalExpr) {
        for (arg in expr.args) {
            visit(arg)
        }
        val f = functions[expr.method]!!

        val sb: StringBuilder = StringBuilder(f.returnType.descriptor)
        sb.append(" " + expr.method)
        sb.append("(")
        for (arg in f.args) {
            sb.append(arg.type.descriptor)
        }
        sb.append(")")

        ga!!.invokeStatic(TypeUtils.getAsmType(moduleName), Method.getMethod(sb.toString()))
    }

    override fun visit(varDecl: VariableDeclaration) {
        val symbol = VariableSymbol(varDecl.name, varDecl.type!!, varDecl.isConst)

        val local = ga!!.newLocal(TypeUtils.getAsmType(varDecl.type!!.name))
        symbol.id = local

        visit(varDecl.value!!)
        ga!!.storeLocal(local)

        env.defineVariable(varDecl.name, symbol)
    }

    override fun visit(decl: ASTResult) {
        cw.visit(V21, ACC_PUBLIC, decl.moduleName, null, "java/lang/Object", null)//Java 21 bytecode support(later, Java 8/11/21/28 support)

        this.moduleName = decl.moduleName

        for (f in decl.functions) {
            functions[f.name] = FunctionSymbol(f.name, f.args, f.returnType, f.modifier)
        }

        for (decl in decl.functions) {
            val sb: StringBuilder = StringBuilder("(")
            for (arg in decl.args) {
                sb.append(arg.type.descriptor)
            }
            sb.append(")")
            sb.append(decl.returnType.descriptor)

            val mv: MethodVisitor = cw.visitMethod(decl.modifier.opcode, decl.name, sb.toString(), null, null)//Java 21 bytecode support
            ga = GeneratorAdapter(mv, decl.modifier.opcode, decl.name, sb.toString())

            ga!!.visitCode()

            val temp: Environment = env
            env = Environment(env)

            for ((index, arg) in decl.args.withIndex()) {
                val vs = VariableSymbol(arg.name, arg.type, true)//every arg is const
                vs.isArgVar = true
                vs.id = index
                env.defineVariable(arg.name, vs)
            }

            visit(decl.body)
            env = temp

            ga!!.visitMaxs(0, 0)
            ga!!.visitEnd()
        }

        cw.visitEnd()
        compiled[decl.moduleName] = cw.toByteArray()

        for (clazz in decl.classes) {
            compiled[clazz.name] = generateClassBytecode(clazz)
        }
    }

    private fun generateClassBytecode(clazz: ClassDeclaration): ByteArray {
        val classCw = ClassWriter(ClassWriter.COMPUTE_MAXS or ClassWriter.COMPUTE_FRAMES)
        classCw.visit(V21, ACC_PUBLIC, clazz.name, null, "Ljava/lang/Object;", null)

        for (mem in clazz.members) {
            if (mem is ClassField) {
                val decl = mem.varDeclaration
                val access = if (mem.modifier == Modifier.PUB) ACC_PUBLIC or ACC_STATIC else ACC_PRIVATE
                classCw.visitField(access, decl.name, decl.type!!.descriptor, null, null).visitEnd()
            }
        }

        val stMv = classCw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null)
        val stGa = GeneratorAdapter(stMv, ACC_STATIC, "<clinit>", "()V")
        stGa.visitCode()

        val temp = env
        env = Environment(env)
        for (mem in clazz.members.filterIsInstance<ClassField>()) {
            val decl = mem.varDeclaration

            ga = stGa
            env = Environment(null)

            visit(decl.value!!)

            stGa.visitFieldInsn(PUTSTATIC, clazz.name, decl.name, decl.type!!.descriptor)
        }
        env = temp
        stGa.visitInsn(RETURN)
        stGa.visitMaxs(0, 0)
        stGa.visitEnd()

        stGa.visitEnd()

        val methVisit: MethodVisitor = classCw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null)
        methVisit.visitCode()
        methVisit.visitVarInsn(ALOAD, 0)
        methVisit.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V")
        methVisit.visitInsn(RETURN)
        methVisit.visitMaxs(0, 0)
        methVisit.visitEnd()

        for (mem in clazz.members) {
            if (mem is ClassMethod) {
                val f = mem.func
                val sb = StringBuilder("(")
                for (arg in f.args) {
                    sb.append(arg.type.descriptor)
                }
                sb.append(")")
                sb.append(f.returnType.descriptor)

                val mv = classCw.visitMethod(f.modifier.opcode, f.name, sb.toString(), null, null)
                val methGa = GeneratorAdapter(mv, f.modifier.opcode, f.name, sb.toString())
                methGa.visitCode()

                val temp = env
                val tempGa = ga
                env = Environment(env)
                ga = methGa

                for ((index, arg) in f.args.withIndex()) {
                    val vs = VariableSymbol(arg.name, arg.type, true)
                    vs.isArgVar = true
                    vs.id = index
                    env.defineVariable(arg.name, vs)
                }

                visit(f.body)

                ga = tempGa
                env = temp

                methGa.visitMaxs(0, 0)
                methGa.visitEnd()
            }
        }

        return classCw.toByteArray()
    }

    override fun visit(id: IdExpr) {
        val variable: VariableSymbol = env.lookupVariable(id.id)
        if (variable.isArgVar) {
            ga!!.loadArg(variable.id)
        } else {
            ga!!.loadLocal(variable.id)
        }
    }

    override fun visit(ret: ReturnExpr) {
        if (ret.expr != null) {
            visit(ret.expr!!)
        }
        ga!!.returnValue()
    }

    override fun visit(bin: BinaryExpr) {
        visit(bin.a)
        visit(bin.b)

        ga!!.math(when(bin.op) {
            BinaryType.REM -> GeneratorAdapter.REM
            BinaryType.MUL -> GeneratorAdapter.MUL
            BinaryType.DIV -> GeneratorAdapter.DIV
            BinaryType.ADD -> GeneratorAdapter.ADD
            BinaryType.SUB -> GeneratorAdapter.SUB
        }, bin.type)
    }

    override fun visit(ifelse: IfElseExpr) {
        val end: Label = ga!!.newLabel()
        val elseLbl: Label = ga!!.newLabel()

        visitJump(ifelse.ifExpr, elseLbl)
        visit(ifelse.ifStmt)
        ga!!.goTo(end)
        ga!!.mark(elseLbl)
        if (ifelse.elseStmt != null) {
            visit(ifelse.elseStmt!!)
        }
        ga!!.mark(end)
    }

    override fun visit(eqCmp: EqCmpExpr) {
        val op: Int = when(eqCmp.ec) {
            ECType.BT -> GeneratorAdapter.LE
            ECType.BE -> GeneratorAdapter.LT
            ECType.EQ -> GeneratorAdapter.NE
            ECType.NEQ -> GeneratorAdapter.EQ
            ECType.LE -> GeneratorAdapter.GT
            ECType.LT -> GeneratorAdapter.GE
        }

        val end: Label = ga!!.newLabel()
        val elseLbl: Label = ga!!.newLabel()

        visit(eqCmp.left)
        visit(eqCmp.right)
        ga!!.ifCmp(eqCmp.type, op, elseLbl)
        ga!!.push(true)
        ga!!.goTo(end)
        ga!!.mark(elseLbl)
        ga!!.push(false)
        ga!!.mark(end)
    }

    override fun visit(whileExpr: WhileExpr) {
        val lblStart: Label = ga!!.newLabel()
        val lblEnd: Label = ga!!.newLabel()

        breakQueue.push(lblEnd)
        continueQueue.push(lblStart)

        ga!!.mark(lblStart)
        visitJump(whileExpr.condition, lblEnd)
        visit(whileExpr.statement)
        ga!!.goTo(lblStart)
        ga!!.mark(lblEnd)

        breakQueue.pop()
        continueQueue.pop()
    }

    override fun visit(loop: DoWhileExpr) {
        val lblStart: Label = ga!!.newLabel()
        val lblCondition: Label = ga!!.newLabel()
        val lblEnd: Label = ga!!.newLabel()

        breakQueue.push(lblEnd)
        continueQueue.push(lblCondition)

        ga!!.mark(lblStart)
        visit(loop.statement)
        ga!!.mark(lblCondition)
        visitJump(loop.condition, lblEnd)
        ga!!.goTo(lblStart)
        ga!!.mark(lblEnd)

        breakQueue.pop()
        continueQueue.pop()
    }

    override fun visit(forExpr: ForExpr) {
        val start: Label = ga!!.newLabel()
        val end: Label = ga!!.newLabel()

        breakQueue.push(end)
        continueQueue.push(start)

        val temp = env
        env = Environment(env)

        if (forExpr.initializer != null) {
            visit(forExpr.initializer!!)
        }

        ga!!.mark(start)
        if (forExpr.condition != null) {
            visitJump(forExpr.condition!!, end)
        }

        visit(forExpr.body)
        if (forExpr.increment != null) {
            visit(forExpr.increment!!)
        }
        ga!!.goTo(start)
        ga!!.mark(end)

        env = temp

        breakQueue.pop()
        continueQueue.pop()
    }

    override fun visit(breakExpr: BreakExpr) {
        ga!!.goTo(breakQueue.peek())
    }

    override fun visit(continueExpr: ContinueExpr) {
        ga!!.goTo(continueQueue.peek())
    }

    override fun visit(castExpr: CastExpr) {
        visit(castExpr.value)

        ga!!.cast(castExpr.fromAsmType, Type.getType(castExpr.toType.descriptor))
    }

    override fun visit(arrayGet: ArrayGetExpr) {
        visit(arrayGet.left)

        var desc = arrayGet.firstValue.descriptor
        for (index in arrayGet.indexes) {
            visit(index)
            ga!!.arrayLoad(Type.getType(desc.removePrefix("[")))
            desc = desc.removePrefix("[")
        }
    }

    override fun visit(assignment: AssignmentExpression) {
        if (assignment.indexes.isNotEmpty()) {
            ga!!.loadLocal(env.lookupVariable(assignment.name).id)
            var desc = assignment.firstValue.descriptor
            for (i in 0 until assignment.indexes.size - 1) {
                visit(assignment.indexes[i])
                ga!!.arrayLoad(Type.getType(desc.removePrefix("[")))
                desc = desc.removePrefix("[")
            }

            visit(assignment.indexes.last())

            val type = when (assignment.type) {
                AssignmentType.ADD -> GeneratorAdapter.ADD
                AssignmentType.DIV -> GeneratorAdapter.DIV
                AssignmentType.SUB -> GeneratorAdapter.SUB
                AssignmentType.MUL -> GeneratorAdapter.MUL
                AssignmentType.REM -> GeneratorAdapter.REM
                AssignmentType.EQ -> GeneratorAdapter.EQ
            }

            if (type != GeneratorAdapter.EQ) {
                ga!!.dup2()
                ga!!.arrayLoad(Type.getType(desc))
                visit(assignment.value)
                ga!!.math(type, Type.getType(assignment.variableValue.descriptor))
            } else {
                visit(assignment.value)
            }

            ga!!.arrayStore(Type.getType(desc.removePrefix("[")))
        } else {
            val type = when (assignment.type) {
                AssignmentType.ADD -> GeneratorAdapter.ADD
                AssignmentType.DIV -> GeneratorAdapter.DIV
                AssignmentType.SUB -> GeneratorAdapter.SUB
                AssignmentType.MUL -> GeneratorAdapter.MUL
                AssignmentType.REM -> GeneratorAdapter.REM
                AssignmentType.EQ -> GeneratorAdapter.EQ
            }

            if (type != GeneratorAdapter.EQ) {
                ga!!.loadLocal(env.lookupVariable(assignment.name).id)
                visit(assignment.value)
                ga!!.math(type, Type.getType(assignment.variableValue.descriptor))
            } else {
                visit(assignment.value)
            }
            ga!!.storeLocal(env.lookupVariable(assignment.name).id)
        }
    }

    override fun visit(arrayCreate: ArrayCreateExpr) {
        if (arrayCreate.indexes.size == 1) {
            visit(arrayCreate.indexes[0])
            ga!!.newArray(Type.getType(arrayCreate.type.descriptor))
        } else {
            //>=1
            for (index in arrayCreate.indexes) {
                visit(index)
            }

            ga!!.visitMultiANewArrayInsn("[".repeat(arrayCreate.indexes.size) + arrayCreate.type.descriptor, arrayCreate.indexes.size)
        }
    }

    override fun visit(unary: UnaryNotExpr) {
        visit(unary.expr)
        ga!!.not()
    }

    override fun visit(unary: UnaryMinusExpr) {
        visit(unary.expr)

        if (unary.type == ValueType.INT) {
            ga!!.visitInsn(INEG)
        } else {
            ga!!.visitInsn(FNEG)
        }
    }

    override fun visit(inc: IncDecExpr) {
        val variable = env.lookupVariable(inc.varName)
        if (inc.isIncrement) {
            ga!!.iinc(variable.id, 1)
        } else {
            ga!!.iinc(variable.id, -1)
        }
    }

    override fun visit(new: NewExpr) {
        if (new.isClass) {
            ga!!.visitTypeInsn(NEW, new.owner)
            ga!!.visitInsn(DUP)

            ga!!.visitMethodInsn(INVOKESPECIAL, new.owner, "<init>", "()V", false)
        } else {
            ga!!.newInstance(Type.getType(new.clazz))
            ga!!.visitInsn(DUP)

            for (arg in new.args) {
                visit(arg)
            }

            ga!!.invokeConstructor(Type.getType(new.clazz), Method("<init>", new.constructorDescriptor))
        }
    }

    override fun visit(logic: LogicExpr) {
        when (logic.op) {
            Logic.OR -> {
                val end: Label = ga!!.newLabel()
                val trueLbl: Label = ga!!.newLabel()

                visit(logic.left)
                ga!!.ifZCmp(GeneratorAdapter.NE, trueLbl)
                visit(logic.right)
                ga!!.ifZCmp(GeneratorAdapter.NE, trueLbl)
                ga!!.push(false)
                ga!!.goTo(end)
                ga!!.mark(trueLbl)
                ga!!.push(true)
                ga!!.mark(end)
            }
            Logic.AND -> {
                val end: Label = ga!!.newLabel()
                val falseLbl: Label = ga!!.newLabel()

                visit(logic.left)
                ga!!.ifZCmp(GeneratorAdapter.EQ, falseLbl)
                visit(logic.right)
                ga!!.ifZCmp(GeneratorAdapter.EQ, falseLbl)
                ga!!.push(true)
                ga!!.goTo(end)
                ga!!.mark(falseLbl)
                ga!!.push(false)
                ga!!.mark(end)
            }
            Logic.XOR -> {
                val end: Label = ga!!.newLabel()
                val falseLbl: Label = ga!!.newLabel()

                visit(logic.left)
                visit(logic.right)
                ga!!.ifCmp(Type.getType(ValueType.BOOL.descriptor), GeneratorAdapter.NE, falseLbl)
                ga!!.push(false)
                ga!!.goTo(end)
                ga!!.mark(falseLbl)
                ga!!.push(true)
                ga!!.mark(end)
            }
        }
    }

    override fun visit(logic: IsExpr) {
        visit(logic.left)
        ga!!.instanceOf(Type.getType(logic.isType.descriptor))
    }

    override fun visit(expr: InvokeVirtual) {
        visit(expr.owner)

        for (arg in expr.args) {
            visit(arg)
        }

        val owner = Class.forName(expr.asmType.className)
        if (owner.isInterface) {
            ga!!.invokeInterface(expr.asmType, expr.asmMethod)
        } else {
            ga!!.invokeVirtual(expr.asmType, expr.asmMethod)
        }
    }

    override fun visit(field: GetField) {
        if (field.phoenixField != null) {
            visit(field.left)
            ga!!.visitFieldInsn(GETFIELD, field.ownerClass, field.name, field.phoenixField!!.type.descriptor)
            return
        }

        if (field.isStatic) {
            ga!!.getStatic(Type.getType(field.clazz), field.name, Type.getType(field.field!!.type.descriptorString()))
            return
        }

        visit(field.left)
        ga!!.getField(Type.getType(field.clazz), field.name, Type.getType(field.field!!.type.descriptorString()))
    }

    override fun visit(field: SetField) {
        if (field.phoenixField != null) {
            visit(field.left)
            visit(field.right)
            ga!!.visitFieldInsn(PUTFIELD, field.ownerClass, field.id, field.phoenixField!!.type.descriptor)
            return
        }

        if (field.isStatic) {
            visit(field.right)
            ga!!.putStatic(Type.getType(field.clazz), field.id, Type.getType(field.field!!.type.descriptorString()))
            return
        }

        visit(field.left)
        visit(field.right)
        ga!!.putField(Type.getType(field.clazz), field.id, Type.getType(field.field!!.type.descriptorString()))
    }

    override fun visit(expr: Expr) {
        return when (expr) {
            is InvokeStaticExpr -> visit(expr)
            is IntLiteral -> visit(expr)
            is FloatLiteral -> visit(expr)
            is BoolLiteral -> visit(expr)
            is StrLiteral -> visit(expr)
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
            is SetField -> visit(expr)
            is GetField -> visit(expr)
            is DoWhileExpr -> visit(expr)
            is NullLiteral -> visit(expr)
            else -> throw RuntimeException("No expression found for " + expr.javaClass.name)
        }
    }

    fun getCompiledClasses(): Map<String, ByteArray> {
        return compiled
    }

    fun visitJump(e: Expr, falseLbl: Label) {
        if (e is EqCmpExpr) {
            visit(e.left)
            visit(e.right)
            ga!!.ifCmp(e.type, when (e.ec) {
                ECType.BT -> GeneratorAdapter.LE
                ECType.BE -> GeneratorAdapter.LT
                ECType.EQ -> GeneratorAdapter.NE
                ECType.NEQ -> GeneratorAdapter.EQ
                ECType.LE -> GeneratorAdapter.GT
                ECType.LT -> GeneratorAdapter.GE
            }, falseLbl)
        } else {
            visit(e)
            ga!!.ifZCmp(GeneratorAdapter.EQ, falseLbl)
        }
    }
}