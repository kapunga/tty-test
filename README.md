# TTY Test

# The Problem
I'm trying to figure out how to change the TTY mode for an application. I have two sample apps:
* _tty.TtyBoxApp_ - My rough and dirty initial pass. This correctly sets the tty mode I want during execution, however
  the mode doesn't get reset cleanly after execution is complete. Running the app a second time without manually
  resetting the terminal from the command line completely breaks the terminal freezing input and output.
* _tty.TtyResourceApp_ - My attempt to clean things up, by making TTY a resource that cleans itself up after it's
  complete. The terminal doesn't ever seem to get in the correct mode.

## How I'm running this

I'm running this on an Apple Silicon Macbook. From Intellij, I have the following running in a terminal prompt:
```shell
sbt:tty-test> ~ttyTest/nativeLink
[info] compiling 5 Scala sources to /Users/kapunga/code/apps/tty-test/tty/target/scala-3.3.1/classes ...
[warn] multiple main classes detected: run 'show discoveredMainClasses' to see the list

Multiple main classes detected. Select one to run:
 [1] tty.TtyBoxApp
 [2] tty.TtyResourceApp

Enter number: 1
. . .
```

Note: Running `ttyTest/run` from `sbt` doesn't really seem to work, I assume sbt also sets the console mode, and
that is why.

From the same folder as `tty-test` I am running the following from _iTerm2_:
```shell
tty-test % ./tty/target/scala-3.3.1/tty-test-out
```

Additionally, I use the following commands:
* `/bin/stty -f /dev/tty` - Check the tty mode between runs.
* `/bin/stty -f /dev/tty -g` - To get the string needed to reset the terminal mode manually. For me this ends up looking
  like the following:
```shell
tty-test % /bin/stty -f /dev/tty -g gfmt1:cflag=4b00:iflag=6b02:lflag=200005cf:oflag=3:discard=f:dsusp=19:eof=4:eol=ff:eol2=ff:erase=7f:intr=3:kill=15:lnext=16:min=1:quit=1c:reprint=12:start=11:status=14:stop=13:susp=1a:time=0:werase=17:ispeed=38400:ospeed=38400
```
