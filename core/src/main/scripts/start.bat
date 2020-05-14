@echo off

rem
rem Windows startscript for WSRFlite
rem

set CP=.

rem Figure out where wsrflite is installed
set WSRFLITE_HOME=%~d0%~p0..

for %%i in ("%WSRFLITE_HOME%\lib\*.jar") do ( call :cpappend %%i )


set CMD_LINE_ARGS=
:setArgs
if ""%1""=="""" goto doneSetArgs
set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
shift
goto setArgs
:doneSetArgs

rem
rem Go
rem
java -cp %CP% de.fzj.unicore.wsrflite.Kernel %CMD_LINE_ARGS%
goto :eof


rem
rem Append stuff to the classpath
rem
:cpappend
if ""%1"" == """" goto done
set CP=%CP%;%1
shift
goto :cpappend
:done
goto :eof