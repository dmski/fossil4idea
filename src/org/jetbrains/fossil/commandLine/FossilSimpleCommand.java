package org.jetbrains.fossil.commandLine;

import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vcs.ProcessEventListener;
import com.intellij.openapi.vcs.VcsException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.fossil.FossilVcs;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 2/13/13
 * Time: 8:34 PM
 */
public class FossilSimpleCommand extends FossilTextCommand {
  private final StringBuilder myStderr;
  private final StringBuilder myStdout;

  public FossilSimpleCommand(Project project, File workingDirectory, @NotNull FCommandName commandName) {
    super(project, workingDirectory, commandName);

    myStderr = new StringBuilder();
    myStdout = new StringBuilder();
  }

  @Override
  protected void processTerminated(int exitCode) {
    //
  }

  @Override
  protected void onTextAvailable(String text, Key outputType) {
    if (ProcessOutputTypes.STDOUT.equals(outputType)) {
      myStdout.append(text);
    } else if (ProcessOutputTypes.STDERR.equals(outputType)) {
      myStderr.append(text);
    }
  }

  public StringBuilder getStderr() {
    return myStderr;
  }

  public StringBuilder getStdout() {
    return myStdout;
  }

  public String run() throws VcsException {
    final VcsException[] ex = new VcsException[1];
    final String[] result = new String[1];
    addListener(new ProcessEventListener() {
      @Override
      public void processTerminated(int exitCode) {
        try {
          if (exitCode == 0) {
            result[0] = getStdout().toString();
          }
          else {
            String msg = getStderr().toString();
            if (msg.length() == 0) {
              msg = getStdout().toString();
            }
            if (msg.length() == 0) {
              msg = "Svn process exited with error code: " + exitCode;
            }
            ex[0] = new VcsException(msg);
          }
        }
        catch (Throwable t) {
          ex[0] = new VcsException(t.toString(), t);
        }
      }

      @Override
      public void startFailed(Throwable exception) {
        ex[0] = new VcsException("Process failed to start (" + myCommandLine.getCommandLineString() + "): " + exception.toString(), exception);
      }
    });
    start();
    if (myProcess != null) {
      waitFor();
    }
    if (ex[0] != null) {
      FossilVcs.getInstance(myProject).checkVersion();
      throw ex[0];
    }
    if (result[0] == null) {
      throw new VcsException("Svn command returned null: " + myCommandLine.getCommandLineString());
    }
    return result[0];
  }
}