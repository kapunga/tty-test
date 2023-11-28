package tty

import cats.effect.*

object TtyResourceApp extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    for {
      _ <- TTY.printTtyMode("TTY mode before setting ")
      ec <- runTest
      _ <- TTY.printTtyMode("TTY mode after reset ")
    } yield ec
  }

  private def runTest: IO[ExitCode] = {
    TTY.resource.use { tty =>
      for {
        _ <- tty.printTtyMode("TTY mode during resource use ")
        _ <- tty.println(s"Testing tty!")
        _ <- tty.print(s"User: ")
        u <- tty.readPlain
        _ <- tty.print("Password: ")
        p <- tty.readPassword
        _ <- tty.println(s"$u has the password '$p'")
      } yield ExitCode.Success
    }
  }
}
