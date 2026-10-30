package me.felek.fenix.compiler

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
import me.felek.ast.exprs.IntExpr
import me.felek.ast.exprs.ReturnExpr
import me.felek.ast.exprs.StrExpr
import me.felek.ast.exprs.VariableDeclaration
import me.felek.ast.exprs.util.BinaryType
import me.felek.ast.exprs.util.ECType
import me.felek.ast.invoke.InvokeLocalExpr
import me.felek.ast.invoke.InvokeStaticExpr
import me.felek.ast.loop.WhileExpr
import me.felek.ast.type.ValueType
import me.felek.ast.visitor.Visitor
import me.felek.fenix.ast.ASTResult
import me.felek.fenix.scope.Environment
import me.felek.fenix.scope.FunctionSymbol
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
    private var cw: ClassWriter = ClassWriter(ClassWriter.COMPUTE_MAXS or ClassWriter.COMPUTE_FRAMES)
    private var ga: GeneratorAdapter? = null
    private var env: Environment = Environment(null)
    private lateinit var moduleName: String
    private var functions: MutableMap<String, FunctionSymbol> = mutableMapOf()
    private var breakQueue: Deque<Label> = ArrayDeque()
    private var continueQueue: Deque<Label> = ArrayDeque()

    override fun visit(expr: IntExpr) {
        ga!!.push(expr.value)
    }

    override fun visit(expr: FloatExpr) {
        ga!!.push(expr.value)
    }

    override fun visit(expr: StrExpr) {
        ga!!.push(expr.value)
    }

    override fun visit(expr: BoolExpr) {
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

        ga!!.invokeStatic(TypeUtils.getAsmType(expr.owner), Method.getMethod(expr.methodImpl))
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
            functions[f.name] = FunctionSymbol(f.name, f.args, f.returnType)
        }

        for (decl in decl.functions) {
            val sb: StringBuilder = StringBuilder("(")
            for (arg in decl.args) {
                sb.append(arg.type!!.descriptor)
            }
            sb.append(")")
            sb.append(decl.returnType.descriptor)

            val mv: MethodVisitor = cw.visitMethod(ACC_PUBLIC or ACC_STATIC, decl.name, sb.toString(), null, null)//Java 21 bytecode support
            ga = GeneratorAdapter(mv, ACC_PUBLIC or ACC_STATIC, decl.name, sb.toString())

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
        visit(ret.expr)
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

        visit(ifelse.ifExpr)
        ga!!.ifZCmp(GeneratorAdapter.EQ, elseLbl)
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
        visit(whileExpr.condition)
        ga!!.ifZCmp(GeneratorAdapter.EQ, lblEnd)
        visit(whileExpr.statement)
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

        visit(forExpr.initializer)

        ga!!.mark(start)
        visit(forExpr.condition)
        ga!!.ifZCmp(GeneratorAdapter.EQ, end)
        visit(forExpr.body)
        visit(forExpr.increment)
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

        val usedValue: ValueType = arrayGet.firstValue
        for ((_, index) in arrayGet.indexes.withIndex()) {
            ga!!.push(index)
            ga!!.arrayLoad(Type.getType(usedValue.descriptor))

            usedValue.dimensions -= 1
            usedValue.descriptor.replaceFirst("[", "")
        }
    }

    override fun visit(assignment: AssignmentExpression) {
        if (assignment.indexes.isNotEmpty()) {
            ga!!.loadLocal(env.lookupVariable(assignment.name).id)

            val usedValue: ValueType = assignment.firstValue
            for (index in 0 until assignment.indexes.size - 1) {
                ga!!.push(index)
                ga!!.arrayLoad(Type.getType(usedValue.descriptor))

                usedValue.dimensions -= 1
                usedValue.descriptor = usedValue.descriptor.replaceFirst("[", "")
            }

            ga!!.push(assignment.indexes.last())
            visit(assignment.value)
            ga!!.arrayStore(Type.getType(usedValue.descriptor.removePrefix("[")))
        } else {
            visit(assignment.value)

            val variable = env.lookupVariable(assignment.name)
            ga!!.storeLocal(variable.id)
        }
    }

    override fun visit(arrayCreate: ArrayCreateExpr) {
        if (arrayCreate.indexes.size == 1) {
            ga!!.push(arrayCreate.indexes[0])
            ga!!.newArray(Type.getType(arrayCreate.type.descriptor))
        } else {
            //>=1
            for (index in arrayCreate.indexes) {
                ga!!.push(index)
            }

            ga!!.visitMultiANewArrayInsn("[".repeat(arrayCreate.indexes.size) + arrayCreate.type.descriptor, arrayCreate.indexes.size)
        }
    }

    override fun visit(expr: Expr) {
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
            else -> throw RuntimeException("No expression found for " + expr.javaClass.name)
        }
    }

    fun bytecode(): ByteArray {
        return cw.toByteArray()
    }
}