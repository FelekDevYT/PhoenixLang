package me.felek.fenix.compiler

import me.felek.ast.Expr
import me.felek.ast.decl.FunctionDeclaration
import me.felek.ast.exprs.BinaryExpr
import me.felek.ast.exprs.BlockExpr
import me.felek.ast.exprs.BoolExpr
import me.felek.ast.exprs.EqCmpExpr
import me.felek.ast.exprs.FloatExpr
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

/**
 * @className: Compiler
 * @author: Felek
 * @date: 27/10/2026 15:23
 */

class Compiler() : Visitor<Void?> {
    private var cw: ClassWriter = ClassWriter(ClassWriter.COMPUTE_MAXS or ClassWriter.COMPUTE_FRAMES)
    private var ga: GeneratorAdapter? = null
    private var env: Environment = Environment(null)
    private lateinit var moduleName: String
    private var functions: MutableMap<String, FunctionSymbol> = mutableMapOf()

    override fun visit(expr: IntExpr): Void? {
        ga!!.push(expr.value)

        return null
    }

    override fun visit(expr: FloatExpr): Void? {
        ga!!.push(expr.value)

        return null
    }

    override fun visit(expr: StrExpr): Void? {
        ga!!.push(expr.value)

        return null
    }

    override fun visit(expr: BoolExpr): Void? {
        ga!!.push(expr.value)

        return null
    }

    override fun visit(expr: BlockExpr): Void? {
        val temp: Environment = env
        env = Environment(env)
        for (e in expr.body) {
            visit(e)
        }
        env = temp

        return null
    }

    override fun visit(expr: InvokeStaticExpr): Void? {
        for (arg in expr.args) {
            visit(arg)
        }

        ga!!.invokeStatic(TypeUtils.getAsmType(expr.owner), Method.getMethod(expr.methodImpl))

        return null
    }

    override fun visit(expr: InvokeLocalExpr): Void? {
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

        return null
    }

    override fun visit(varDecl: VariableDeclaration): Void? {
        val symbol = VariableSymbol(varDecl.name, varDecl.type!!, varDecl.isConst)

        var local = ga!!.newLocal(TypeUtils.getAsmType(varDecl.type!!.name))
        symbol.id = local

        visit(varDecl.value!!)
        ga!!.storeLocal(local)

        env.defineVariable(varDecl.name, symbol)

        return null
    }

    override fun visit(ast: ASTResult): Void? {
        cw.visit(V21, ACC_PUBLIC, ast.moduleName, null, "java/lang/Object", null)//Java 21 bytecode support(later, Java 8/11/21/28 support)

        this.moduleName = ast.moduleName

        for (f in ast.functions) {
            functions[f.name] = FunctionSymbol(f.name, f.args, f.returnType)
        }

        for (decl in ast.functions) {
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

        return null
    }

    override fun visit(id: IdExpr): Void? {
        var variable: VariableSymbol = env.lookupVariable(id.id)
        if (variable.isArgVar) {
            ga!!.loadArg(variable.id)
        } else {
            ga!!.loadLocal(variable.id)
        }

        return null
    }

    override fun visit(ret: ReturnExpr): Void? {
        visit(ret.expr)
        ga!!.returnValue()

        return null
    }

    override fun visit(bin: BinaryExpr): Void? {
        visit(bin.a)
        visit(bin.b)

        ga!!.math(when(bin.op) {
            BinaryType.REM -> GeneratorAdapter.REM
            BinaryType.MUL -> GeneratorAdapter.MUL
            BinaryType.DIV -> GeneratorAdapter.DIV
            BinaryType.ADD -> GeneratorAdapter.ADD
            BinaryType.SUB -> GeneratorAdapter.SUB
        }, bin.type)

        return null
    }

    override fun visit(ifelse: IfElseExpr): Void? {
        var end: Label = ga!!.newLabel()
        var elseLbl: Label = ga!!.newLabel()

        visit(ifelse.ifExpr)
        ga!!.ifZCmp(GeneratorAdapter.EQ, elseLbl)
        visit(ifelse.ifStmt)
        ga!!.goTo(end)
        ga!!.mark(elseLbl)
        if (ifelse.elseStmt != null) {
            visit(ifelse.elseStmt!!)
        }
        ga!!.mark(end)

        return null
    }

    override fun visit(eqCmp: EqCmpExpr): Void? {
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

        return null
    }

    override fun visit(expr: Expr): Void? {
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
            else -> throw RuntimeException("No expression found for " + expr.javaClass.name)
        }
    }

    public fun bytecode(): ByteArray {
        return cw.toByteArray()
    }
}