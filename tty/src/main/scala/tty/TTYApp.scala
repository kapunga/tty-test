package tty

import cats.effect.{ExitCode, IO, IOApp}
import scala.scalanative.posix.{termios, unistd}
import scala.scalanative.unsafe
import scala.scalanative.unsafe.CLong

trait TTYApp extends IOApp {
  final def run(args: List[String]): IO[ExitCode] =
    TTY.printTtyMode("TTY Mode before run") *> {
      val t_ptr = unsafe.stackalloc[termios.termios]()

      if (termios.tcgetattr(unistd.STDIN_FILENO, t_ptr) != 0) {
        IO.raiseError(new RuntimeException("Failed to get terminal settings"))
      } else {
        val u_ptr = unsafe.stackalloc[termios.termios]()
        u_ptr.update(0, !t_ptr)

        val update: CLong = ~(termios.ICANON | termios.ECHO | termios.ECHONL /*| termios.ISIG */)

        val updatedMode: CLong = u_ptr._4 & update

        u_ptr._4 = updatedMode

        val success = termios.tcsetattr(unistd.STDIN_FILENO, termios.TCSANOW, u_ptr) == 0

        if (!success) {
          IO.raiseError(new RuntimeException("Failed to set raw mode."))
        } else {
          TTY.noMode.use { tty =>
            run(args, tty) <*
              IO(termios.tcsetattr(unistd.STDIN_FILENO, termios.TCSAFLUSH, t_ptr)) <*
              TTY.printTtyMode("Mode after reset.")
          }
        }
      }
    }

  def run(args: List[String], tty: TTY): IO[ExitCode]
}
