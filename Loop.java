package paddle;

public abstract class Loop extends Thread {

	// General
	private boolean			starting = true;
	private boolean			running = false;
	private boolean			willPause = false;
	private boolean			paused = false;
	
	
	public Loop () {}
	
	public Loop ( String name ) {
		super( name );
	}
	
	public void start () {
		running			= true;
		super.start();
	}
	
	public boolean starting () {
		return starting;
	}
	
	/////////////// ABSTRACT METHOD ///////////////
	public abstract void init() throws Exception;

	public void initSuccess () {
		System.out.println( this.getClass().getName()+" '"+getName()+"' has started." );
	}

	public void initException ( Exception e ) {
		System.out.println( this.getClass().getName()+" '"+getName()+"': Exception during init():" );
		e.printStackTrace();
		end();
	}

	public boolean running () {
		return running;
	}
	
	/////////////// ABSTRACT METHOD ///////////////
	public abstract void loop () throws Exception;
	
	public void loopException ( Exception e ) {
		System.out.println( this.getClass().getName()+" '"+getName()+"': Exception during loop():" );
		e.printStackTrace();
		try {
			sleep(500);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}
	
	public void loopEnded () {
		System.out.println( this.getClass().getName()+" '"+getName()+"' has ended." );
	}

	public void pause () {
		willPause = true;
	}
	
	public void unpause () {
		paused = false;
	}
	
	public boolean paused () {
		return paused;
	}
	
	public void end () {
		running = false;
	}
	
	// annonymous wait; useful for child classes
	public void wait ( int ms ) {
		try {
			Thread.sleep ( ms );
		} catch (Exception e) {
			e.printStackTrace();
			end();
		}
	}
	
	public String toString () {
		return
			this.getClass().getName()+" '"+getName()+"':"+
			"Name:        "+getName()+"\n" +
			"Running:     "+running+"\n" +
			"Starting:    "+starting+"\n" +
			"Paused:      "+paused+"\n"
		;
	}
	
	public void run () {

		try {
			init();
			starting = false;
			initSuccess();
		} catch (Exception e) {
			initException( e );
		}

		while (running) {
			try {
				loop();
			}
			catch (Exception e) {
				loopException( e );
			}
		
			if (willPause) {
				paused = true;
				willPause = false;
			}
			while (paused) {
				try {
					sleep(1);
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (!running) {
					paused = false;
					break;
				}
			}

		}
		
		loopEnded();

	}
	
}
