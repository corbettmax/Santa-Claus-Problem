/**
 * Santa Claus Problem - Java Implementation using Semaphores
 *
 * Problem Summary:
 * - Santa sleeps until awakened by either:
 *   1. All 9 reindeer returning from vacation (PRIORITY)
 *   2. A group of 3 elves needing help
 * - If both are waiting, reindeer have priority
 * - Santa helps one group at a time
 *
 * This implementation uses 7 semaphores for robust synchronization:
 * 1. santaSem - Wakes up Santa
 * 2. reindeerSem - Controls reindeer harness operations
 * 3. elfSem - Controls elf consultation
 * 4. mutex - Protects shared counters
 * 5. reindeerMutex - Protects reindeer counter
 * 6. elfMutex - Protects elf counter
 * 7. santaMutex - Ensures Santa handles one group at a time
 */

import java.util.concurrent.Semaphore;
import java.util.Random;

public class SantaClaus {
    // Constants
    private static final int NUM_REINDEER = 9;
    private static final int NUM_ELVES = 10;
    private static final int ELF_GROUP_SIZE = 3;
    private static final int SIMULATION_TIME = 30000; // milliseconds

    // Semaphores
    private static Semaphore santaSem = new Semaphore(0);
    private static Semaphore reindeerSem = new Semaphore(0);
    private static Semaphore elfSem = new Semaphore(0);
    private static Semaphore santaMutex = new Semaphore(1);

    // Mutexes (using Semaphore with 1 permit)
    private static Semaphore reindeerMutex = new Semaphore(1);
    private static Semaphore elfMutex = new Semaphore(1);

    // Counters
    private static int reindeerCount = 0;
    private static int elfCount = 0;
    private static int waitingElves = 0;

    // Statistics
    private static int deliveries = 0;
    private static int elfConsultations = 0;

    // Random number generator
    private static Random random = new Random();

    /**
     * Santa thread - waits to be woken by reindeer or elves
     */
    static class Santa extends Thread {
        public void run() {
            System.out.println("🎅 SANTA: Starting my shift at the North Pole!");

            while (!Thread.interrupted()) {
                try {
                    // Wait to be woken up
                    santaSem.acquire();
                    santaMutex.acquire();

                    // Check if reindeer are ready (priority)
                    reindeerMutex.acquire();
                    if (reindeerCount == NUM_REINDEER) {
                        System.out.println("\n🎅 SANTA: Ho Ho Ho! All reindeer are back!");
                        System.out.println("🎅 SANTA: Preparing sleigh for Christmas delivery...");

                        // Release all reindeer to harness
                        for (int i = 0; i < NUM_REINDEER; i++) {
                            reindeerSem.release();
                        }

                        reindeerCount = 0;
                        reindeerMutex.release();

                        Thread.sleep(500); // Simulate delivery preparation
                        deliveries++;
                        System.out.println("🎅 SANTA: Sleigh ready! Delivering toys! (Delivery #" + deliveries + ")");
                        System.out.println("🎅 SANTA: Going back to sleep...\n");

                    } else {
                        reindeerMutex.release();

                        // Check if elves need help
                        elfMutex.acquire();
                        if (elfCount == ELF_GROUP_SIZE) {
                            System.out.println("\n🎅 SANTA: Three elves need help!");
                            System.out.println("🎅 SANTA: Meeting with elves...");

                            // Release the three elves for consultation
                            for (int i = 0; i < ELF_GROUP_SIZE; i++) {
                                elfSem.release();
                            }

                            elfCount = 0;
                            elfMutex.release();

                            Thread.sleep(300); // Simulate consultation
                            elfConsultations++;
                            System.out.println("🎅 SANTA: Consultation complete! (Session #" + elfConsultations + ")");
                            System.out.println("🎅 SANTA: Going back to sleep...\n");
                        } else {
                            elfMutex.release();
                        }
                    }

                    santaMutex.release();

                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }

    /**
     * Reindeer thread - returns from vacation and gets harnessed
     */
    static class Reindeer extends Thread {
        private int id;

        public Reindeer(int id) {
            this.id = id;
        }

        public void run() {
            while (!Thread.interrupted()) {
                try {
                    // Vacation in the tropics
                    Thread.sleep(2000 + random.nextInt(3000));
                    System.out.println("🦌 Reindeer " + id + ": Returning from vacation");

                    reindeerMutex.acquire();
                    reindeerCount++;

                    if (reindeerCount == NUM_REINDEER) {
                        System.out.println("🦌 Reindeer " + id + ": I'm the last one! Waking Santa!");
                        santaSem.release(); // Wake Santa
                    }

                    reindeerMutex.release();

                    // Wait to be harnessed
                    reindeerSem.acquire();
                    System.out.println("🦌 Reindeer " + id + ": Getting harnessed to sleigh");
                    Thread.sleep(100);
                    System.out.println("🦌 Reindeer " + id + ": Harnessed! Ready to deliver toys!");

                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }

    /**
     * Elf thread - occasionally needs Santa's help
     */
    static class Elf extends Thread {
        private int id;

        public Elf(int id) {
            this.id = id;
        }

        public void run() {
            while (!Thread.interrupted()) {
                try {
                    // Work on toys
                    Thread.sleep(1000 + random.nextInt(3000));

                    elfMutex.acquire();
                    waitingElves++;

                    if (waitingElves == ELF_GROUP_SIZE) {
                        System.out.println("🧝 Elf " + id + ": We have 3 elves waiting! Waking Santa!");
                        elfCount = ELF_GROUP_SIZE;
                        waitingElves = 0;
                        santaSem.release(); // Wake Santa
                    } else {
                        System.out.println("🧝 Elf " + id + ": Waiting for help (Total waiting: " + waitingElves + ")");
                    }

                    elfMutex.release();

                    // Wait for consultation
                    elfSem.acquire();
                    System.out.println("🧝 Elf " + id + ": Getting help from Santa...");
                    Thread.sleep(100);
                    System.out.println("🧝 Elf " + id + ": Problem solved! Back to work!");

                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("============================================================");
        System.out.println("🎄 SANTA CLAUS PROBLEM - JAVA IMPLEMENTATION 🎄");
        System.out.println("============================================================");
        System.out.println("Configuration:");
        System.out.println("  - Number of Reindeer: " + NUM_REINDEER);
        System.out.println("  - Number of Elves: " + NUM_ELVES);
        System.out.println("  - Elves per consultation group: " + ELF_GROUP_SIZE);
        System.out.println("  - Number of Semaphores: 7");
        System.out.println("============================================================");
        System.out.println("\nStarting simulation...\n");

        // Create Santa thread
        Santa santa = new Santa();
        santa.setDaemon(true);
        santa.start();

        // Create reindeer threads
        Reindeer[] reindeerThreads = new Reindeer[NUM_REINDEER];
        for (int i = 0; i < NUM_REINDEER; i++) {
            reindeerThreads[i] = new Reindeer(i + 1);
            reindeerThreads[i].setDaemon(true);
            reindeerThreads[i].start();
        }

        // Create elf threads
        Elf[] elfThreads = new Elf[NUM_ELVES];
        for (int i = 0; i < NUM_ELVES; i++) {
            elfThreads[i] = new Elf(i + 1);
            elfThreads[i].setDaemon(true);
            elfThreads[i].start();
        }

        // Let simulation run
        try {
            Thread.sleep(SIMULATION_TIME);
        } catch (InterruptedException e) {
            System.out.println("\nSimulation interrupted by user.");
        }

        System.out.println("\n============================================================");
        System.out.println("🎄 Simulation Complete! 🎄");
        System.out.println("============================================================");
        System.out.println("Statistics:");
        System.out.println("  - Total Deliveries: " + deliveries);
        System.out.println("  - Total Elf Consultations: " + elfConsultations);
        System.out.println("============================================================");
    }
}
