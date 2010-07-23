package org.drugis.addis.util.threading;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.LinkedList;

import org.junit.Test;

public class ThreadHandlerTest {

	class FakeModel extends AbstractSuspendableRunnable{
		
		private final int d_ms;
		boolean d_done;

		public FakeModel(int ms) {
			d_ms = ms;
		}
		
		public synchronized boolean getDone() {
			return d_done;
		}
		
		@Override
		public String toString() {
			return ""+d_ms;
		}
		
		public void run() {
			if (d_done)
				throw new IllegalStateException("Thread already done.");
			try {
				Thread.sleep(d_ms);
				waitIfSuspended();
			} catch (InterruptedException e) {
			}
			d_done = true;
		}
		
		// FIXME: Hacky.
		@Override
		public boolean equals(Object o) {
			if (o instanceof SuspendableThreadWrapper)
				return ((SuspendableThreadWrapper) o).getRunnable().equals(this);
			else if (o instanceof FakeModel)
				return super.equals(o);
			return false;
		}
	};
	
	@Test
	public void testQueueingOrder() {
		LinkedList<Runnable> ToDo1 = new LinkedList<Runnable>();
		LinkedList<Runnable> ToDo2 = new LinkedList<Runnable>();
		
		FakeModel tmp1 = new FakeModel(100);
		FakeModel tmp2 = new FakeModel(200);
		FakeModel tmp3 = new FakeModel(300);
		FakeModel tmp4 = new FakeModel(400);
		FakeModel tmp5 = new FakeModel(500);
		FakeModel tmp6 = new FakeModel(600);
		FakeModel tmp7 = new FakeModel(700);
		
		ToDo1.add(tmp1);
		ToDo1.add(tmp2);
		ToDo1.add(tmp3);
		ToDo1.add(tmp4);

		ToDo2.add(tmp5);
		ToDo2.add(tmp6);
		ToDo2.add(tmp7);

		ThreadHandler th = ThreadHandler.getInstance();
		
		th.scheduleTasks(ToDo1);
		th.scheduleTasks(ToDo2);
		assertTrue(th.d_scheduledTasks.contains(tmp1));
		assertTrue(th.d_runningTasks.contains(tmp6));
	}
	

	
	@Test
	public void testReprioritise() {
		LinkedList<Runnable> ToDo1 = new LinkedList<Runnable>();
						
		final int NUMMODELS = 4;
		
		for(int i=0; i < NUMMODELS; ++i) {
			ToDo1.add(new FakeModel((i+1) * 300));
		}
		
		ThreadHandler th = ThreadHandler.getInstance();
		th.scheduleTasks(ToDo1);
		assertTrue(th.d_runningTasks.containsAll(ToDo1.subList(0,2)));
		assertTrue(th.d_scheduledTasks.containsAll(ToDo1.subList(2,4)));
		
		th.scheduleTasks(ToDo1.subList(0, 2));
		assertTrue(th.d_runningTasks.containsAll(ToDo1.subList(0,2)));
		assertTrue(th.d_scheduledTasks.containsAll(ToDo1.subList(2,4)));
		
		th.scheduleTasks(ToDo1.subList(2, 4));
		assertTrue(th.d_runningTasks.containsAll(ToDo1.subList(2,4)));
		assertTrue(th.d_scheduledTasks.containsAll(ToDo1.subList(0,2)));
		
	}
	
	@Test
	public void testHighLoad() {
		final int NUMTHREADS = 100;
		LinkedList<Runnable> runnables = new LinkedList<Runnable>();
		ThreadHandler th = ThreadHandler.getInstance();
		ArrayList<FakeModel> threadList = new ArrayList<FakeModel>(NUMTHREADS);
		
		for (int i=0; i<NUMTHREADS; ++i) {
			FakeModel mod = new FakeModel((int) (Math.random() * 100));
			threadList.add(mod);
			runnables.add(mod);
			if ((Math.random() > 0.75) || (i == (NUMTHREADS-1))) {
				th.scheduleTasks(runnables);
				runnables.clear();
			}
			sleep((int) (Math.random() * 50));
			assertTrue(Runtime.getRuntime().availableProcessors() >= th.d_runningTasks.size());
//			System.out.println("running: " + th.d_runningTasks.size() + " scheduled: "+th.d_scheduledTasks.size());
		}
		
		assertTrue(Runtime.getRuntime().availableProcessors() >= th.d_runningTasks.size());
		waitTillDone();
		for (FakeModel mod : threadList) {
//			System.out.println(mod + " " + mod.getDone());
			assertTrue(mod.getDone());
		}
	}
	
	private void waitTillDone() {
		ThreadHandler th = ThreadHandler.getInstance();
		while ((th.d_runningTasks.size() > 0) || (th.d_scheduledTasks.size() > 0)) {
//			System.out.println("running: " + th.d_runningTasks.size() + " scheduled: "+th.d_scheduledTasks.size());
			sleep(100);
		}
	}

	private void sleep(int ms ) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
