package chisel3.experimental.hierarchy

import chisel3.experimental.hierarchy.core._
import chisel3.internal.firrtl._
import chisel3.internal.{PseudoModule, Namespace, HasId, throwException}
import chisel3.internal.sourceinfo.SourceInfo
import chisel3.experimental.BaseModule
import chisel3._
import Utils._

/** Represents a normal Module within the current context
  * If representing either, it has IO accordingly.
  * For Normal modules, it shares the module's IO
  * For ModuleClones, it shares the standInClone's io
 */
private[chisel3] final class ModuleTransparent[T <: BaseModule] private (
    val genesis: ModuleDefinition[T]
) extends PseudoModule with Transparent[T, BaseModule] {
  lazy val ioMap: Map[Data, Data] = proto.getChiselPorts.map { case (_, data) => data -> data }.toMap
  def parent = _parent.get

  // ======== THINGS TO MAKE CHISEL WORK ========

  override def toString = s"ModuleTransparent(${proto})"
  // No addition components are generated
  private[chisel3] def generateComponent(): Option[Component] = None
  // Necessary for toTarget to work
  private[chisel3] def setAsInstanceRef(): Unit = { this.setRef(Ref(instanceName)) }
  // This module doesn't acutally exist in the FIRRTL so no initialization to do
  private[chisel3] def initializeInParent(parentCompileOptions: CompileOptions): Unit = ()
  // Instance name is the same as proto's instance name
  override def instanceName = proto.instanceName
  // Module name is the same as proto's module name
  override def desiredName: String = proto.name
}

private[chisel3] object ModuleTransparent {
  def apply[T <: BaseModule](genesis: ModuleDefinition[T])(
    implicit sourceInfo: SourceInfo,
    compileOptions: CompileOptions
  ): ModuleTransparent[T] = {
    val ret = Module.do_pseudo_apply(new ModuleTransparent(genesis))
    ret._parent = genesis.proto._parent
    ret
  }
}