package me.felek.fenix.ast

import com.sun.jdi.IntegerValue
import me.felek.ast.Expr
import me.felek.ast.decl.Arg
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
import me.felek.ast.type.ValueType
import me.felek.bright.parser.FenixBaseVisitor
import me.felek.bright.parser.FenixParser
import me.felek.fenix.utils.TypeUtils

/**
 * @className: ASTVisitor
 * @author: Felek
 * @date: 27/10/2026 14:59
 */

class ASTVisitor(var moduleName: String) : FenixBaseVisitor<Expr?>() {
    public val functions: MutableList<FunctionDeclaration> = mutableListOf()
    public val importMap: MutableMap<String, String> = mutableMapOf()

    override fun visitUsingDeclaration(ctx: FenixParser.UsingDeclarationContext?): Expr? {
        importMap[ctx!!.STRING().text.substring(1, ctx.STRING().text.length - 1).substring(ctx.STRING().text.lastIndexOf('.'))] = ctx.STRING().text.substring(1, ctx.STRING().text.length - 1)

        return null
    }

    override fun visitFunctionDeclaration(ctx: FenixParser.FunctionDeclarationContext?): Expr? {
        val list: MutableList<Arg> = mutableListOf()

        for (arg in ctx!!.arg()) {
            list.add(Arg(arg.ID().text, TypeUtils.typeByName(arg.type().text)))
        }


        functions.add(FunctionDeclaration(ctx.start.line, ctx.start.charPositionInLine, ctx.ID().text, list, if (ctx.type() == null) ValueType.VOID else TypeUtils.typeByName(ctx.type().text), if (ctx.block() == null) visit(ctx.expr())!! else visit(ctx.block())!!))

        return null
    }

    override fun visitInt(ctx: FenixParser.IntContext?): Expr? {
        return IntExpr(ctx!!.start.line, ctx.start.charPositionInLine, ctx.INT().text.toInt())
    }

    override fun visitString(ctx: FenixParser.StringContext?): Expr? {
        return StrExpr(ctx!!.start.line, ctx.start.charPositionInLine, ctx.STRING().text)
    }

    override fun visitFloat(ctx: FenixParser.FloatContext?): Expr? {
        return FloatExpr(ctx!!.start.line, ctx.start.charPositionInLine, ctx.FLOAT().text.toFloat())
    }

    override fun visitBool(ctx: FenixParser.BoolContext?): Expr? {
        return BoolExpr(ctx!!.start.line, ctx.start.charPositionInLine, ctx.BOOL().text.toBoolean())
    }

    override fun visitBlock(ctx: FenixParser.BlockContext?): Expr? {
        var list: MutableList<Expr> = mutableListOf()

        for (expr in ctx!!.expr()) {
            list.add(visit(expr)!!)
        }

        return BlockExpr(ctx.start.line, ctx.start.charPositionInLine, list)
    }

    override fun visitReturn(ctx: FenixParser.ReturnContext?): Expr? {
        return ReturnExpr(ctx!!.start.line, ctx.start.charPositionInLine, visit(ctx.expr())!!)
    }

    override fun visitAdditive(ctx: FenixParser.AdditiveContext?): Expr? {
        val bin: BinaryType = getBinaryType(ctx!!.op.text)

        return BinaryExpr(ctx.start.line, ctx.start.charPositionInLine, visit(ctx.expr(0))!!, visit(ctx.expr(1))!!, bin)
    }

    override fun visitIfElse(ctx: FenixParser.IfElseContext?): Expr? {
        //expr0 - if condition
        //expr1 - if statement
        //expr2 - else stmt
        val ifCondition = visit(ctx!!.expr())!!
        val ifStmt = if (ctx.statement(0).block() == null) visit(ctx.statement(0).expr())!! else visit(ctx.statement(0).block())!!
        var elseStmt: Expr? = null
        if (ctx.statement(1) != null) {
            elseStmt = if (ctx.statement(1).block() == null) visit(ctx.statement(1).expr())!! else visit(ctx.statement(1).block())!!
        }

        return IfElseExpr(ctx.start.line, ctx.start.charPositionInLine, ifCondition, ifStmt, elseStmt)
    }

    override fun visitEqCmp(ctx: FenixParser.EqCmpContext?): Expr? {
        val op = when (ctx!!.op.text) {
            "!=" -> ECType.NEQ
            "==" -> ECType.EQ
            ">=" -> ECType.BE
            "<=" -> ECType.LE
            ">" -> ECType.BT
            else -> ECType.LT
        }

        return EqCmpExpr(ctx!!.start.line, ctx.start.charPositionInLine, visit(ctx.expr(0))!!, visit(ctx.expr(1))!!, op)
    }

    override fun visitMultiplicative(ctx: FenixParser.MultiplicativeContext?): Expr? {
        val bin: BinaryType = getBinaryType(ctx!!.op.text)

        return BinaryExpr(ctx.start.line, ctx.start.charPositionInLine, visit(ctx.expr(0))!!, visit(ctx.expr(1))!!, bin)
    }

    fun getBinaryType(symbol: String): BinaryType {
        return when(symbol) {
            "+" -> BinaryType.ADD
            "-" -> BinaryType.SUB
            "/" -> BinaryType.DIV
            "*" -> BinaryType.MUL
            else -> BinaryType.REM
        }
    }

    override fun visitInvokeStatic(ctx: FenixParser.InvokeStaticContext?): Expr? {
        var list: MutableList<Expr> = mutableListOf()

        for (expr in ctx!!.expr()) {
            list.add(visit(expr)!!)
        }

        return InvokeStaticExpr(ctx.start.line, ctx.start.charPositionInLine, ctx.ID(0).text, ctx.ID(1).text, list)
    }

    override fun visitInvokeLocal(ctx: FenixParser.InvokeLocalContext?): Expr? {
        var list: MutableList<Expr> = mutableListOf()

        for (expr in ctx!!.expr()) {
            list.add(visit(expr)!!)
        }

        return InvokeLocalExpr(ctx.start.line, ctx.start.charPositionInLine, ctx.ID().text, list)
    }

    override fun visitVariableDeclaration(ctx: FenixParser.VariableDeclarationContext?): Expr? {
        return VariableDeclaration(ctx!!.start.line, ctx.start.charPositionInLine, ctx.ID().text, ctx.CONST() != null,
            if (ctx.type().text != null) TypeUtils.typeByName(ctx.type().text) else null, if (ctx!!.expr() != null) visit(ctx!!.expr()) else null)
    }

    override fun visitId(ctx: FenixParser.IdContext?): Expr? {
        return IdExpr(ctx!!.start.line, ctx.start.charPositionInLine, ctx.ID().text)
    }

    public fun getResult(): ASTResult {
        return ASTResult(functions, moduleName, importMap)
    }
}