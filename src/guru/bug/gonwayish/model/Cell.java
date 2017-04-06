package guru.bug.gonwayish.model;

import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Dimitrijs Fedotovs <a href="http://www.bug.guru">www.bug.guru</a>
 * @version 1.0
 * @since 1.0
 */
public class Cell implements Runnable {
    private static final long LIFE_PERIOD = 5000; // milliseconds
    private final ReentrantLock lock = new ReentrantLock();
    private final Field field;
    private final Position position;
    private double size;
    private long birthtime;

    Cell(Field field, Position position, boolean initialAlive) {
        this.field = field;
        this.position = position;
        this.size = 0;

        if (initialAlive) {
            birthtime = System.currentTimeMillis();
//            birthtime=1;
            size=1;
        } else {
            birthtime = -1;
            size=0;
        }
    }

    public Position getPosition() {
        return position;
    }

    public Field getField() {
        return field;
    }

    @Override
    public void run() {
        waitUntilFieldReady();
        while (field.isRunning()) {
            pause();
            lock();


            try {
                long bt = getBirthtime();
                long cur = System.currentTimeMillis();

//                Set<Cell> around = field.findAround(position);
                List<Cell> around = field.findAroundAndTryLock(position);
                if (around == null) {
                    continue;
                }
                try {

//            long liveCount=0;
//            for (Cell c : around) {
//                CellInfo info = c.getCellInfo();
//                if (info.isAlive()){
//                    liveCount++;
//                }
//            }

//            liveCount= around.stream()
//                    .map(Cell::getCellInfo)
//                    .filter(CellInfo::isAlive)
//                    .count();

                    long liveCount = around.stream()
                            .map(Cell::getCellInfo)
                            .filter(CellInfo::isAlive)
                            .count();

//Any dead cell with exactly three live neighbours becomes a live cell, as if by reproduction.
                    if (bt == -1 && liveCount == 3) {
                        bt = System.currentTimeMillis();
                        updateCellInfo(bt, 1);
                    }
//Any live cell with two or three live neighbours lives on to the next generation.
                    if (bt != -1 && (liveCount == 3 || liveCount == 2)) {
                        bt = System.currentTimeMillis();
                        updateCellInfo(bt, 1);
//                  birthtime=System.currentTimeMillis();
//                    age=0;
//                    size=1;
                    }
//Any live cell with more than three live neighbours dies, as if by overpopulation.
                    if (bt != -1 && liveCount > 3) {
                        updateCellInfo(-1, 0);
//                birthtime=System.currentTimeMillis();
//                age=0;
//                size=1;
                    }
//            Any live cell with fewer than two live neighbours dies, as if caused by underpopulation.
                    if (bt != -1 && liveCount < 2) {
                        updateCellInfo(-1, 0);
                    }

//            long liveCount = around.stream()
//                    .map(Cell::getCellInfo)
//                    .filter(!CellInfo::isAlive)
//                    .count();


                    long age = cur - bt;


                    if (age > LIFE_PERIOD) {
                        System.out.println("Cell " + position + " is too old");
                        updateCellInfo(-1, 0);
//                break;
                    }

//            double p =(age - LIFE_PERIOD / 2.0) / LIFE_PERIOD * Math.PI;
////            double p = age * Math.PI;
//            double s = Math.cos(p);
//            setSize(1);
                } finally {
                    field.releaseAround(position);
                }
            } finally {
                unlock();
            }
        }
        System.out.println("Cell " + position + " finished");
    }

    private void waitUntilFieldReady() {
        synchronized (field) {
            while (!field.isRunning()){
                try {
                    field.wait();
                } catch (InterruptedException e) {

                }
            }
        }
    }

    private void pause() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    private synchronized void updateCellInfo(long birthtime, double size) {
        setBirthtime(birthtime);
        setSize(size);
    }

    private synchronized void setSize(double size) {
        this.size = size;
    }

    private synchronized void setBirthtime(long birthtime) {
        this.birthtime = birthtime;
    }

    public synchronized CellInfo getCellInfo() {
        return new CellInfo(position, birthtime > -1, size);
    }

    public  void lock() {
        lock.lock();
    }
    public  void unlock() {
        lock.unlock();
    }

    public long getBirthtime() {
        return birthtime;
    }

    public boolean tryLock() {
        return lock.tryLock();
    }
}
