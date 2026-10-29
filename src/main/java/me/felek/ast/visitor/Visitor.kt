package me.felek.ast.visitor

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
import me.felek.ast.invoke.InvokeLocalExpr
import me.felek.ast.invoke.InvokeStaticExpr
import me.felek.ast.loop.WhileExpr
import me.felek.fenix.ast.ASTResult

/**
 * @className: Visitor
 * @author: Felek
 * @date: 27/10/2026 14:13
 */

interface Visitor<T> {
    fun visit(expr: IntExpr): T
    fun visit(expr: FloatExpr): T
    fun visit(expr: StrExpr): T
    fun visit(expr: BoolExpr): T

    fun visit(expr: BlockExpr): T

    fun visit(expr: InvokeStaticExpr): T
    fun visit(expr: InvokeLocalExpr): T

    fun visit(varDecl: VariableDeclaration): T
    fun visit(id: IdExpr): T
    fun visit(ret: ReturnExpr): T
    fun visit(bin: BinaryExpr): T
    fun visit(ifelse: IfElseExpr): T
    fun visit(eqCmp: EqCmpExpr): T
    fun visit(whileExpr: WhileExpr): T
    fun visit(forExpr: ForExpr): T

    fun visit(breakExpr: BreakExpr): T
    fun visit(continueExpr: ContinueExpr): T

    fun visit(assignment: AssignmentExpression): T
    fun visit(castExpr: CastExpr): T
    fun visit(arrayGet: ArrayGetExpr): T
    fun visit(arrayCreate: ArrayCreateExpr): T

    fun visit(expr: Expr): T
    fun visit(decl: ASTResult): T
}