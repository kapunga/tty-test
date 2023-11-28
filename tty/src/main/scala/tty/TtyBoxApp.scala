package tty

import cats.effect.*

object TtyBoxApp extends TTYApp {
  override def run(args: List[String], tty: TTY): IO[ExitCode] = {
    for {
      ec <- runTest(tty)
    } yield ec
  }

  private def runTest(tty: TTY): IO[ExitCode] = {
    for {
      _ <- tty.printTtyMode(s"TTY mode during run")
      _ <- tty.println(s"Testing tty!")
      _ <- tty.print(s"User: ")
      u <- tty.readPlain
      _ <- tty.print("Password: ")
      p <- tty.readPassword
      _ <- tty.println(s"$u has the password '$p'")
    } yield ExitCode.Success
  }
}
