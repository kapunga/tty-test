package tty

import cats.Show
import scala.scalanative.posix.termios
import scala.scalanative.posix.termios.c_cc
import scala.scalanative.unsafe.{CLong, Ptr}

case class Termios(iflag: CLong, oflag: CLong, cflag: CLong, lflag: CLong, cc: Array[Byte], ispeed: Long, ospeed: Long)

object Termios {
  given Show[Termios] with
    def show(t: Termios): String = {
      s"""Termios\r
         |=======\r
         |Input Flags   - ${t.iflag}\r
         |Output Flags  - ${t.oflag}\r
         |Control Flags - ${t.cflag}\r
         |Local Flags   - ${t.lflag}\r
         |Control Chars - ${t.cc.map(b => "0x" + b.toInt.toHexString).mkString(", ")}\r
         |Speeds (i:o)  - ${t.ispeed}:${t.ospeed}\r
         |""".stripMargin
    }

  // Foo
  def fromPtr(ptr: Ptr[termios.termios]): Termios =
    new Termios(
      iflag = ptr._1,
      oflag = ptr._2,
      cflag = ptr._3,
      lflag = ptr._4,
      cc = fromCC(ptr._5),
      ispeed = ptr._6,
      ospeed = ptr._7
    )

  private def fromCC(arr: c_cc): Array[Byte] = Range(0, arr.length).map(arr.apply).toArray

  def writePtr(t: Termios, ptr: Ptr[termios.termios]): Unit = {
    ptr.at1.update(0, t.iflag)
    ptr.at2.update(0, t.oflag)
    ptr.at3.update(0, t.cflag)
    ptr.at4.update(0, t.lflag)
    updateCC(t.cc, ptr.at5)
    ptr.at6.update(0, t.ispeed)
    ptr.at7.update(0, t.ospeed)
  }

  private def updateCC(t: Array[Byte], arr: Ptr[c_cc]): Unit =
    Range(0, arr.length).foreach(i => arr.update(i, t(i % arr.length)))
}
