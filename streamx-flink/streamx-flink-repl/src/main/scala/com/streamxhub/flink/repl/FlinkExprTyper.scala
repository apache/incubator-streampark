package com.streamxhub.flink.repl

import scala.tools.nsc.interpreter.{ExprTyper, IR}

trait FlinkExprTyper extends ExprTyper {

  import repl._
  import global.{Import => _, reporter => _, _}
  import naming.freshInternalVarName

  def doInterpret(code: String): IR.Result = {
    // interpret/interpretSynthetic may change the phase,
    // which would have unintended effects on types.
    val savedPhase = phase
    try interpretSynthetic(code) finally phase = savedPhase
  }

  override def symbolOfLine(code: String): Symbol = {
    def asExpr(): Symbol = {
      val name = freshInternalVarName()
      // Typing it with a lazy val would give us the right type, but runs
      // into compiler bugs with things like existentials, so we compile it
      // behind a def and strip the NullaryMethodType which wraps the expr.
      val line = "def " + name + " = " + code

      doInterpret(line) match {
        case IR.Success =>
          val sym0 = symbolOfTerm(name)
          // drop NullaryMethodType
          sym0.cloneSymbol setInfo exitingTyper(sym0.tpe_*.finalResultType)
        case _ => NoSymbol
      }
    }

    def asDefn(): Symbol = {
      val old = repl.definedSymbolList.toSet

      doInterpret(code) match {
        case IR.Success =>
          repl.definedSymbolList filterNot old match {
            case Nil => NoSymbol
            case sym :: Nil => sym
            case syms => NoSymbol.newOverloaded(NoPrefix, syms)
          }
        case _ => NoSymbol
      }
    }

    def asError(): Symbol = {
      doInterpret(code)
      NoSymbol
    }

    beSilentDuring(asExpr()) orElse beSilentDuring(asDefn()) orElse asError()
  }

}
