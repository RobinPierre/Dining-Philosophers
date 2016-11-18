import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;


class RandomSleep {
	static public void SleepAWhile(int min) 	{
		try 		{
			Thread.sleep(min + (int)(Math.random() * 1000));
		}
		catch (InterruptedException e) 		{
			e.printStackTrace();
		}
	}
}

class SharedFork {
	private boolean wantedByNeighbor = false;
	private boolean requested = false;
	private boolean mine = false;

	public SharedFork(boolean isMine) {
		wantedByNeighbor = false;
		requested = false;
		mine = isMine;
	}
	public synchronized 
	boolean isWantedByNeighbor() {
		return (wantedByNeighbor);
	}
	public synchronized 
	boolean wasRequested() {
		return (requested);
	}
	public synchronized 
	boolean isMine() {
		return (mine);
	}
	public synchronized 
	boolean isReleasable() {
		return (mine && wantedByNeighbor);
	}
	public synchronized 
	boolean isRequestable() {
		return (!mine && !requested);
	}
	public synchronized 
	void setWantedByNeighbor() {
		wantedByNeighbor = true;
	}
	public synchronized 
	void setRequested() {
		requested = true;
	}
	public synchronized 
	void release() {
		wantedByNeighbor = false;
		mine = false;
	}
	public synchronized 
	void receive() {
		requested = false;
		mine = true;
	}
}


public class Diner implements IDinerNeighbor {

	enum DinerState {THINKING, HUNGRY, EATING};
	private DinerState myState;
	private IDinerNeighbor leftNeighbor, rightNeighbor;
	private SharedFork leftFork, rightFork;
	private String myName, leftNeighborName, rightNeighborName;
	private boolean initialized = false;

	protected Diner (String myN, boolean hasLeft, String leftNeighborN, 
			boolean hasRight, String rightNeighborN) {
		System.out.format("%s got args %b, %s, %b, %s\n", 
				myN, hasLeft, leftNeighborN, hasRight, rightNeighborN);
		initialized = false;
		myState = DinerState.THINKING;
		myName = myN;
		leftFork = new SharedFork(hasLeft);
		leftNeighborName = leftNeighborN;
		rightFork = new SharedFork(hasRight);
		rightNeighborName = rightNeighborN;
		System.out.print("Initial state: ");
		showState();
		initDinerAsServer("localhost");
		initDinerAsClient("localhost");
		if (leftNeighbor != null && rightNeighbor != null)
			initialized = true;
		System.out.print("Initial state: ");
		showState();
	}
	// methods from interface IDinerNeighbor
	public //synchronized 
	void requestFromRight()
	throws RemoteException {
		System.out.format("%s got request for right fork from %s.\n", myName, rightNeighborName);
        System.out.println ("requestFromRight(): " + Thread.currentThread().getName() + 
                " priority: " + Thread.currentThread().getPriority());
		rightFork.setWantedByNeighbor();
		showState();
		if (myState == DinerState.THINKING && rightFork.isMine())
			sendMyRightFork();
		System.out.format("Finished handling right fork request from %s.\n", rightNeighborName);
		showState();
	}
	public //synchronized 
	void requestFromLeft()
	throws RemoteException {
		System.out.format("%s got request for left fork from %s.\n", myName, leftNeighborName);
        System.out.println ("requestFromLeft(): " + Thread.currentThread().getName() + 
                " priority: " + Thread.currentThread().getPriority());

		leftFork.setWantedByNeighbor();
		showState();
		if (myState == DinerState.THINKING && leftFork.isMine()) 
			sendMyLeftFork();
		System.out.format("Finished handling left fork request from %s.\n", leftNeighborName);
		showState();
	}	
	public //synchronized 
	void forkFromRight() {
		System.out.format("%s received right fork from %s.\n", myName, rightNeighborName);
        System.out.println ("forkFromRight(): " + Thread.currentThread().getName() + 
                " priority: " + Thread.currentThread().getPriority());
		rightFork.receive();
		System.out.format("Finished receiving right fork from %s.\n", rightNeighborName);
		showState();
	}
	public //synchronized 
	void forkFromLeft() {
		System.out.format("%s received left fork from %s.\n", myName, leftNeighborName);
        System.out.println ("forkFromLeft(): " + Thread.currentThread().getName() + 
                " priority: " + Thread.currentThread().getPriority());
		leftFork.receive();
		System.out.format("Finished receiving left fork from %s.\n", leftNeighborName);
		showState();
	}
	// Diner state-handling methods
	protected //synchronized 
	void sendMyRightFork()
	throws RemoteException {
		showState();
		System.out.format("%s sending right fork to %s.\n", myName, rightNeighborName);
        System.out.println ("sendMyRightFork(): " + Thread.currentThread().getName() + 
                " priority: " + Thread.currentThread().getPriority());
        while (!initialized) {
        	System.out.println ("...wait until both rightNeighbor and leftNeighbor are set...");
        	RandomSleep.SleepAWhile(100);
        }
        synchronized(rightFork) {
		rightFork.release();
		rightNeighbor.forkFromLeft();
        }
		System.out.format("Finished sending right fork to %s.\n", rightNeighborName);
		showState();
	}
	protected //synchronized 
	void sendMyLeftFork()
	throws RemoteException {
		showState();
		System.out.format("%s sending left fork to %s.\n", myName, leftNeighborName);
        System.out.println ("sendMyLeftFork(): " + Thread.currentThread().getName() + 
                " priority: " + Thread.currentThread().getPriority());
        while (!initialized) {
        	System.out.println ("...wait until both rightNeighbor and leftNeighbor are set...");
        	RandomSleep.SleepAWhile(100);
        }
        synchronized(leftFork) {
		leftFork.release();
		leftNeighbor.forkFromRight();
        }
		System.out.format("Finished sending left fork to %s.\n", leftNeighborName);
		showState();
	}	
	protected //synchronized 
	void requestMyRightFork()
	throws RemoteException {
		showState();
		System.out.format("%s requesting right fork from %s.\n", myName, rightNeighborName);
        System.out.println ("requestMyRightFork(): " + Thread.currentThread().getName() + 
                " priority: " + Thread.currentThread().getPriority());
        while (!initialized) {
        	System.out.println ("...wait until both rightNeighbor and leftNeighbor are set...");
        	RandomSleep.SleepAWhile(100);
        }
		rightFork.setRequested();
		rightNeighbor.requestFromLeft(); // request right fork
		System.out.format("Finished requesting right fork from %s.\n", rightNeighborName);
		showState();
	}
	protected //synchronized 
	void requestMyLeftFork()
	throws RemoteException {
		showState();
		System.out.format("%s requesting left fork from %s.\n", myName, leftNeighborName);
        System.out.println ("requestMyLeftFork(): " + Thread.currentThread().getName() + 
                " priority: " + Thread.currentThread().getPriority());
        while (!initialized) {
        	System.out.println ("...wait until both rightNeighbor and leftNeighbor are set...");
        	RandomSleep.SleepAWhile(100);
        }
		leftFork.setRequested();
		leftNeighbor.requestFromRight();  // request left fork
		System.out.format("Finished requesting left fork from %s.\n", leftNeighborName);
		showState();
	}	
	protected void showState() {
		try {
			if (leftFork.isWantedByNeighbor()) {
				System.out.format("%s ", leftNeighborName);
				System.out.print("wants left fork; ");
			}
			System.out.format("%s ", myName);
			if (leftFork.isMine()) {
				System.out.print("has left fork, ");
			}
			if (leftFork.wasRequested())
				System.out.print("has requested left fork, ");
			System.out.print("is ");
			System.out.print(myState);
			if (rightFork.isMine()) {
				System.out.print(", has right fork");
			}
			if (rightFork.wasRequested())
				System.out.print(", has requested right fork");
			System.out.print("; ");
			if (rightFork.isWantedByNeighbor()) {
				System.out.format("%s ", rightNeighborName);
				System.out.print("wants right fork");
			}
		}
		catch (Exception e) {
			System.err.format("Exception from showState for %s",myName);
		}
		System.out.println();
	}
	protected void tryToSendForks()
	throws RemoteException {
		// pre: !EATING
		if (myState == DinerState.EATING)
			return;
		if (myState == DinerState.THINKING && rightFork.isReleasable())
			sendMyRightFork();
		if (myState == DinerState.THINKING && leftFork.isReleasable())
			sendMyLeftFork();
		if (myState == DinerState.HUNGRY && leftFork.isReleasable() && !rightFork.isMine())
			sendMyLeftFork();
	}
	protected void tryToRequestForks() 
	throws RemoteException {
		// pre: HUNGRY
		if (myState != DinerState.HUNGRY)
			return;
		if (rightFork.isRequestable()) 
			requestMyRightFork();
		if (rightFork.isMine() && leftFork.isRequestable()) 
			requestMyLeftFork();	
	}
	protected void tryToEat() 
	throws RemoteException {
		// pre: HUNGRY
		// post: HUNGRY || THINKING
		//System.out.println("Entering tryToEat().");
		//showState();
		if (myState != DinerState.HUNGRY)
			return;
		tryToSendForks();
		tryToRequestForks();
		RandomSleep.SleepAWhile(1);
		if (rightFork.isMine() && leftFork.isMine())
			eat(); // changes state to THINKING
		tryToSendForks();
		//showState();
		//System.out.println("Exiting tryToEat().");
	}
	protected void eat() {
		// pre: HUNGRY
		// post: THINKING
//		System.out.println("Entering eat().");
//        System.out.println ("eat(): " + Thread.currentThread().getName() + 
//                " priority: " + Thread.currentThread().getPriority());
		myState = DinerState.EATING;
//		showState();
		System.out.format("\t\t\t\t\t%s is EATING!\n", myName);
		RandomSleep.SleepAWhile(1000); // EATING
		// now done eating
		myState = DinerState.THINKING;
//		showState();
//		System.out.println("Exiting eat().");
	}
	protected void think() {
		// pre: THINKING
		// post: HUNGRY
//		System.out.println("Entering think().");
//        System.out.println ("think(): " + Thread.currentThread().getName() + 
//                " priority: " + Thread.currentThread().getPriority());
		System.out.format("\t%s is thinking...\n", myName);
		RandomSleep.SleepAWhile(1000); // THINKING
		myState = DinerState.HUNGRY;
//		showState();
//		System.out.format("\t\t\t%s is hungry...\n", myName);
//		System.out.println("Exiting think().");
	}
	protected void run()
	throws RemoteException {
		while (true) {
			if (myState == DinerState.THINKING)
				think(); // changes state to HUNGRY
			if (myState == DinerState.HUNGRY)
				tryToEat(); // returns with state HUNGRY || THINKING
		}
	}
	// Diner communication-setup methods
	protected void initDinerAsServer(String hostname) {
		Registry registry = null;
		try {
			registry = LocateRegistry.createRegistry(1099);
		}
		catch (Exception e) {
			System.err.format("In initDinerAsServer(), exception creating registry on host %s\n", hostname);
		}
		try {
			registry = LocateRegistry.getRegistry(hostname);
		}
		catch (Exception e) {
			System.err.format("In initDinerAsServer(), exception locating registry on host %s\n", hostname);
			System.exit(0);
		}
		if (registry==null) System.exit(0);
		try {
			IDinerNeighbor dinerStub =
				(IDinerNeighbor) UnicastRemoteObject.exportObject(this, 0);
			registry.rebind(myName, dinerStub);
			System.out.format("%s is ready.\n", myName);
		} 
		catch (Exception e) {
			System.err.format("%s exception trying to register.\n", myName);
			System.exit(0);
			//e.printStackTrace();
		}	
	}
	protected void initDinerAsClient(String hostname) {
		Registry registry = null;
		try {
			// By default the registry is at port 1099.
			registry = LocateRegistry.getRegistry(hostname);
		} 
		catch (Exception e) {
			System.err.format("In initDinerAsClient(), exception locating registry on host %s\n", hostname);
			System.exit(0);
		}
		leftNeighbor = null;
		while (leftNeighbor==null) {
			try {
				leftNeighbor = (IDinerNeighbor) registry.lookup(leftNeighborName); 
				System.out.format("Successful connection to left neighbor %s\n", 
						leftNeighborName);
				
			} 
			catch (Exception e) {
				System.err.format("%s exception trying to lookup %s.\n", myName, leftNeighborName);
				RandomSleep.SleepAWhile(1000);
				//e.printStackTrace();
			}		
		}
		rightNeighbor = null;
		while (rightNeighbor==null) {
			try {
				rightNeighbor = (IDinerNeighbor) registry.lookup(rightNeighborName); 
				System.out.format("Successful Connection to right neighbor %s\n", 
						rightNeighborName);
			} 
			catch (Exception e) {
				System.err.format("%s exception trying to lookup %s.\n", myName, rightNeighborName);
				RandomSleep.SleepAWhile(1000);
				//e.printStackTrace();
			}		
		}
	}

	public static void main(String[] args) {
    	String myName, leftNeighborName, rightNeighborName;
    	boolean hasLeft, hasRight;
    	if (args.length != 5) {
    		System.err.println("Sorry-- need 5 command line args");
    		// ToDo: print description of cmd line args
    		System.exit(0);
    	}
    	//System.out.format("cmd line args are: %s %s %s %s %s\n", 
    	//		args[0], args[1], args[2], args[3], args[4]);
    	myName = args[0];
    	hasLeft = args[1].equals("true");
    	leftNeighborName = args[2];
    	hasRight = args[3].equals("true");
    	rightNeighborName = args[4];
    	
    	Diner d = new Diner(myName, hasLeft, leftNeighborName, 
    			hasRight, rightNeighborName);
    	try {
    		d.run();
    	} 
    	catch (Exception e) {
    		System.err.format("%s exception while running: %s", 
    				myName, e.getMessage());
    		//System.exit(0);
    		e.printStackTrace();
    	}	
	}


}
