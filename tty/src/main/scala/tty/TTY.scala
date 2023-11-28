package tty

import cats.implicits.*
import cats.effect.{IO, Resource}
import cats.effect.std.{Console, Queue}
import scala.scalanative.posix.{termios, unistd}
import scala.scalanative.unsafe
import scala.scalanative.unsafe.CLong

class TTY(inputQueue: Queue[IO, String], outputQueue: Queue[IO, String]) {
  def print(line: String): IO[Unit] = outputQueue.offer(line)
  def println(line: String): IO[Unit] = print(line + "\r\n")

  def readPlain: IO[String] = readLine(false)

  def readPassword: IO[String] = readLine(true)

  private def readLine(secret: Boolean): IO[String] =  {
    def echo(next: String): String =
      if (secret) {
        if (next == "\n") "\r\n" else "*"
      } else {
        next
      }

    def readImpl(acc: String = ""): IO[String] =
      for {
        next <- inputQueue.take
        _ <- outputQueue.offer(echo(next))
        pw <- if (next == "\n") IO.pure(acc + next) else readImpl(acc + next)
      } yield pw

    readImpl()
  }

  def printTtyMode(msg: String): IO[Unit] = {
    val ptr = unsafe.stackalloc[termios.termios]()
    termios.tcgetattr(unistd.STDIN_FILENO, ptr)

    val ttyMode = Termios.fromPtr(ptr)
    this.println(s"$msg ${ttyMode.show}")
  }
}

object TTY {
  // TTY resource without mode setting
  def noMode: Resource[IO, TTY] =
    for {
      inputQueue <- stdinQueue
      outputQueue <- stdoutQueue
    } yield TTY(inputQueue, outputQueue)

  // TTY resource with mode setting
  def resource: Resource[IO, TTY] =
    for {
      _ <- terminalModeResource
      inputQueue <- stdinQueue
      outputQueue <- stdoutQueue
    } yield TTY(inputQueue, outputQueue)

  private def terminalModeResource: Resource[IO, Termios] = {
    val ptr = unsafe.stackalloc[termios.termios]()
    val failed = termios.tcgetattr(unistd.STDIN_FILENO, ptr) != 0
    if (failed) {
      Resource.eval(IO.raiseError(new RuntimeException("Unable to lookup ttymode!")))
    } else {
      val existing = Termios.fromPtr(ptr)

      Resource.make({
        val noEchoMask: CLong = ~(termios.ICANON | termios.ECHO | termios.ECHONL /* | termios.ISIG */)
        val updatedLflag: CLong = existing.lflag & noEchoMask
        val noEchoMode = existing.copy(iflag = 0, oflag = 0, cflag = 0, lflag = 0)

        val t_ptr = unsafe.stackalloc[termios.termios]()
        Termios.writePtr(noEchoMode, t_ptr)

        if (termios.tcsetattr(unistd.STDIN_FILENO, termios.TCSANOW, t_ptr) == 0) {
          val s = s"- old_lflag: ${ptr._4.toBinaryString}\n- no-echo-m: ${noEchoMask.toBinaryString}\n- raw--mode: ${updatedLflag.toBinaryString}\n- new_lflag: ${t_ptr._4.toBinaryString}"
          IO.pure(existing).flatTap(_ => Console[IO].println(s"Setting ttymode to raw with\n$s"))
        } else {
          IO.raiseError(new RuntimeException("Unable to set ttymode!"))
        }
      })(existing => {
        val t_ptr = unsafe.stackalloc[termios.termios]()
        Termios.writePtr(existing, t_ptr)

        if (termios.tcsetattr(unistd.STDIN_FILENO, termios.TCSANOW, t_ptr) == 0) {
          IO.unit.flatTap(_ => Console[IO].println("Setting ttymode to back to original."))
        } else {
          IO.raiseError(new RuntimeException("Unable to reset ttymode!"))
        }
      })
    }
  }

  private def stdinQueue: Resource[IO, Queue[IO, String]] =
    Queue.synchronous[IO, String].toResource.flatTap { q =>
      fs2.io.stdinUtf8[IO](256)
        .enqueueUnterminated(q)
        .compile.drain.background
    }

  private def stdoutQueue: Resource[IO, Queue[IO, String]] = {
    Queue.synchronous[IO, String].toResource.flatTap { q =>
      val queueStream: fs2.Stream[IO, Byte] =
        fs2.Stream.fromQueueUnterminated(q)
          .flatMap(s => fs2.Stream.emits(s.getBytes))

      fs2.io.stdout[IO].apply(queueStream).compile.drain.background
    }
  }

  def printTtyMode(msg: String): IO[Unit] = {
    val ptr = unsafe.stackalloc[termios.termios]()
    termios.tcgetattr(unistd.STDIN_FILENO, ptr)

    val t = Termios.fromPtr(ptr)

    Console[IO].println(s"$msg ${t.show}")
  }
}
