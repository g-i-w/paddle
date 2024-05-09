package paddle;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.io.*;

public class SystemCommand extends TimerTask {

	private String name;
	private Process commandProc;
	private boolean running;
	private boolean done;
	private int executed;
	private int skipped;
	private int destroyed;
	private int destroyedForcibly;
	private String command;
	private InputStream stdoutStream;
	private InputStream stderrStream;
	private ReadStream stdout;
	private ReadStream stderr;
	private long durationStart;
	private long duration;
	private long timeout;
	private long successfulRuns;
	private int exitValue;
	private boolean verbose;
	private boolean lastLineOnly;

	public SystemCommand ( String command ) {
		this( command, command, 10000 ); // default: 10 sec timeout
	}

	public SystemCommand ( String command, String name, long timeout ) {
		this( command, name, timeout, false, false );
	}

	public SystemCommand ( String command, String name, long timeout, boolean verbose, boolean lastLineOnly ) {
		this.name = name;
		this.command = command;
		this.timeout = timeout;
		this.verbose = verbose;
		this.lastLineOnly = lastLineOnly;
		running = false;
		done = false;
		executed = 0;
		skipped = 0;
		destroyed = 0;
		destroyedForcibly = 0;
		durationStart = 0;
		duration = 0;
		successfulRuns = 0;
		exitValue = 127; // default, in the event that an Exception leaves commandProc null
	}
	
	public void preExec () {
		if (verbose) System.out.println( this.getClass().getName()+" '"+name+"': executing...\n> "+command );
	}
	
	public void onException ( Exception e ) {
		e.printStackTrace();
	}
	
	public void postExec () {
		if (verbose) System.out.println( this.getClass().getName()+" '"+name+"': done." );	
	}
		
	public void run () {
		// skip execution if run as TimerTask and the scheduled execution time + has elapsed
		if (scheduledExecutionTime() != 0 && System.currentTimeMillis() - scheduledExecutionTime() >= Math.max(timeout/2, 100L)) {
			exitValue = 126;
			done = true;
			skipped++;
			return;
		}
		done = false; // applicable only when run as repeat TimerTask
		preExec();
		durationStart = System.currentTimeMillis();
		try {
			commandProc = Runtime.getRuntime().exec( command );
			stdoutStream = commandProc.getInputStream();
			stderrStream = commandProc.getErrorStream();
		} catch (Exception e) {
			onException( e );
		}
		stdout = new ReadStream( stdoutStream, "stdout", lastLineOnly );
		stderr = new ReadStream( stderrStream, "stderr", lastLineOnly );
		running = true;
		executed++;
		try {
			if (timeout < 1) { // wait idefinitely...
				commandProc.waitFor();
			} else { // otherwise, wait until timeout... and if it doesn't die, another timeout... then '-9' it!
				if (!commandProc.waitFor( timeout, TimeUnit.MILLISECONDS )) {
					kill();
					destroyed++;
					if (!commandProc.waitFor( timeout, TimeUnit.MILLISECONDS )) {				
						killForcibly();
						destroyedForcibly++;
					}
				}
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		while( commandProc != null && commandProc.isAlive() ) {
			try { Thread.sleep(1); } catch (Exception e) { e.printStackTrace(); }
		}
		duration = time();
		done = true;
		while( !stdout.done() || !stderr.done() ) { // ...in case the output buffers are still printing...
			try { Thread.sleep(1); } catch (Exception e) { e.printStackTrace(); }
		}
		running = false;
		postExec();
	}
	
	public long time () {
		return System.currentTimeMillis() - durationStart;
	}
	
	public long timeout () {
		return timeout;
	}
	
	public void timeout ( long timeout ) {
		this.timeout = timeout;
	}
	
	public void kill () {
		if (stdout!=null) stdout.end();
		if (stderr!=null) stderr.end();
		if (commandProc!=null) commandProc.destroy();
	}
	
	public void killForcibly () {
		if (commandProc!=null) commandProc.destroyForcibly();
		if (stdout!=null) stdout.end();
		if (stderr!=null) stderr.end();
	}
	
	public String command () {
		return command;
	}
	
	public SystemCommand command ( String command ) {
		this.command = command;
		return this;
	}
	
	public int exitValue () {
		return ( commandProc != null ? commandProc.exitValue() : exitValue );
	}
	
	public ReadStream stdout () {
		return stdout;
	}
	
	public ReadStream stderr () {
		return stderr;
	}
	
	public boolean running () {
		return running;
	}
	
	public boolean done () {
		return done;
	}
	
	public int executed () {
		return executed;
	}
	
	public int skipped () {
		return skipped;
	}
	
	public int destroyed () {
		return destroyed;
	}
	
	public int destroyedForcibly () {
		return destroyedForcibly;
	}
	
	public long duration () {
		return duration;
	}
	
	public String getName () {
		return name;
	}
	
	public String output () {
		run();
		return stdout().text();
	}
	
	public String toString () {
		return
			"command: "+command()+"\n"+
			"running: "+running()+"\n"+
			"done: "+done()+"\n"+
			"executed: "+executed()+"\n"+
			"skipped: "+skipped()+"\n"+
			( running() || done() ?
				"stdout: "+stdout().text()+"\n"+
				"stderr: "+stderr().text()+"\n"
			: "" ) +
			( done() ?
				"destroyed: "+destroyed()+"\n"+
				"destroyedForcibly: "+destroyedForcibly()+"\n"+
				"duration: "+duration()+"ms\n"+
				"exit code: "+exitValue()+"\n"
			: "" );
	}
	
	// test
	public static void main (String[] args) throws Exception {
	
		if (args.length>0) {
		
			SystemCommand sc = new SystemCommand( args[0] );
			new Thread( sc ).start();
			//sc.run();
			while(true) {
				Thread.sleep(500);
				System.out.println( sc );
			}

		} else {
		
			SystemCommand sc3 = new SystemCommand( "sleep 5s", "long running command", 1000 );
			sc3.run();
			System.out.println( sc3 );
			
			System.out.println();
			
			SystemCommand sc = new SystemCommand( "ls -lh", "test command!", 1000 );
			System.out.println( sc );
			sc.run();
			while(sc.running());
			System.out.println( sc );
			
			System.out.println();
			
			SystemCommand sc1 = new SystemCommand( "ls -z", "stderr command", 1000 );
			sc1.run();
			while(sc1.running());
			System.out.println( sc1 );
			
			System.out.println();
			
			SystemCommand sc2 = new SystemCommand( "l", "bad command", 1000 );
			sc2.run();
			System.out.println( sc2 );
			while(sc2.running());
			System.out.println( sc2 );
			
			Thread.sleep(500);
			System.out.println("done.");
			
		}
	}

}
